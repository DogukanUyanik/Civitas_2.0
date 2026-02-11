package org.example.civitaswebapp.validator;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class MemberEmailValidator implements Validator {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return Member.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Member member = (Member) target;

        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        boolean exists = memberRepository.existsByEmailAndUnionAndIdNot(
                member.getEmail(),
                currentUser.getUnion(),
                member.getId() == null ? -1L : member.getId()
        );

        if (exists) {
            errors.rejectValue("email", "member.email.duplicate", "This email is already registered in your union.");
        }
    }
}