package com.csdg1t3.ryverbankapi.security;

import com.csdg1t3.ryverbankapi.user.*;

import org.springframework.stereotype.Component;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * Convenience class that performs user role authentication. It is able to retrieve the UserDetails
 * given the authorization credentials provided in HTTP requests
 */

@Component
public class UserAuthenticator {
    private UserRepository userRepo;

    public static final SimpleGrantedAuthority MANAGER = new SimpleGrantedAuthority("ROLE_MANAGER");
    public static final SimpleGrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");
    public static final SimpleGrantedAuthority ANALYST = new SimpleGrantedAuthority("ROLE_ANALYST");

    public UserAuthenticator(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User getAuthenticatedUser() {
        UserDetails uDetails = (UserDetails)SecurityContextHolder.getContext()
        .getAuthentication().getPrincipal(); 

        return userRepo.findByUsername(uDetails.getUsername()).get();
    }

    public boolean idMatchesAuthenticatedUser(Long id) {
        if (!userRepo.existsById(id))
            return false;

        User userAtId = userRepo.findById(id).get();
        User authenticatedUser = getAuthenticatedUser();
        return authenticatedUser.getId() == userAtId.getId();
    }
}
