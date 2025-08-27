package org.example.civitaswebapp.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;
    private String address;

    private LocalDate dateOfBirth;
    private LocalDate dateOfLastPayment;

    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus;
}
