package org.example.civitaswebapp.domain;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "accounting_invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceType type;

    private LocalDate invoiceDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmount;

    private String counterparty;
    private String invoiceNumber;
    private String category;

    private String fileUrl;
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

}
