package org.example.civitaswebapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSummaryDto {
    private Long id;
    private String memberName;
    private Double amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}


