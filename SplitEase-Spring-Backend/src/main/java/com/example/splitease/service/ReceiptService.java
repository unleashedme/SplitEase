package com.example.splitease.service;

import com.example.splitease.models.ExpenseReceipts;
import com.example.splitease.models.Expenses;
import com.example.splitease.repo.ExpenseReceiptRepo;
import com.example.splitease.repo.ExpenseRepo;
import com.example.splitease.repo.GroupMemberRepo;
import com.example.splitease.requestAndResponse.ReceiptImage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
public class ReceiptService {

    static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    // Guards against small files that decode into enormous images
    private static final long MAX_PIXELS = 40_000_000L;
    private static final int THUMBNAIL_SIZE = 320;

    // ImageIO format name -> content type we store and serve
    private static final Map<String, String> SUPPORTED_FORMATS = Map.of(
            "jpeg", "image/jpeg",
            "png", "image/png"
    );

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private ExpenseReceiptRepo expenseReceiptRepo;

    @Autowired
    private GroupMemberRepo groupMemberRepo;

    /** Attaches the bill photo to an expense, replacing any earlier one. Only the payer can do this. */
    @Transactional
    public void saveReceipt(UUID expenseId, MultipartFile file, String userEmail) throws AccessDeniedException {
        Expenses expense = findExpense(expenseId);
        requirePayer(expense, userEmail);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The bill photo is empty");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Bill photo must be 5 MB or smaller");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couldn't read the bill photo");
        }

        // The content type comes from the file itself, not from what the client claims
        DecodedImage decoded = decode(bytes);

        ExpenseReceipts receipt = expenseReceiptRepo.findById(expenseId).orElseGet(ExpenseReceipts::new);
        receipt.setExpense(expense);
        receipt.setContentType(decoded.contentType());
        receipt.setData(bytes);
        receipt.setThumbnail(thumbnail(decoded.image()));
        receipt.setSizeBytes(bytes.length);
        // Also identifies this version of the photo for caching, so a replaced bill isn't served stale
        receipt.setCreatedAt(Instant.now());
        expenseReceiptRepo.save(receipt);
    }

    /** Returns the bill photo to any member of the expense's group. */
    public ReceiptImage getReceipt(UUID expenseId, boolean thumbnail, String userEmail) throws AccessDeniedException {
        Expenses expense = findExpense(expenseId);

        boolean isMember = groupMemberRepo.findByGroupId(expense.getGroup().getId()).stream()
                .anyMatch(gm -> gm.getUser().getEmail().equals(userEmail));
        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this group");
        }

        return (thumbnail ? expenseReceiptRepo.findThumbnail(expenseId) : expenseReceiptRepo.findFullImage(expenseId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No bill attached to this expense"));
    }

    @Transactional
    public void deleteReceipt(UUID expenseId, String userEmail) throws AccessDeniedException {
        Expenses expense = findExpense(expenseId);
        requirePayer(expense, userEmail);

        if (!expenseReceiptRepo.existsById(expenseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No bill attached to this expense");
        }
        expenseReceiptRepo.deleteById(expenseId);
    }

    private Expenses findExpense(UUID expenseId) {
        return expenseRepo.findById(expenseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    // Same rule as deleting an expense: only the person who paid can change it
    private static void requirePayer(Expenses expense, String userEmail) throws AccessDeniedException {
        if (!expense.getPayer().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Only the person who paid can change the bill");
        }
    }

    private record DecodedImage(BufferedImage image, String contentType) {}

    private static DecodedImage decode(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw unsupportedImage();
            }
            ImageReader reader = readers.next();
            try {
                String contentType = SUPPORTED_FORMATS.get(reader.getFormatName().toLowerCase());
                if (contentType == null) {
                    throw unsupportedImage();
                }
                reader.setInput(input, true, true);
                // Check the size from the header before decoding any pixels
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill photo resolution is too large");
                }
                return new DecodedImage(reader.read(0), contentType);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw unsupportedImage();
        }
    }

    private static ResponseStatusException unsupportedImage() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Bill photo must be a JPEG or PNG image");
    }

    private static byte[] thumbnail(BufferedImage source) {
        double scale = Math.min(1.0, (double) THUMBNAIL_SIZE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        // JPEG has no transparency, so transparent PNG areas become white instead of black
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.8f);
            writer.write(null, new IIOImage(scaled, null, null), params);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't create bill thumbnail", e);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
