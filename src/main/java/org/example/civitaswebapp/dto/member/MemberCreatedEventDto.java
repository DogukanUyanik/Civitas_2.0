package org.example.civitaswebapp.dto.member;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.civitaswebapp.domain.MyUser;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberCreatedEventDto {
    private Long id;
    private String firstName;
    private String lastName;
    private Long createdByUserId;
}
