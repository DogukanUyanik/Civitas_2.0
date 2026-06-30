package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.dto.member.BulkImportResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberService {

    Page<Member> getMembers(Pageable pageable);

    Page<Member> getMembers(Pageable pageable, String search, String status);

    List<Member> getAllMembers();

    /**
     * Persists a member and initializes recurring-billing scheduling. When a subscription is being
     * enabled (frequency != NONE and no schedule yet), {@code nextBillingDate} is derived from
     * {@code subscriptionStartDate} (defaulting to today) and the frequency. Pass {@code null} for
     * {@code subscriptionStartDate} to default to today.
     */
    void saveMember(Member member, MyUser user, LocalDate subscriptionStartDate);

    void deleteMember(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByIdWithTransactions(Long id);

    Member getIdForPdf(Long id);

    BulkImportResultDto bulkImport(MultipartFile file, MyUser currentUser);
}