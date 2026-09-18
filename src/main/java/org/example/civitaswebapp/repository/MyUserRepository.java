package org.example.civitaswebapp.repository;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.domain.Union;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {
    MyUser findByUsername(String username);

    boolean existsByUsername(String username);

    // Union-scoped lookups for the user-management screen (see CLAUDE.md multi-tenancy rules).
    List<MyUser> findAllByUnionOrderByUsernameAsc(Union union);

    Optional<MyUser> findByIdAndUnion(Long id, Union union);

    long countByUnionAndRole(Union union, MyUserRole role);
}
