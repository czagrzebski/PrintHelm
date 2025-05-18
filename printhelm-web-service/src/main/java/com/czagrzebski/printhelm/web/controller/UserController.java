package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.UserApi;
import com.czagrzebski.printhelm.model.ApiCreateUserRequest;
import com.czagrzebski.printhelm.model.ApiUserResponse;
import com.czagrzebski.printhelm.web.dto.AuthenticationDTO;
import com.czagrzebski.printhelm.web.dto.ResponseDTO;
import com.czagrzebski.printhelm.web.dto.UserDTO;
import com.czagrzebski.printhelm.web.mapper.UserResponseMapper;
import com.czagrzebski.printhelm.web.model.User;
import com.czagrzebski.printhelm.web.service.UserService;
import jakarta.servlet.http.Cookie;
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
@RequestMapping(value="/api")
public class UserController implements UserApi {

    private final UserService userService;
    private final UserResponseMapper userResponseMapper;
    private static final Logger logger = LogManager.getLogger(UserController.class);

    public UserController(final UserService userService, final UserResponseMapper userResponseMapper) {
        this.userService = userService;
        this.userResponseMapper = userResponseMapper;
    }

    @PostMapping(value="/refreshToken")
    public ResponseEntity<ResponseDTO<AuthenticationDTO>> refreshToken(@RequestBody AuthenticationDTO authenticationDTO, jakarta.servlet.http.HttpServletResponse response) {
        try {
            logger.info("Refreshing token for user [Username={}]", authenticationDTO.getUsername());
            var authentication  = userService.refreshToken(authenticationDTO);

            // Create cookie with refresh token
            Cookie cookie = new Cookie("refreshToken", authentication.getRefreshToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");

            authentication.setRefreshToken(null); // Clear refresh token from response

            // Set cookie expiration to 7 days
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days in seconds

            // add the cookie to the response
            response.addCookie(cookie);

            return new ResponseEntity<>(new ResponseDTO<>("Successfully refreshed token!", authentication), HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Failed to refresh token for user [Username={}]", authenticationDTO.getUsername());
            return new ResponseEntity<>(new ResponseDTO<>(String.format("Failed to refresh token! %s", e.toString()), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value="/login")
    public ResponseEntity<ResponseDTO<AuthenticationDTO>> loginUser(@RequestBody AuthenticationDTO authenticationDTO, jakarta.servlet.http.HttpServletResponse response) {
        try {
            logger.info("Logging in user [Username={}]", authenticationDTO.getUsername());
            var authentication = userService.verifyUser(authenticationDTO);

            // Create cookie with refresh token
            Cookie cookie = new Cookie("refreshToken", authentication.getRefreshToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");

            authentication.setRefreshToken(null); // Clear refresh token from response
            // Set cookie expiration to 7 days
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days in seconds
            // add the cookie to the response
            authenticationDTO.setRefreshToken(null);

            response.addCookie(cookie);

            return new ResponseEntity<>(new ResponseDTO<>("Successfully logged in!", authentication), HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Failed to log in user [Username={}]", authenticationDTO.getUsername());
            return new ResponseEntity<>(new ResponseDTO<>(String.format("Failed to log in user! %s", e.toString()), null), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ApiUserResponse> createUser(ApiCreateUserRequest apiCreateUserRequest) {
        try {
            logger.info("Creating new user [Username={}]", apiCreateUserRequest.getUsername());
            User newUser = userService.createUser(
                    apiCreateUserRequest.getUsername(),
                    apiCreateUserRequest.getPassword(),
                    apiCreateUserRequest.getFirstName(),
                    apiCreateUserRequest.getLastName()
            );
            ApiUserResponse apiUserResponse = userResponseMapper.userToApiUserResponse(newUser);
            return new ResponseEntity<>(apiUserResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.info("Failed to create new user [Username={}]", apiCreateUserRequest.getUsername());
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }
}
