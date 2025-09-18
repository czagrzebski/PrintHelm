package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiAuthResponse;
import com.czagrzebski.printhelm.web.dto.AuthenticationDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, uses = {UserResponseMapper.class})
public interface AuthResponseMapper {
    ApiAuthResponse authenticationDTOToApiAuthResponse(AuthenticationDTO authenticationDTO);
}
