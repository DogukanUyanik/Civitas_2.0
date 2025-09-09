package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MemberService {

    Page<Member> getMembers(Pageable pageable);

    Page<Member> getMembers(Pageable pageable, String search, String status);

    //needed for create event modal
    List<Member> getAllMembers();


    @Transactional
    public void saveMember(Member member);

    @Transactional
    public void deleteMember(Member member);

    public Optional<Member> findById(Long id);

}
