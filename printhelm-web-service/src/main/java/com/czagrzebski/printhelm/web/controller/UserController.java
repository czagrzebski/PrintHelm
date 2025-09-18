package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.UserApi;
import com.czagrzebski.printhelm.model.ApiCreateUserRequest;
import com.czagrzebski.printhelm.model.ApiUserResponse;
import com.czagrzebski.printhelm.web.dto.RoleDTO;
import com.czagrzebski.printhelm.web.exception.PrintHelmException;
import com.czagrzebski.printhelm.web.mapper.UserResponseMapper;
import com.czagrzebski.printhelm.web.domain.User;
import com.czagrzebski.printhelm.web.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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

    @Override
    public ResponseEntity<ApiUserResponse> createUser(ApiCreateUserRequest apiCreateUserRequest) {
        try {
            logger.info("Creating new user [Username={}]", apiCreateUserRequest.getUsername());

            List<RoleDTO> roleDTOList = apiCreateUserRequest.getRoles().stream()
                    .map(role -> {
                        RoleDTO roleDTO = new RoleDTO();
                        roleDTO.setRoleId(role.getRoleId());
                        roleDTO.setRoleName(role.getName());
                        return roleDTO;
                    })
                    .toList();
            User newUser = userService.createUser(
                    apiCreateUserRequest.getUsername(),
                    apiCreateUserRequest.getPassword(),
                    apiCreateUserRequest.getFirstName(),
                    apiCreateUserRequest.getLastName(),
                    roleDTOList
            );
            ApiUserResponse apiUserResponse = userResponseMapper.userToApiUserResponse(newUser);
            return new ResponseEntity<>(apiUserResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.info("Failed to create new user [Username={}]", apiCreateUserRequest.getUsername());
            throw new PrintHelmException("Failed to create user: " + e.getMessage(), e);
        }
    }
}
