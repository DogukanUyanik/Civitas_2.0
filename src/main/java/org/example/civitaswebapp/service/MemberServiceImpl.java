package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.dto.member.MemberSavedEventDto;
import org.example.civitaswebapp.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;



    private Union getCurrentUserUnion() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof MyUser) {
            return ((MyUser) principal).getUnion();
        }
        throw new RuntimeException("No user logged in or user is not of type MyUser");
    }

    @Override
    public Page<Member> getMembers(Pageable pageable) {
        return memberRepository.findAllByUnion(getCurrentUserUnion(), pageable);
    }




    @Override
    public Page<Member> getMembers(Pageable pageable, String search, String status) {
        Union currentUnion = getCurrentUserUnion();
        boolean hasSearch = (search != null && !search.isBlank());
        boolean hasStatus = (status != null && !status.isBlank());

        if (!hasSearch && !hasStatus) {
            // Case 1: Show All (Scoped to Union)
            return memberRepository.findAllByUnion(currentUnion, pageable);

        } else if (hasStatus && !hasSearch) {
            // Case 2: Filter by Status Only (Scoped to Union)
            return memberRepository.findByMemberStatusAndUnion(
                    MemberStatus.valueOf(status), currentUnion, pageable);

        } else if (!hasStatus && hasSearch) {
            // Case 3: Search Text Only (Scoped to Union)
            return memberRepository.searchByUnion(currentUnion, search, pageable);

        } else {
            // Case 4: Search + Status (Scoped to Union)
            return memberRepository.searchByStatusAndUnion(
                    currentUnion, MemberStatus.valueOf(status), search, pageable);
        }
    }

    @Transactional
    @Override
    public void saveMember(Member member, MyUser createdByUser) {
        member.setUnion(createdByUser.getUnion());

        boolean exists = memberRepository.existsByEmailAndUnionAndIdNot(
                member.getEmail(),
                createdByUser.getUnion(),
                member.getId() == null ? -1L : member.getId()
        );

        if (exists) {
            throw new IllegalArgumentException("Email already exists in this Union.");
        }

        boolean isNew = member.getId() == null;
        memberRepository.save(member);

        var dto = new MemberSavedEventDto(
                member.getId(),
                member.getFirstName(),
                member.getLastName(),
                createdByUser.getId(),
                isNew
        );
        eventPublisher.publishEvent(dto);
    }

    @Override
    public void deleteMember(Member member) {
        // 🔒 SECURE: Check before delete
        verifyMemberBelongsToUnion(member);
        memberRepository.delete(member);
    }

    @Override
    public Optional<Member> findById(Long id) {
        Optional<Member> memberOpt = memberRepository.findById(id);

        // 🔒 SECURE: If found, check if it belongs to us. If not, return Empty.
        if (memberOpt.isPresent()) {
            try {
                verifyMemberBelongsToUnion(memberOpt.get());
            } catch (AccessDeniedException e) {
                return Optional.empty(); // Treat "Unauthorized" as "Not Found" to hide data
            }
        }
        return memberOpt;
    }

    @Override
    public Member getIdForPdf(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        verifyMemberBelongsToUnion(member);
        return member;
    }

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAllByUnion(getCurrentUserUnion());
    }

    private void verifyMemberBelongsToUnion(Member member) {
        Union currentUnion = getCurrentUserUnion();
        if (!member.getUnion().getId().equals(currentUnion.getId())) {
            throw new AccessDeniedException("ACCESS DENIED: You do not have permission to view/edit this member.");
        }
    }
}