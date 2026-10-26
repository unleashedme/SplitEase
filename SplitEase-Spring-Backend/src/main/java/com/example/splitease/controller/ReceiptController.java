package com.example.splitease.controller;

import com.example.splitease.requestAndResponse.ReceiptImage;
import com.example.splitease.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.UUID;

@RestController
public class ReceiptController {

    @Autowired
    private ReceiptService receiptService;

    @PostMapping(value = "/expense/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReceipt(
            @PathVariable("id") UUID id,
            @RequestPart("file") MultipartFile file,
            Principal principal
    ) throws AccessDeniedException {
        receiptService.saveReceipt(id, file, principal.getName());
        return ResponseEntity.ok().build();
    }

    /** size=thumb returns a small JPEG preview, size=full the photo as uploaded. */
    @GetMapping("/expense/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable("id") UUID id,
            @RequestParam(value = "size", defaultValue = "full") String size,
            Principal principal,
            WebRequest webRequest
    ) throws AccessDeniedException {
        if (!size.equals("full") && !size.equals("thumb")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be full or thumb");
        }

        ReceiptImage image = receiptService.getReceipt(id, size.equals("thumb"), principal.getName());

        // The app revalidates on every view; an unchanged bill costs a 304 instead of the whole image
        String eTag = "\"" + id + "-" + size + "-" + image.createdAt().toEpochMilli() + "\"";
        if (webRequest.checkNotModified(eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .eTag(eTag)
                .body(image.bytes());
    }

    @DeleteMapping("/expense/{id}/receipt")
    public ResponseEntity<?> deleteReceipt(@PathVariable("id") UUID id, Principal principal) throws AccessDeniedException {
        receiptService.deleteReceipt(id, principal.getName());
        return ResponseEntity.ok().build();
    }
}
