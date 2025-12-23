package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.domain.InvoiceStatus;
import org.example.civitaswebapp.domain.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository  extends JpaRepository<Invoice, Long> {

    List<Invoice> findByTypeAndStatusOrderByInvoiceDateDesc(InvoiceType type, InvoiceStatus status);

    List<Invoice> findTop5ByStatusOrderByInvoiceDateDesc(InvoiceStatus status);
}
