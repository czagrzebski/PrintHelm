package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.web.dto.UserDTO;
import com.czagrzebski.printhelm.web.model.User;
import com.czagrzebski.printhelm.web.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value="/api/user")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LogManager.getLogger(UserController.class);

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value="/createUser")
    public ResponseEntity<String> createNewUser(@RequestBody UserDTO userDTO) {
        try {
            logger.info("Creating new user [Username={}]", userDTO.getUsername());
            User newUser = userService.createUser(userDTO);
            return new ResponseEntity<String>(String.format("Successfully created user with id %d!", newUser.getUserId()), HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Failed to create new user [Username={}]", userDTO.getUsername());
            return new ResponseEntity<String>(String.format("Failed to create user! %s", e.toString()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value="/allUsers")
    public ResponseEntity<String> getAllUsers() {
        return new ResponseEntity<String>("This is a test", HttpStatus.OK);
    }
}
