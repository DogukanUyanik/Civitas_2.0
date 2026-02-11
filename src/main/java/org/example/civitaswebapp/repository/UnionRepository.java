package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.Union;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UnionRepository extends JpaRepository<Union, UUID> {
}
