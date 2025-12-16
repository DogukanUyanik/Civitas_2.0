package org.example.civitaswebapp.service;

import lombok.NoArgsConstructor;
import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.domain.MyUserRole;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

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

        return user;
    }

    private Collection<? extends GrantedAuthority> convertAuthorities(MyUserRole role) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toString()));
    }

}