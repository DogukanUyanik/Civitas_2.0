package org.example.civitaswebapp.service;

import lombok.NoArgsConstructor;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.example.civitaswebapp.security.MyUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private MyUserRepository myUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MyUser user = myUserRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        // Return a detached, collection-free principal. The union association is EAGER, so its id
        // is available within this loaded context. Anything needing the managed MyUser/Union graph
        // must reload it fresh per-request — never via this shared session principal.
        return new MyUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.getUnion().getId()
        );
    }
}
