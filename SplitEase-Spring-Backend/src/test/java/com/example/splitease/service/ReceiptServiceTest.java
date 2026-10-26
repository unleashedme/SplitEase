package com.example.splitease.service;

import com.example.splitease.models.ExpenseReceipts;
import com.example.splitease.models.Expenses;
import com.example.splitease.models.Groups;
import com.example.splitease.models.Users;
import com.example.splitease.repo.ExpenseReceiptRepo;
import com.example.splitease.repo.ExpenseRepo;
import com.example.splitease.repo.GroupMemberRepo;
import com.example.splitease.requestAndResponse.ReceiptImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.splitease.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock private ExpenseRepo expenseRepo;
    @Mock private ExpenseReceiptRepo expenseReceiptRepo;
    @Mock private GroupMemberRepo groupMemberRepo;

    @InjectMocks
    private ReceiptService receiptService;

    private Users alice;
    private Users bob;
    private Groups trip;
    private Expenses dinner;

    @BeforeEach
    void setUp() {
        alice = user("Alice", "alice@test.com");
        bob = user("Bob", "bob@test.com");
        trip = group("Trip", alice);
        dinner = expense(trip, alice, "900", "Dinner");
    }

    private static byte[] image(String format, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static MockMultipartFile upload(byte[] bytes, String contentType) {
        return new MockMultipartFile("file", "bill", contentType, bytes);
    }

    @Test
    void saveReceiptStoresPhotoAndJpegThumbnail() throws Exception {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));
        when(expenseReceiptRepo.findById(dinner.getId())).thenReturn(Optional.empty());
        byte[] png = image("png", 1200, 1600);

        // The declared type is wrong on purpose: the stored type comes from the file contents
        receiptService.saveReceipt(dinner.getId(), upload(png, "image/jpeg"), "alice@test.com");

        ArgumentCaptor<ExpenseReceipts> captor = ArgumentCaptor.forClass(ExpenseReceipts.class);
        verify(expenseReceiptRepo).save(captor.capture());
        ExpenseReceipts saved = captor.getValue();
        assertThat(saved.getExpense()).isSameAs(dinner);
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getData()).isEqualTo(png);
        assertThat(saved.getSizeBytes()).isEqualTo(png.length);
        assertThat(saved.getCreatedAt()).isNotNull();

        BufferedImage thumb = ImageIO.read(new ByteArrayInputStream(saved.getThumbnail()));
        assertThat(thumb.getHeight()).isEqualTo(320);
        assertThat(thumb.getWidth()).isEqualTo(240);
    }

    @Test
    void saveReceiptReplacesExistingPhoto() throws Exception {
        ExpenseReceipts existing = new ExpenseReceipts();
        existing.setExpense(dinner);
        existing.setCreatedAt(Instant.EPOCH);
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));
        when(expenseReceiptRepo.findById(dinner.getId())).thenReturn(Optional.of(existing));

        receiptService.saveReceipt(dinner.getId(), upload(image("jpg", 100, 50), "image/jpeg"), "alice@test.com");

        verify(expenseReceiptRepo).save(existing);
        assertThat(existing.getContentType()).isEqualTo("image/jpeg");
        assertThat(existing.getCreatedAt()).isAfter(Instant.EPOCH);
    }

    @Test
    void onlyPayerCanAttachBill() {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));

        assertThatThrownBy(() -> receiptService.saveReceipt(dinner.getId(), upload(image("png", 10, 10), "image/png"), "bob@test.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseReceiptRepo, never()).save(any());
    }

    @Test
    void rejectsFilesThatAreNotJpegOrPng() throws Exception {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));

        assertThatThrownBy(() -> receiptService.saveReceipt(dinner.getId(), upload("not an image".getBytes(), "image/jpeg"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        assertThatThrownBy(() -> receiptService.saveReceipt(dinner.getId(), upload(image("gif", 10, 10), "image/gif"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        verify(expenseReceiptRepo, never()).save(any());
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));

        assertThatThrownBy(() -> receiptService.saveReceipt(dinner.getId(), upload(new byte[0], "image/png"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        byte[] tooBig = new byte[(int) ReceiptService.MAX_UPLOAD_BYTES + 1];
        assertThatThrownBy(() -> receiptService.saveReceipt(dinner.getId(), upload(tooBig, "image/png"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void saveReceiptForMissingExpenseIsNotFound() {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.saveReceipt(dinner.getId(), upload(new byte[]{1}, "image/png"), "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void anyGroupMemberCanViewBill() throws Exception {
        ReceiptImage thumb = new ReceiptImage("image/jpeg", new byte[]{1}, Instant.now());
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));
        when(groupMemberRepo.findByGroupId(trip.getId())).thenReturn(List.of(member(trip, alice, "admin"), member(trip, bob, "member")));
        when(expenseReceiptRepo.findThumbnail(dinner.getId())).thenReturn(Optional.of(thumb));

        assertThat(receiptService.getReceipt(dinner.getId(), true, "bob@test.com")).isSameAs(thumb);
        verify(expenseReceiptRepo, never()).findFullImage(any());
    }

    @Test
    void outsidersCannotViewBill() {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));
        when(groupMemberRepo.findByGroupId(trip.getId())).thenReturn(List.of(member(trip, alice, "admin")));

        assertThatThrownBy(() -> receiptService.getReceipt(dinner.getId(), false, "bob@test.com"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(expenseReceiptRepo);
    }

    @Test
    void viewingExpenseWithoutBillIsNotFound() {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));
        when(groupMemberRepo.findByGroupId(trip.getId())).thenReturn(List.of(member(trip, alice, "admin")));
        when(expenseReceiptRepo.findFullImage(dinner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receiptService.getReceipt(dinner.getId(), false, "alice@test.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void payerCanRemoveBill() throws Exception {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));
        when(expenseReceiptRepo.existsById(dinner.getId())).thenReturn(true);

        receiptService.deleteReceipt(dinner.getId(), "alice@test.com");

        verify(expenseReceiptRepo).deleteById(dinner.getId());
    }

    @Test
    void othersCannotRemoveBill() {
        when(expenseRepo.findById(dinner.getId())).thenReturn(Optional.of(dinner));

        assertThatThrownBy(() -> receiptService.deleteReceipt(dinner.getId(), "bob@test.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(expenseReceiptRepo, never()).deleteById(any());
    }
}
