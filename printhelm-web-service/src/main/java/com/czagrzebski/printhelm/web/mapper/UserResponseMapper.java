package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiUserResponse;
import com.czagrzebski.printhelm.web.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserResponseMapper {
    ApiUserResponse userToApiUserResponse(User user);
}
