package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.dto.AuthenticationDTO;
import com.czagrzebski.printhelm.web.dto.UserDTO;
import com.czagrzebski.printhelm.web.model.User;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public UserService(final UserRepository userRepository, final PasswordEncoder passwordEncoder, final AuthenticationManager authenticationManager, final JWTService jwtService, final UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public AuthenticationDTO verifyUser(AuthenticationDTO authenticationDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationDTO.getUsername(), authenticationDTO.getPassword()));

        if (authentication.isAuthenticated()) {
            var authenticationResponse = new AuthenticationDTO();
            authenticationResponse.setUsername(authenticationDTO.getUsername());
            authenticationResponse.setAccessToken(jwtService.generateToken(authenticationDTO.getUsername(), JWTService.TokenType.ACCESS));
            authenticationResponse.setRefreshToken(jwtService.generateToken(authenticationDTO.getUsername(), JWTService.TokenType.REFRESH));
            return authenticationResponse;
        } else {
            throw new RuntimeException("Authentication failed");
        }
    }

    public AuthenticationDTO refreshToken(AuthenticationDTO authenticationDTO) {
        // check for valid refresh token
        String username = jwtService.extractUsername(authenticationDTO.getRefreshToken());
        if (username == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // check if refresh token is valid
            if (!jwtService.validateToken(authenticationDTO.getRefreshToken(), userDetails)) {
                throw new RuntimeException("Invalid refresh token");
            }
            String newAccessToken = jwtService.generateToken(authenticationDTO.getUsername(), JWTService.TokenType.ACCESS);
            String newRefreshToken = jwtService.generateToken(authenticationDTO.getUsername(), JWTService.TokenType.REFRESH);
            var authentication = new AuthenticationDTO();
            authentication.setUsername(authenticationDTO.getUsername());
            authentication.setAccessToken(newAccessToken);
            authentication.setRefreshToken(newRefreshToken);
            return authentication;
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    public User createUser(String username, String password, String firstName, String lastName) {
        // Check if the user already exists
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("User already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, encodedPassword, firstName, lastName);
        newUser.setActive(true);
        newUser.setUserRoles(null);
        userRepository.save(newUser);
        return newUser;
    }

}
