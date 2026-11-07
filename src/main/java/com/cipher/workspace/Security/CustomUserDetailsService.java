package com.cipher.workspace.Security;

import com.cipher.workspace.Entity.User;
import com.cipher.workspace.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException("Identifier cannot be empty");
        }

        return userRepository.findByUsernameOrEmail(identifier.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
    }
}
