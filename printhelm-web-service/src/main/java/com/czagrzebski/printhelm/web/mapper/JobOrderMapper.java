package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class JobOrderMapper {

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localToOffset")
    public abstract ApiJobOrderResponse jobOrderToApiJobOrderResponse(JobOrder order);

    public abstract List<ApiJobOrderResponse> jobOrdersToApiJobOrderResponses(List<JobOrder> orders);

    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requirements", ignore = true)
    @Mapping(target = "requiresCustomDesign", ignore = true)
    @Mapping(target = "mongoPartFileId", ignore = true)
    @Mapping(target = "mongoGcodeFileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    public abstract JobOrder apiCreateJobOrderRequestToJobOrder(ApiCreateJobOrderRequest request);

    @Named("localToOffset")
    public OffsetDateTime localToOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
