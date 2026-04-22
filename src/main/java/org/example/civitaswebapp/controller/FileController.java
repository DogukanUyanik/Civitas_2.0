package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final InvoiceRepository invoiceRepository;

    // We don't need UserService anymore, we get the user directly from the controller method
    @Autowired
    public FileController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/invoices/{filename}")
    public ResponseEntity<Resource> downloadInvoice(@PathVariable String filename,
                                                    @AuthenticationPrincipal MyUser currentUser) {

        Optional<Invoice> invoice = invoiceRepository.findByFileUrlAndUnion(filename, currentUser.getUnion());

        if (invoice.isEmpty() && invoiceRepository.existsByFileUrl(filename)) {
            // File is confirmed and saved, but belongs to a different union
            throw new AccessDeniedException("Access Denied: You do not have permission to view this file.");
        }
        // If invoice is empty but existsByFileUrl is false: file is a fresh upload not yet confirmed — allow preview

        try {
            Path filePath = Paths.get("./uploads/invoices").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}