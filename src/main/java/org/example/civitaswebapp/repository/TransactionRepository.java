package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.example.civitaswebapp.domain.TransactionStatus;
import org.example.civitaswebapp.domain.Union;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}