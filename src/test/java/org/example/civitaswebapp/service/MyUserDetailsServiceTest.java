package org.example.civitaswebapp.service;

import jakarta.persistence.Entity;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.domain.Union;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.security.MyUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Guards the durable fix for the {@code ConcurrentModificationException} during Hibernate
 * collection loading: the security principal must be a detached, collection-free value object
 * ({@link MyUserPrincipal}), never the managed {@link MyUser} JPA entity. Because the principal
 * is shared across request threads and reachable from async notification threads, backing it
 * with a managed entity let two threads touch the same {@code PersistentCollection} at once.
 */
@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock
    private MyUserRepository myUserRepository;

    @InjectMocks
    private MyUserDetailsService myUserDetailsService;

    private MyUser buildManagedUser() {
        Union union = Union.builder()
                .id(UUID.randomUUID())
                .name("Civitas Demo Union")
                .address("Demo Street 1")
                .build();

        return MyUser.builder()
                .id(42L)
                .username("apo")
                .password("$2a$bcrypt-hash")
                .role(MyUserRole.ADMIN)
                .union(union)
                .build();
    }

    @Test
    void loadUserByUsername_returnsCollectionFreePrincipal_notTheJpaEntity() {
        MyUser entity = buildManagedUser();
        when(myUserRepository.findByUsername("apo")).thenReturn(entity);

        UserDetails details = myUserDetailsService.loadUserByUsername("apo");

        // Root-cause guard: principal must NOT be the managed entity graph.
        assertThat(details).isInstanceOf(MyUserPrincipal.class);
        assertThat(details).isNotInstanceOf(MyUser.class);
        assertThat((Object) details).isNotSameAs(entity);
    }

    @Test
    void loadUserByUsername_copiesScalarIdentityFromEntity() {
        MyUser entity = buildManagedUser();
        when(myUserRepository.findByUsername("apo")).thenReturn(entity);

        MyUserPrincipal principal = (MyUserPrincipal) myUserDetailsService.loadUserByUsername("apo");

        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getUsername()).isEqualTo("apo");
        assertThat(principal.getPassword()).isEqualTo("$2a$bcrypt-hash");
        assertThat(principal.getRole()).isEqualTo(MyUserRole.ADMIN);
        assertThat(principal.getUnionId()).isEqualTo(entity.getUnion().getId());
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_throwsWhenUserMissing() {
        when(myUserRepository.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> myUserDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    /**
     * Structural regression guard. If a future change makes the principal a JPA entity or gives it
     * a Collection/Map field, it can again share a Hibernate {@code PersistentCollection} across
     * threads. Keep {@link MyUserPrincipal} a plain, collection-free value object.
     */
    @Test
    void principalIsNotAnEntityAndHoldsNoCollections() {
        assertThat(MyUserPrincipal.class.isAnnotationPresent(Entity.class))
                .as("MyUserPrincipal must not be a JPA entity")
                .isFalse();

        List<String> collectionFields = java.util.Arrays.stream(MyUserPrincipal.class.getDeclaredFields())
                .filter(f -> Collection.class.isAssignableFrom(f.getType())
                        || Map.class.isAssignableFrom(f.getType()))
                .map(Field::getName)
                .toList();

        assertThat(collectionFields)
                .as("MyUserPrincipal must hold no Hibernate-managed collections")
                .isEmpty();
    }
}
