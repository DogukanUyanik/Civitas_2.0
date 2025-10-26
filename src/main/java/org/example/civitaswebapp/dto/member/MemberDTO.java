package org.example.civitaswebapp.dto.member;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public record MemberDTO(Long id, String firstName, String lastName) {}

