package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.UserApi;
import com.czagrzebski.printhelm.model.ApiAdminResetPasswordRequest;
import com.czagrzebski.printhelm.model.ApiChangePasswordRequest;
import com.czagrzebski.printhelm.model.ApiCreateUserRequest;
import com.czagrzebski.printhelm.model.ApiUpdateUserRequest;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<ApiUserResponse> createUser(ApiCreateUserRequest req) {
        try {
            logger.info("Creating new user [Username={}]", req.getUsername());
            List<RoleDTO> roleDTOList = req.getRoles().stream()
                    .map(role -> { RoleDTO dto = new RoleDTO(); dto.setRoleId(role.getRoleId()); dto.setRoleName(role.getName()); return dto; })
                    .toList();
            User newUser = userService.createUser(req.getUsername(), req.getPassword(), req.getFirstName(), req.getLastName(), roleDTOList);
            return new ResponseEntity<>(userResponseMapper.userToApiUserResponse(newUser), HttpStatus.CREATED);
        } catch (Exception e) {
            logger.warn("Failed to create user [Username={}]: {}", req.getUsername(), e.getMessage());
            throw new PrintHelmException("Failed to create user: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<List<ApiUserResponse>> getUsers() {
        List<ApiUserResponse> users = userService.getAllUsers().stream()
                .map(userResponseMapper::userToApiUserResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Override
    public ResponseEntity<ApiUserResponse> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(userResponseMapper.userToApiUserResponse(user));
    }

    @Override
    public ResponseEntity<ApiUserResponse> getUserById(Long id) {
        return ResponseEntity.ok(userResponseMapper.userToApiUserResponse(userService.getUserById(id)));
    }

    @Override
    public ResponseEntity<ApiUserResponse> updateUser(Long id, ApiUpdateUserRequest req) {
        List<RoleDTO> roleDTOList = req.getRoles() == null ? null : req.getRoles().stream()
                .map(role -> { RoleDTO dto = new RoleDTO(); dto.setRoleId(role.getRoleId()); dto.setRoleName(role.getName()); return dto; })
                .toList();
        User updated = userService.updateUser(id, req.getUsername(), req.getFirstName(), req.getLastName(), req.getIsActive(), roleDTOList);
        return ResponseEntity.ok(userResponseMapper.userToApiUserResponse(updated));
    }

    @Override
    public ResponseEntity<Void> deleteUser(Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> changeMyPassword(ApiChangePasswordRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        userService.changePassword(user.getUserId(), req.getCurrentPassword(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> adminResetPassword(Long id, ApiAdminResetPasswordRequest req) {
        boolean mustChange = req.getMustChangePassword() != null && req.getMustChangePassword();
        userService.adminResetPassword(id, req.getNewPassword(), mustChange);
        return ResponseEntity.noContent().build();
    }
}
