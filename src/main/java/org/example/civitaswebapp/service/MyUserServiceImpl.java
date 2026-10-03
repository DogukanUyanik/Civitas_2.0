package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.exceptions.UserManagementException;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.security.UserSessionExpirer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class MyUserServiceImpl implements MyUserService {

    @Autowired
    private MyUserRepository myUserRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserSessionExpirer sessionExpirer;

    @Override
    public MyUser getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;

        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = principal.toString();
        }

        return myUserRepository.findByUsername(username);
    }

    @Override
    public MyUser findByUsername(String name) {
        return myUserRepository.findByUsername(name);
    }

    @Override
    public List<MyUser> listUsersInMyUnion() {
        return myUserRepository.findAllByUnionOrderByUsernameAsc(getLoggedInUser().getUnion());
    }

    @Override
    @Transactional
    public MyUser changeRole(Long targetUserId, MyUserRole newRole) {
        Union union = getLoggedInUser().getUnion();

        // Union scoping is manual (Spring doesn't know the concept). A user from another union is
        // reported exactly like a missing one so ids of other tenants' users are never confirmed.
        MyUser target = myUserRepository.findByIdAndUnion(targetUserId, union)
                .orElseThrow(() -> new UserManagementException("users.error.notFound"));

        if (target.getRole() == newRole) {
            return target;
        }

        boolean demotingAdmin = target.getRole() == MyUserRole.ADMIN;
        if (demotingAdmin && myUserRepository.countByUnionAndRole(union, MyUserRole.ADMIN) <= 1) {
            throw new UserManagementException("users.error.lastAdmin");
        }

        target.setRole(newRole);
        MyUser saved = myUserRepository.save(target);
        expireSessionsAfterCommit(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public MyUser createUser(String username, String rawPassword, MyUserRole role) {
        // Stamp the union from the caller, never from the request.
        Union union = getLoggedInUser().getUnion();

        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty() || rawPassword == null || rawPassword.isEmpty() || role == null) {
            throw new UserManagementException("users.error.invalid");
        }
        if (myUserRepository.existsByUsername(cleanUsername)) {
            throw new UserManagementException("users.error.usernameTaken");
        }

        MyUser user = MyUser.builder()
                .username(cleanUsername)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .union(union)
                .build();
        try {
            return myUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Lost a race against a concurrent create with the same username.
            throw new UserManagementException("users.error.usernameTaken");
        }
    }

    // Expire only once the new role is committed: expiring earlier would let the user log straight
    // back in and read the old role from the database.
    private void expireSessionsAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sessionExpirer.expireSessionsOf(userId);
                }
            });
        } else {
            sessionExpirer.expireSessionsOf(userId);
        }
    }
}
