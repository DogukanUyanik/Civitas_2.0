package org.example.civitaswebapp.service.accounting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.civitaswebapp.dto.accounting.ScannedInvoiceDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class PdfInvoiceScanner implements InvoiceScanner {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{2}[-./]\\d{2}[-./]\\d{4})\\b");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{2}))");

    @Override
    public ScannedInvoiceDto scan(Path filePath) {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            LocalDate date = extractDate(text);
            BigDecimal amount = extractAmount(text);
            String counterparty = extractCounterparty(text);

            return new ScannedInvoiceDto(
                    filePath.getFileName().toString(),
                    date,
                    amount,
                    counterparty
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan PDF", e);
        }
    }

    private LocalDate extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String dateStr = matcher.group(1).replace("/", "-").replace(".", "-");
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception e) {
            }
        }
        return LocalDate.now();
    }

    private BigDecimal extractAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        BigDecimal maxAmount = BigDecimal.ZERO;

        // Strategy: Find ALL numbers and take the biggest one.
        // Invoices usually list the Total at the bottom, which is the biggest number.
        while (matcher.find()) {
            try {
                String amountStr = matcher.group(1)
                        .replace(".", "")  // Remove thousands separator (European style: 1.000,00)
                        .replace(",", "."); // Change decimal comma to dot for Java

                BigDecimal amount = new BigDecimal(amountStr);
                if (amount.compareTo(maxAmount) > 0) {
                    maxAmount = amount;
                }
            } catch (Exception e) {
                // Not a number, skip
            }
        }
        return maxAmount;
    }

    private String extractCounterparty(String text) {
        // This is hard! For now, we return "Unknown".
        // Later we can add a list of known companies to check against.
        // e.g. if (text.contains("Coolblue")) return "Coolblue";
        return "Unknown";
    }

}
