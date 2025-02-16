package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.dto.UserDTO;
import com.czagrzebski.printhelm.web.model.User;
import com.czagrzebski.printhelm.web.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(final UserRepository userRepository, final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserDTO userDTO) {
        String encodedPassword = passwordEncoder.encode(userDTO.getPlaintextPassword());
        User newUser = new User(userDTO.getUsername(), encodedPassword, userDTO.getFirstName(), userDTO.getLastName());
        newUser.setActive(true);
        newUser.setUserRoles(null);
        userRepository.save(newUser);
        return newUser;
    }

}
