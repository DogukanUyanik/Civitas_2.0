package org.example.civitaswebapp.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
// id-only identity, consistent with the other entities — see [[lombok-data-jpa-entities]].
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "unions")
public class Union {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;


    @NotBlank(message = "Union name is required")
    @ToString.Include
    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String vatCode;

    @NotBlank(message = "Address is required")
    private String address;

    private String stripeCustomerId;
    private SubscriptionStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
