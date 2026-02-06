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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfInvoiceScanner implements InvoiceScanner {

    // 1. Smart Date Regex: Matches "29-12-2025", "29/12/2025" OR "29 december 2025"
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2}[\\s-./]+(?:januari|februari|maart|april|mei|juni|juli|augustus|september|oktober|november|december|\\d{1,2})[\\s-./]+\\d{4})", Pattern.CASE_INSENSITIVE);

    // 2. Strict Money Regex: Enforces 2 decimals (European format: 1.234,56)
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:[.]\\d{3})*(?:[,]\\d{2}))");

    // Mapping Dutch months to numbers
    private static final Map<String, String> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("januari", "01");
        MONTH_MAP.put("februari", "02");
        MONTH_MAP.put("maart", "03");
        MONTH_MAP.put("april", "04");
        MONTH_MAP.put("mei", "05");
        MONTH_MAP.put("juni", "06");
        MONTH_MAP.put("juli", "07");
        MONTH_MAP.put("augustus", "08");
        MONTH_MAP.put("september", "09");
        MONTH_MAP.put("oktober", "10");
        MONTH_MAP.put("november", "11");
        MONTH_MAP.put("december", "12");
    }

    @Override
    public ScannedInvoiceDto scan(Path filePath) {
        try (PDDocument document = PDDocument.load(filePath.toFile())) {

            PDFTextStripper stripper = new PDFTextStripper();
            // IMPORTANT: Sort by position helps read tables/columns accurately
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            LocalDate date = extractDate(text);
            BigDecimal amount = extractAmount(text);
            String counterparty = extractCounterparty(text);
            String type = extractType(text, counterparty);

            return new ScannedInvoiceDto(
                    filePath.getFileName().toString(),
                    date,
                    amount,
                    counterparty,
                    type
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to scan PDF", e);
        }
    }

    private LocalDate extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String dateStr = matcher.group(1).toLowerCase();

                // Convert Dutch month names to numbers (e.g. "december" -> "12")
                for (Map.Entry<String, String> entry : MONTH_MAP.entrySet()) {
                    if (dateStr.contains(entry.getKey())) {
                        dateStr = dateStr.replace(entry.getKey(), entry.getValue());
                    }
                }

                // Normalize all separators (spaces, dots, slashes) to dashes
                dateStr = dateStr.replaceAll("[\\s/.]", "-");

                // Now parsing: usually "29-12-2025" or "1-12-2025"
                // We use a flexible formatter that handles single digit days
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d-MM-yyyy"));
            } catch (Exception e) {
                System.err.println("Date parsing failed for string: " + matcher.group(1));
            }
        }
        return LocalDate.now(); // Fallback
    }

    private BigDecimal extractAmount(String text) {
        // Split text into lines so we can analyze context
        String[] lines = text.split("\n");
        BigDecimal maxAmount = BigDecimal.ZERO;

        for (String line : lines) {
            String cleanLine = line.trim().toUpperCase();

            // --- FILTERING RULES (The "Smart" Part) ---

            // 1. Ignore lines with "BE" followed by many digits (VAT or IBAN)
            // Example: "BTW BE 0403.170.701" -> matches the money regex but is NOT money
            if (cleanLine.contains("BE") && cleanLine.replaceAll("[^0-9]", "").length() > 9) {
                continue;
            }

            // 2. Ignore explicitly known non-amount numbers found in headers
            if (cleanLine.contains("TELEFOON") || cleanLine.contains("FAX")) {
                continue;
            }

            Matcher matcher = AMOUNT_PATTERN.matcher(line);
            while (matcher.find()) {
                try {
                    String amountStr = matcher.group(1)
                            .replace(".", "")   // 1.000 -> 1000
                            .replace(",", "."); // 12,50 -> 12.50

                    BigDecimal amount = new BigDecimal(amountStr);

                    // 3. Logic: Total is usually the biggest number, BUT...
                    // It must be less than 100,000 (avoids catching ID numbers, contract #s)
                    // It must be greater than current max
                    if (amount.compareTo(maxAmount) > 0 && amount.compareTo(new BigDecimal("100000")) < 0) {
                        maxAmount = amount;
                    }
                } catch (Exception e) {
                    // Skip invalid numbers
                }
            }
        }
        return maxAmount;
    }

    private String extractCounterparty(String text) {
        String lowerText = text.toLowerCase();

        // Simple keyword matching for common suppliers
        if (lowerText.contains("engie")) return "Engie";
        if (lowerText.contains("luminus")) return "Luminus";
        if (lowerText.contains("telenet")) return "Telenet";
        if (lowerText.contains("proximus")) return "Proximus";
        if (lowerText.contains("water-link")) return "Water-Link";
        if (lowerText.contains("coolblue")) return "Coolblue";
        if (lowerText.contains("bol.com")) return "Bol.com";
        if (lowerText.contains("albert heijn") || lowerText.contains("jumbo")) return "Supermarket";

        return "Unknown";
    }
    private String extractType(String text, String counterparty) {
        // 1. If we identified a supplier (Engie, Coolblue), it's definitely an Expense.
        if (!"Unknown".equals(counterparty)) {
            return "EXPENSE";
        }

        String lowerText = text.toLowerCase();

        // 2. Look for keywords implying we need to pay money
        if (lowerText.contains("te betalen") ||
                lowerText.contains("bedrag te betalen") ||
                lowerText.contains("overschrijving") ||
                lowerText.contains("factuur")) {
            return "EXPENSE";
        }

        // 3. Default to Expense (Safer assumption for uploaded PDFs)
        // Usually "Income" documents are ones YOU generate, not ones you receive.
        return "EXPENSE";
    }
}