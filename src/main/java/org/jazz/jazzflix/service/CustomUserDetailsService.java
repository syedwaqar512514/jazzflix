package org.jazz.jazzflix.service;

import org.jazz.jazzflix.config.security.CustomUserDetails;
import org.jazz.jazzflix.entity.user.User;
import org.jazz.jazzflix.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = Optional.ofNullable(userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not Found for username: " + email)
        ));
        return user.map(CustomUserDetails::new).orElse(null);
    }
}
