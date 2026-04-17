package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.dto.AuthenticationDTO;
import com.czagrzebski.printhelm.web.dto.RoleDTO;
import com.czagrzebski.printhelm.web.domain.Role;
import com.czagrzebski.printhelm.web.domain.User;
import com.czagrzebski.printhelm.web.repository.RoleRepository;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String username = jwtService.extractUsername(refreshToken, JWTService.TokenType.REFRESH);
        if (username == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.validateToken(refreshToken, userDetails, JWTService.TokenType.REFRESH)) {
                throw new RuntimeException("Invalid refresh token");
            }
            String newAccessToken = jwtService.generateToken(userDetails.getUsername(), JWTService.TokenType.ACCESS);
            String newRefreshToken = jwtService.generateToken(userDetails.getUsername(), JWTService.TokenType.REFRESH);

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
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("User already exists");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User newUser = new User(username, encodedPassword, firstName, lastName);
        newUser.setActive(true);

        Set<Role> roles = new HashSet<>();
        if (rolesDTOList != null && !rolesDTOList.isEmpty()) {
            for (RoleDTO roleDTO : rolesDTOList) {
                Role role = roleRepository.findByRoleName(roleDTO.getRoleName());
                if (role != null) {
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) throw new RuntimeException("User not found: " + username);
        return user;
    }

    @Transactional
    public User updateUser(Long id, String username, String firstName, String lastName, Boolean isActive, List<RoleDTO> rolesDTOList) {
        User user = getUserById(id);

        if (username != null && !username.isBlank()) user.setUsername(username);
        if (firstName != null) user.setFirstname(firstName);
        if (lastName != null) user.setLastname(lastName);
        if (isActive != null) user.setActive(isActive);

        if (rolesDTOList != null) {
            Set<Role> roles = new HashSet<>();
            for (RoleDTO roleDTO : rolesDTOList) {
                Role role = roleRepository.findByRoleName(roleDTO.getRoleName());
                if (role != null) {
                    roles.add(role);
                } else {
                    throw new RuntimeException("Role not found: " + roleDTO.getRoleName());
                }
            }
            user.setUserRoles(roles);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = getUserById(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Transactional
    public void adminResetPassword(Long id, String newPassword, boolean mustChangePassword) {
        User user = getUserById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(mustChangePassword);
        userRepository.save(user);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
