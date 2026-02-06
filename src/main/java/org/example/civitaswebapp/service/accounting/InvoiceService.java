package org.example.civitaswebapp.service.accounting;

import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.domain.InvoiceStatus;
import org.example.civitaswebapp.domain.InvoiceType;
import org.example.civitaswebapp.dto.accounting.ScannedInvoiceDto;
import org.example.civitaswebapp.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StorageService storageService;
    private final InvoiceScanner invoiceScanner;

    @Autowired
    public InvoiceService(InvoiceRepository invoiceRepository,
                          StorageService storageService,
                          InvoiceScanner invoiceScanner) {
        this.invoiceRepository = invoiceRepository;
        this.storageService = storageService;
        this.invoiceScanner = invoiceScanner;
    }

    /**
     * STEP 1: Upload & Scan
     * This saves the file and tries to guess the contents.
     * It does NOT save to the database yet.
     */
    public ScannedInvoiceDto uploadAndScan(MultipartFile file) {
        // 1. Save file to disk
        String filename = storageService.store(file);

        // 2. Get the full path
        Path filePath = storageService.getRootLocation().resolve(filename);

        // 3. Scan the PDF
        ScannedInvoiceDto dto = invoiceScanner.scan(filePath);

        // 4. Ensure the DTO has the filename so we can link it later
        // (We return a new record because Java Records are immutable)
        return new ScannedInvoiceDto(
                filename,
                dto.guessedDate(),
                dto.guessedAmount(),
                dto.guessedCounterparty()
        );
    }

    /**
     * STEP 2: Confirm & Save
     * The user checked the data, corrected it, and clicked "Save".
     */
    public Invoice saveConfirmedInvoice(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.APPROVED);
        return invoiceRepository.save(invoice);
    }

    // Fetch lists for the UI tabs
    public List<Invoice> getIncomes() {
        return invoiceRepository.findByTypeAndStatusOrderByInvoiceDateDesc(
                InvoiceType.INCOME, InvoiceStatus.APPROVED);
    }

    public List<Invoice> getExpenses() {
        return invoiceRepository.findByTypeAndStatusOrderByInvoiceDateDesc(
                InvoiceType.EXPENSE, InvoiceStatus.APPROVED);
    }
}