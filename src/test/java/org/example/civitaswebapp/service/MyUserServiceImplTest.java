package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.exceptions.UserManagementException;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.security.MyUserPrincipal;
import org.example.civitaswebapp.security.UserSessionExpirer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the rules of the user/role management screen: union scoping is manual (Spring doesn't know
 * the concept), the last ADMIN of a union can never be demoted, new users are stamped with the
 * caller's union and get a BCrypt-encoded password, and a role change forces the target to log in
 * again so the new role isn't stuck behind a stale session principal.
 */
@ExtendWith(MockitoExtension.class)
class MyUserServiceImplTest {

    @Mock
    private MyUserRepository myUserRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private UserSessionExpirer sessionExpirer;

    @InjectMocks
    private MyUserServiceImpl service;

    private Union unionA;
    private MyUser caller;

    @BeforeEach
    void logInAdminOfUnionA() {
        unionA = Union.builder().id(UUID.randomUUID()).name("Union A").address("A street 1").build();
        caller = user(1L, "boss", MyUserRole.ADMIN, unionA);

        MyUserPrincipal principal = new MyUserPrincipal(1L, "boss", "hash", MyUserRole.ADMIN, unionA.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(myUserRepository.findByUsername("boss")).thenReturn(caller);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static MyUser user(Long id, String username, MyUserRole role, Union union) {
        return MyUser.builder().id(id).username(username).password("hash").role(role).union(union).build();
    }

    // ---- changeRole -------------------------------------------------------------------------

    @Test
    void changeRole_promotesViewerToAdmin_andExpiresTheirSessions() {
        MyUser target = user(2L, "viewer", MyUserRole.VIEWER, unionA);
        when(myUserRepository.findByIdAndUnion(2L, unionA)).thenReturn(Optional.of(target));
        when(myUserRepository.save(target)).thenReturn(target);

        MyUser result = service.changeRole(2L, MyUserRole.ADMIN);

        assertThat(result.getRole()).isEqualTo(MyUserRole.ADMIN);
        verify(myUserRepository).save(target);
        verify(sessionExpirer).expireSessionsOf(2L);
    }

    @Test
    void changeRole_lookupIsScopedToCallersUnion_soOtherTenantsUsersAreReportedAsNotFound() {
        // A user belonging to another union simply isn't found within union A.
        when(myUserRepository.findByIdAndUnion(99L, unionA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(99L, MyUserRole.ADMIN))
                .isInstanceOf(UserManagementException.class)
                .extracting(e -> ((UserManagementException) e).getMessageKey())
                .isEqualTo("users.error.notFound");

        // Must never fall back to an unscoped lookup, and must not mutate or log anyone out.
        verify(myUserRepository, never()).findById(any());
        verify(myUserRepository, never()).save(any());
        verify(sessionExpirer, never()).expireSessionsOf(any());
    }

    @Test
    void changeRole_refusesToDemoteTheLastAdmin() {
        MyUser onlyAdmin = user(3L, "solo", MyUserRole.ADMIN, unionA);
        when(myUserRepository.findByIdAndUnion(3L, unionA)).thenReturn(Optional.of(onlyAdmin));
        when(myUserRepository.countByUnionAndRole(unionA, MyUserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(3L, MyUserRole.VIEWER))
                .isInstanceOf(UserManagementException.class)
                .extracting(e -> ((UserManagementException) e).getMessageKey())
                .isEqualTo("users.error.lastAdmin");

        assertThat(onlyAdmin.getRole()).isEqualTo(MyUserRole.ADMIN);
        verify(myUserRepository, never()).save(any());
        verify(sessionExpirer, never()).expireSessionsOf(any());
    }

    @Test
    void changeRole_selfDemotionIsRefused_whenCallerIsTheOnlyAdmin() {
        when(myUserRepository.findByIdAndUnion(1L, unionA)).thenReturn(Optional.of(caller));
        when(myUserRepository.countByUnionAndRole(unionA, MyUserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(1L, MyUserRole.VIEWER))
                .isInstanceOf(UserManagementException.class)
                .extracting(e -> ((UserManagementException) e).getMessageKey())
                .isEqualTo("users.error.lastAdmin");
        verify(myUserRepository, never()).save(any());
    }

    @Test
    void changeRole_selfDemotionIsAllowed_whenAnotherAdminExists() {
        when(myUserRepository.findByIdAndUnion(1L, unionA)).thenReturn(Optional.of(caller));
        when(myUserRepository.countByUnionAndRole(unionA, MyUserRole.ADMIN)).thenReturn(2L);
        when(myUserRepository.save(caller)).thenReturn(caller);

        MyUser result = service.changeRole(1L, MyUserRole.VIEWER);

        assertThat(result.getRole()).isEqualTo(MyUserRole.VIEWER);
        verify(sessionExpirer).expireSessionsOf(1L);
    }

    @Test
    void changeRole_toTheSameRole_isANoOp() {
        MyUser target = user(2L, "viewer", MyUserRole.VIEWER, unionA);
        when(myUserRepository.findByIdAndUnion(2L, unionA)).thenReturn(Optional.of(target));

        service.changeRole(2L, MyUserRole.VIEWER);

        verify(myUserRepository, never()).save(any());
        verify(sessionExpirer, never()).expireSessionsOf(any());
    }

    // ---- createUser -------------------------------------------------------------------------

    @Test
    void createUser_stampsCallersUnion_trimsUsername_andStoresOnlyTheBcryptHash() {
        when(myUserRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode("temp-pass-1")).thenReturn("ENCODED");
        when(myUserRepository.saveAndFlush(any(MyUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createUser("  newbie  ", "temp-pass-1", MyUserRole.VIEWER);

        ArgumentCaptor<MyUser> saved = ArgumentCaptor.forClass(MyUser.class);
        verify(myUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("newbie");
        assertThat(saved.getValue().getPassword()).isEqualTo("ENCODED").isNotEqualTo("temp-pass-1");
        assertThat(saved.getValue().getRole()).isEqualTo(MyUserRole.VIEWER);
        assertThat(saved.getValue().getUnion()).isSameAs(unionA);
    }

    @Test
    void createUser_rejectsAnAlreadyTakenUsername() {
        when(myUserRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("taken", "temp-pass-1", MyUserRole.VIEWER))
                .isInstanceOf(UserManagementException.class)
                .extracting(e -> ((UserManagementException) e).getMessageKey())
                .isEqualTo("users.error.usernameTaken");
        verify(myUserRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUser_mapsALostUniqueConstraintRaceToUsernameTaken() {
        when(myUserRepository.existsByUsername("racer")).thenReturn(false);
        when(passwordEncoder.encode("temp-pass-1")).thenReturn("ENCODED");
        when(myUserRepository.saveAndFlush(any(MyUser.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.createUser("racer", "temp-pass-1", MyUserRole.VIEWER))
                .isInstanceOf(UserManagementException.class)
                .extracting(e -> ((UserManagementException) e).getMessageKey())
                .isEqualTo("users.error.usernameTaken");
    }

    @Test
    void createUser_rejectsBlankUsername() {
        assertThatThrownBy(() -> service.createUser("   ", "temp-pass-1", MyUserRole.VIEWER))
                .isInstanceOf(UserManagementException.class)
                .extracting(e -> ((UserManagementException) e).getMessageKey())
                .isEqualTo("users.error.invalid");
        verify(myUserRepository, never()).saveAndFlush(any());
    }

    // ---- listUsersInMyUnion -----------------------------------------------------------------

    @Test
    void listUsersInMyUnion_isScopedToCallersUnion() {
        List<MyUser> users = List.of(caller);
        when(myUserRepository.findAllByUnionOrderByUsernameAsc(unionA)).thenReturn(users);

        assertThat(service.listUsersInMyUnion()).isSameAs(users);
    }
}
