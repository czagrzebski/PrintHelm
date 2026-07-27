package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.RoleApi;
import com.czagrzebski.printhelm.model.ApiRole;
import com.czagrzebski.printhelm.web.mapper.RoleResponseMapper;
import com.czagrzebski.printhelm.web.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(value="/api")
public class RoleController implements RoleApi {

    private final UserService userService;
    private final RoleResponseMapper roleResponseMapper;

    public RoleController(UserService userService, RoleResponseMapper roleResponseMapper) {
        this.userService = userService;
        this.roleResponseMapper = roleResponseMapper;
    }

    @Override
    public ResponseEntity<List<ApiRole>> getRoles() {
        return ResponseEntity.ok(roleResponseMapper.rolesToApiRoles(userService.getAllRoles()));
    }
}
