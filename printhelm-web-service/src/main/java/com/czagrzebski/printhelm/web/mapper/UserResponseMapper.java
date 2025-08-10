package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiUserResponse;
import com.czagrzebski.printhelm.web.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, uses = {RoleResponseMapper.class})
public interface UserResponseMapper {

    @Mapping(target = "roles", source = "userRoles")
    ApiUserResponse userToApiUserResponse(User user);
}
