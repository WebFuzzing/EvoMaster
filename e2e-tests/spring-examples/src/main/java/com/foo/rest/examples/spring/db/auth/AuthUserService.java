package com.foo.rest.examples.spring.db.auth;


import com.foo.rest.examples.spring.db.auth.db.AuthUserEntity;
import com.foo.rest.examples.spring.db.auth.db.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthUserService implements UserDetailsService {

    @Autowired
    private AuthUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUserEntity u = repository.findById(username).orElse(null);
        if (u == null) {
            throw new UsernameNotFoundException("Username not found: " + username);
        }
        return new User(username, u.getPassword(), Collections.emptySet());
    }
}
