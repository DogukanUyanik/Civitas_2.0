package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    @NotNull(message = "{transaction.member.required}")
    private Member member;

    @NotNull(message = "{transaction.amount.required}")
    @DecimalMin(value = "0.49", message = "{transaction.amount.min}")
    private Double amount;

    @NotBlank(message = "{transaction.currency.required}")
    private String currency;

    private String paymentId;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{transaction.type.required}")
    private TransactionType type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String note;
}
