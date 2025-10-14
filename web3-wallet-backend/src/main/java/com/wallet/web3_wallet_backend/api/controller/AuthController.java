package com.wallet.web3_wallet_backend.api.controller;

import com.wallet.web3_wallet_backend.api.dto.AuthResponse;
import com.wallet.web3_wallet_backend.api.dto.LoginRequest;
import com.wallet.web3_wallet_backend.api.dto.RegisterRequest;
import com.wallet.web3_wallet_backend.model.User;
import com.wallet.web3_wallet_backend.security.jwt.JwtTokenProvider;
import com.wallet.web3_wallet_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for user registration and login.
 * Provides JWT-based authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager,
                         UserService userService,
                         JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Registers a new user.
     *
     * @param request Registration request with username, email, and password
     * @return Response with success message
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(
                    request.username(),
                    request.email(),
                    request.password()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponse("User registered successfully: " + user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request Login request with username and password
     * @return Response with JWT token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            User user = userService.findByUsername(request.username());

            return ResponseEntity.ok(new AuthResponse(jwt, user.getUsername(), user.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid username or password"));
        }
    }

    /**
     * Simple message response record for API responses.
     */
    public record MessageResponse(String message) {}
}
