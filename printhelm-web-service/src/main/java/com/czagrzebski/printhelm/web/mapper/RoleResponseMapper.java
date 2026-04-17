package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiRole;
import com.czagrzebski.printhelm.web.domain.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RoleResponseMapper {

    @Mapping(target = "name", source = "roleName")
    @Mapping(target = "description", source = "roleDescription")
    ApiRole roleToApiRole(Role role);

    @Mapping(target = "roleName", source = "name")
    @Mapping(target = "roleDescription", source = "description")
    Role apiRoleToRole(ApiRole apiRole);

    List<ApiRole> rolesToApiRoles(List<Role> roles);
}
