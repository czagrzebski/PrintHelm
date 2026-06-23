package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiGcodeFilamentInfo;
import com.czagrzebski.printhelm.model.ApiGcodeMetadata;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class JobOrderMapper {

    @Autowired
    protected GcodeMetadataRepository gcodeMetadataRepository;

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localToOffset")
    @Mapping(target = "quotedAt", source = "quotedAt", qualifiedByName = "localToOffset")
    @Mapping(target = "invoicedAt", source = "invoicedAt", qualifiedByName = "localToOffset")
    @Mapping(target = "assignedPrinterId", source = "assignedPrinter.printerId")
    @Mapping(target = "gcodeMetadata", source = "mongoGcodeMetadataId", qualifiedByName = "loadGcodeMetadata")
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

    @Named("loadGcodeMetadata")
    protected ApiGcodeMetadata loadGcodeMetadata(String mongoGcodeMetadataId) {
        if (mongoGcodeMetadataId == null) return null;
        return gcodeMetadataRepository.findById(mongoGcodeMetadataId).map(meta -> {
            ApiGcodeMetadata apiMeta = new ApiGcodeMetadata();
            apiMeta.setMultiColor(meta.isMultiColor());
            apiMeta.setColorCount(meta.getColorCount());
            if (meta.getFilaments() != null) {
                List<ApiGcodeFilamentInfo> filaments = meta.getFilaments().stream().map(f -> {
                    ApiGcodeFilamentInfo info = new ApiGcodeFilamentInfo();
                    info.setSlotIndex(f.getSlotIndex());
                    info.setType(f.getType());
                    info.setColor(f.getColor());
                    return info;
                }).collect(Collectors.toList());
                apiMeta.setFilaments(filaments);
            }
            return apiMeta;
        }).orElse(null);
    }

    @Named("localToOffset")
    public OffsetDateTime localToOffset(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
