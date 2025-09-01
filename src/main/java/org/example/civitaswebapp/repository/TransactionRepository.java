package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Member;
import org.example.civitaswebapp.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByMember(Member member);

}
