package org.example.civitaswebapp.service.accounting;

import org.example.civitaswebapp.dto.accounting.ScannedInvoiceDto;
import java.nio.file.Path;

public interface InvoiceScanner {
    ScannedInvoiceDto scan(Path filePath);
}
