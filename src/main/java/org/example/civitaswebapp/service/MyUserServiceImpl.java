package org.example.civitaswebapp.service;

import org.example.civitaswebapp.domain.MyUser;
import org.example.civitaswebapp.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class MyUserServiceImpl implements MyUserService {

    @Autowired
    private MyUserRepository myUserRepository;

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
}
