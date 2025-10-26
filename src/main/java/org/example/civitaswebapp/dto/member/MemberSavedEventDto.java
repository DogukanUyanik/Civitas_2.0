package org.example.civitaswebapp.dto.member;

public record MemberSavedEventDto(
        Long memberId,
        String firstName,
        String lastName,
        Long createdByUserId,
        boolean isNew
) {}

