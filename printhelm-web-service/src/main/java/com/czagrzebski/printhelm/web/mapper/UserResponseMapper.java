package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiUserResponse;
import com.czagrzebski.printhelm.web.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, uses = {RoleResponseMapper.class})
public interface UserResponseMapper {

    @Mapping(target = "roles", source = "userRoles")
    @Mapping(target = "firstName", source = "firstname")
    @Mapping(target = "lastName", source = "lastname")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "mustChangePassword", source = "mustChangePassword")
    ApiUserResponse userToApiUserResponse(User user);
}
