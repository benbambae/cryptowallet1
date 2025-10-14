package com.wallet.web3_wallet_backend.service;

import com.wallet.web3_wallet_backend.model.User;
import com.wallet.web3_wallet_backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * User service implementing UserDetailsService for Spring Security authentication.
 * Handles user registration, retrieval, and password encoding.
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Loads user by username for Spring Security authentication.
     *
     * @param username Username to search for
     * @return UserDetails object for authentication
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.getEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    /**
     * Registers a new user with username, email, and password.
     *
     * @param username Username for the new user
     * @param email Email for the new user
     * @param password Plain text password (will be hashed)
     * @return Created User entity
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public User registerUser(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, email, hashedPassword);

        return userRepository.save(user);
    }

    /**
     * Finds a user by username.
     *
     * @param username Username to search for
     * @return User entity if found
     * @throws UsernameNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Checks if a username exists.
     *
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Checks if an email exists.
     *
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Gets the password encoder instance.
     *
     * @return PasswordEncoder instance
     */
    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
