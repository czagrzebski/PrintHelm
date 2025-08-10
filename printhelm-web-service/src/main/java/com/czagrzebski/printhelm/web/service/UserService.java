package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.dto.AuthenticationDTO;
import com.czagrzebski.printhelm.web.dto.RoleDTO;
import com.czagrzebski.printhelm.web.domain.Role;
import com.czagrzebski.printhelm.web.domain.User;
import com.czagrzebski.printhelm.web.repository.RoleRepository;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final RoleRepository roleRepository;
    private final UserDetailsServiceImpl userDetailsService;

    public UserService(final UserRepository userRepository,
                       final PasswordEncoder passwordEncoder,
                       final AuthenticationManager authenticationManager,
                       final JWTService jwtService,
                       final UserDetailsServiceImpl userDetailsService,
                       final RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.roleRepository = roleRepository;
    }

    public AuthenticationDTO authenticateUser(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        if (authentication.isAuthenticated()) {
            var authenticationDTO = new AuthenticationDTO();
            var user = userRepository.findByUsername(username);
            authenticationDTO.setUsername(username);
            authenticationDTO.setAccessToken(jwtService.generateToken(username, JWTService.TokenType.ACCESS));
            authenticationDTO.setRefreshToken(jwtService.generateToken(username, JWTService.TokenType.REFRESH));
            authenticationDTO.setUser(user);
            return authenticationDTO;
        } else {
            throw new RuntimeException("Authentication failed");
        }
    }

    public AuthenticationDTO refreshToken(String refreshToken) {
        // check for valid refresh token
        String username = jwtService.extractUsername(refreshToken, JWTService.TokenType.REFRESH);
        if (username == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // check if refresh token is valid
            if (!jwtService.validateToken(refreshToken, userDetails, JWTService.TokenType.REFRESH)) {
                throw new RuntimeException("Invalid refresh token");
            }
            String newAccessToken = jwtService.generateToken(userDetails.getUsername(), JWTService.TokenType.ACCESS);
            String newRefreshToken = jwtService.generateToken(userDetails.getUsername(), JWTService.TokenType.REFRESH);

            // Get the user
            User user = userRepository.findByUsername(userDetails.getUsername());
            var authentication = new AuthenticationDTO();
            authentication.setUsername(userDetails.getUsername());
            authentication.setAccessToken(newAccessToken);
            authentication.setRefreshToken(newRefreshToken);
            authentication.setUser(user);
            return authentication;
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    public User createUser(String username, String password, String firstName, String lastName, List<RoleDTO> rolesDTOList) {
        // Check if the user already exists
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("User already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, encodedPassword, firstName, lastName);
        newUser.setActive(true);

        // Set roles
        Set<Role> roles = new HashSet<>();

        if (rolesDTOList != null && !rolesDTOList.isEmpty()) {
            for (RoleDTO roleDTO : rolesDTOList) {
                Role role = roleRepository.findByRoleName(roleDTO.getRoleName());
                if(role != null) {
                    roles.add(role);
                } else {
                    throw new RuntimeException("Role not found: " + roleDTO.getRoleName());
                }
            }
            newUser.setUserRoles(roles);
        }
        userRepository.save(newUser);
        return newUser;
    }

}
