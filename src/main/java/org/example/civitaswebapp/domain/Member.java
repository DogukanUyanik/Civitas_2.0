package org.example.civitaswebapp.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.validation.constraints.*;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateOfBirth;

    private LocalDate dateOfLastPayment;

    @NotNull(message = "Member status is required")
    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();

    @ManyToMany(mappedBy = "attendees")
    @JsonBackReference
    private Set<Event> events = new HashSet<>();

    // new field to track creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public String getName(){
        return this.firstName + " " + this.lastName;
    }
}
