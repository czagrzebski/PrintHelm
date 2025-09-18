package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiRole;
import com.czagrzebski.printhelm.web.domain.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface RoleResponseMapper {

    @Mapping(target = "name", source = "roleName")
    ApiRole roleToApiRole(Role role);

    @Mapping(target = "roleName", source = "name")
    Role apiRoleToRole(ApiRole apiRole);
}
