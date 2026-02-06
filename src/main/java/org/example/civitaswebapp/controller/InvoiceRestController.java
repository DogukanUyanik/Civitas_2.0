package org.example.civitaswebapp.controller;

import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.dto.accounting.ScannedInvoiceDto;
import org.example.civitaswebapp.service.accounting.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceRestController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/scan")
    public ResponseEntity<ScannedInvoiceDto> scanInvoice(@RequestParam("file") MultipartFile file) {
        try {
            ScannedInvoiceDto result = invoiceService.uploadAndScan(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<Invoice> confirmInvoice(@RequestBody Invoice invoice) {
        Invoice saved = invoiceService.saveConfirmedInvoice(invoice);
        return ResponseEntity.ok(saved);
    }
}