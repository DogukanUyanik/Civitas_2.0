package org.example.civitaswebapp.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "union_id"}) // <--- Unique combination
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{member.firstName.required}")
    private String firstName;

    @NotBlank(message = "{member.lastName.required}")
    private String lastName;

    @Email(message = "{member.email.valid}")
    @NotBlank(message = "{member.email.required}")
    private String email;

    @NotBlank(message = "{member.phone.required}")
    private String phoneNumber;

    @NotBlank(message = "{member.address.required}")
    private String address;

    @Past(message = "{member.dob.past}")
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateOfBirth;

    private LocalDate dateOfLastPayment;

    @NotNull(message = "{member.status.required}")
    @Enumerated(EnumType.STRING)
    private MemberStatus memberStatus;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Transaction> transactions = new ArrayList<>();

    @ManyToMany(mappedBy = "attendees")
    @JsonBackReference
    private Set<Event> events = new HashSet<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "union_id", nullable = false)
    private Union union;

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
