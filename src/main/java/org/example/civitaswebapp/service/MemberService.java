package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MemberService {

    Page<Member> getMembers(Pageable pageable);

    Page<Member> getMembers(Pageable pageable, String search, String status);

    List<Member> getAllMembers();

    void saveMember(Member member, MyUser user);

    void deleteMember(Member member);

    Optional<Member> findById(Long id);

    Member getIdForPdf(Long id);
}