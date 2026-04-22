package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Invoice;
import org.example.civitaswebapp.domain.InvoiceStatus;
import org.example.civitaswebapp.domain.InvoiceType;
import org.example.civitaswebapp.domain.Union;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository  extends JpaRepository<Invoice, Long> {

    List<Invoice> findByTypeAndStatusOrderByInvoiceDateDesc(InvoiceType type, InvoiceStatus status);

    Optional<Invoice> findByFileUrlAndUnion(String fileUrl, Union union);

    boolean existsByFileUrl(String fileUrl);

    Optional<Invoice> findByIdAndUnion(Long id, Union union);
}
