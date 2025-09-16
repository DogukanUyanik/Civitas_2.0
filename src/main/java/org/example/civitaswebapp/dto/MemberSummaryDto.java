package org.example.civitaswebapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberSummaryDto {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String dateOfLastPayment; // formatted as string
}
