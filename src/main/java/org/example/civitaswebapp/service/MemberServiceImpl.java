package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public Page<Member> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Override
    public void saveMember(Member member) {
        memberRepository.save(member);

    }

    @Override
    public void deleteMember(Member member) {
        memberRepository.delete(member);

    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    @Override
    public Page<Member> getMembers(Pageable pageable, String search, String status) {
        if ((search == null || search.isBlank()) && (status == null || status.isBlank())) {
            return memberRepository.findAll(pageable);
        } else if (status == null || status.isBlank()) {
            // filter only by search
            return memberRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, search, pageable);
        } else if (search == null || search.isBlank()) {
            // filter only by status
            return memberRepository.findByMemberStatus(org.example.civitaswebapp.domain.MemberStatus.valueOf(status), pageable);
        } else {
            // filter by search AND status
            return memberRepository.findByMemberStatusAndFirstNameContainingIgnoreCaseOrMemberStatusAndLastNameContainingIgnoreCaseOrMemberStatusAndEmailContainingIgnoreCase(
                    org.example.civitaswebapp.domain.MemberStatus.valueOf(status), search,
                    org.example.civitaswebapp.domain.MemberStatus.valueOf(status), search,
                    org.example.civitaswebapp.domain.MemberStatus.valueOf(status), search,
                    pageable
            );
        }
    }



    /*
    @Override
    public Page<Member> getMembers(Pageable pageable, String search, String status) {

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null && !status.isBlank();

        if (hasSearch && hasStatus) {
            MemberStatus memberStatus = MemberStatus.valueOf(status.toUpperCase());
            return memberRepository
                    .findByMemberStatusAndFirstNameContainingIgnoreCaseOrMemberStatusAndLastNameContainingIgnoreCaseOrMemberStatusAndEmailContainingIgnoreCase(
                            memberStatus, search,
                            memberStatus, search,
                            memberStatus, search,
                            pageable
                    );
        } else if (hasSearch) {
            return memberRepository
                    .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            search, search, search, pageable
                    );
        } else if (hasStatus) {
            MemberStatus memberStatus = MemberStatus.valueOf(status.toUpperCase());
            return memberRepository.findByMemberStatus(memberStatus, pageable);
        } else {
            return memberRepository.findAll(pageable);
        }
    }

    */


}
