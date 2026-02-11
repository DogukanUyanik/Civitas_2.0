package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.MemberStatus;
import org.example.civitaswebapp.domain.Union;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {


    Page<Member> findAllByUnion(Union union, Pageable pageable);
    List<Member> findAllByUnion(Union union);
    boolean existsByEmailAndUnionAndIdNot(String email, Union union, Long id);

    @Query("SELECT m FROM Member m WHERE " +
            "m.union = :union AND " +
            "m.memberStatus = :status AND " +
            "(LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')))")

    Page<Member> searchByStatusAndUnion(@Param("union") Union union,
                                        @Param("status") MemberStatus status,
                                        @Param("search") String search,
                                        Pageable pageable);


    @Query("SELECT m FROM Member m WHERE " +
            "m.union = :union AND " +
            "(LOWER(m.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(m.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')))")

    Page<Member> searchByUnion(@Param("union") Union union, @Param("search") String search, Pageable pageable);

    Page<Member> findByMemberStatusAndUnion(MemberStatus status, Union union, Pageable pageable);

    long countByMemberStatusAndUnion(MemberStatus status, Union union);

    long countByCreatedAtAfterAndUnion(LocalDateTime date, Union union);

    List<Member> findByDateOfLastPaymentBeforeAndUnion(LocalDate date, Union union);
}