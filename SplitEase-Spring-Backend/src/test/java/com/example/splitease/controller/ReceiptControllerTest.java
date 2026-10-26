package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.ReceiptImage;
import com.example.splitease.service.ReceiptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

import static com.example.splitease.controller.ControllerTestSupport.ALICE;
import static com.example.splitease.controller.ControllerTestSupport.mvc;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReceiptControllerTest {

    @Mock private ReceiptService receiptService;

    @InjectMocks private ReceiptController receiptController;

    @Test
    void uploadPassesFileToService() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "bill.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mvc(receiptController).perform(multipart("/expense/{id}/receipt", id).file(file).principal(ALICE))
                .andExpect(status().isOk());

        verify(receiptService).saveReceipt(eq(id), argThat((MultipartFile f) -> f.getSize() == 3), eq("alice@test.com"));
    }

    @Test
    void getReturnsImageWithCachingHeaders() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.ofEpochMilli(1234);
        when(receiptService.getReceipt(id, true, "alice@test.com"))
                .thenReturn(new ReceiptImage("image/jpeg", new byte[]{7, 8}, createdAt));

        mvc(receiptController).perform(get("/expense/{id}/receipt", id).param("size", "thumb").principal(ALICE))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{7, 8}))
                .andExpect(header().string("ETag", "\"" + id + "-thumb-1234\""))
                .andExpect(header().string("Cache-Control", "no-cache, private"));
    }

    @Test
    void getReturnsNotModifiedWhenAppHasCurrentVersion() throws Exception {
        UUID id = UUID.randomUUID();
        when(receiptService.getReceipt(id, false, "alice@test.com"))
                .thenReturn(new ReceiptImage("image/png", new byte[]{7, 8}, Instant.ofEpochMilli(1234)));

        mvc(receiptController).perform(get("/expense/{id}/receipt", id).principal(ALICE)
                        .header("If-None-Match", "\"" + id + "-full-1234\""))
                .andExpect(status().isNotModified())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void getRejectsUnknownSize() throws Exception {
        mvc(receiptController).perform(get("/expense/{id}/receipt", UUID.randomUUID()).param("size", "huge").principal(ALICE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(receiptService);
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();

        mvc(receiptController).perform(delete("/expense/{id}/receipt", id).principal(ALICE))
                .andExpect(status().isOk());

        verify(receiptService).deleteReceipt(id, "alice@test.com");
    }
}
