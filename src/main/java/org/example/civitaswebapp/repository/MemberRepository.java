package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.dto.MemberDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailAndIdNot(String email, Long id);

    // Combined search by firstName, lastName, or email AND memberStatus
    Page<Member> findByMemberStatusAndFirstNameContainingIgnoreCaseOrMemberStatusAndLastNameContainingIgnoreCaseOrMemberStatusAndEmailContainingIgnoreCase(
            MemberStatus status1, String firstName,
            MemberStatus status2, String lastName,
            MemberStatus status3, String email,
            Pageable pageable);

    // Fallback: search without status filter
    Page<Member> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email,
            Pageable pageable);

    // Optionally, just filter by status
    Page<Member> findByMemberStatus(MemberStatus status, Pageable pageable);


    long countByMemberStatus(MemberStatus status);

    List<Member> findByDateOfLastPaymentBefore(LocalDate date);

}
