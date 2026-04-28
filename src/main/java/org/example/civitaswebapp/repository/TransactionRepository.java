package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.Union;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllByMemberAndUnion(Member member, Union union);

    List<Transaction> findAllByStatusAndUnion(TransactionStatus status, Union union);

    List<Transaction> findAllByUnion(Union union);
    Page<Transaction> findAllByUnion(Union union, Pageable pageable);

    Optional<Transaction> findByIdAndUnion(Long id, Union union);

    long countByStatusAndUnion(TransactionStatus status, Union union);

    long countByStatusAndUnionAndCreatedAtBetween(
            TransactionStatus status, Union union, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0.0) FROM Transaction t " +
           "WHERE t.union = :union AND t.status = :status " +
           "AND t.createdAt >= :start AND t.createdAt < :end")
    Double sumAmountByUnionAndStatusAndPeriod(
            @Param("union") Union union,
            @Param("status") TransactionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DAY', t.createdAt), SUM(t.amount) " +
           "FROM Transaction t " +
           "WHERE t.union = :union AND t.status = :status " +
           "AND t.createdAt >= :start AND t.createdAt < :end " +
           "GROUP BY FUNCTION('DAY', t.createdAt) ORDER BY FUNCTION('DAY', t.createdAt)")
    List<Object[]> revenueByDayInPeriod(
            @Param("union") Union union,
            @Param("status") TransactionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
