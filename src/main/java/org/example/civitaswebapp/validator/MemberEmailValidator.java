package org.example.civitaswebapp.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.repository.MemberRepository;

@Component
public class MemberEmailValidator implements Validator {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return Member.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Member member = (Member) target;

        if (member.getEmail() == null || member.getEmail().isBlank()) {
            errors.rejectValue("email", "member.email.empty");
            return;
        }

        // Email format check
        if (!member.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.rejectValue("email", "member.email.invalid");
            return;
        }

        // Check uniqueness
        Long id = member.getId() != null ? member.getId() : -1L;
        boolean emailExists = memberRepository.existsByEmailAndIdNot(member.getEmail(), id);
        if (emailExists) {
            errors.rejectValue("email", "member.email.exists");
        }
    }
}
