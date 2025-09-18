package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.bambulab.BambuLabPrinter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, uses = {ConnectionConfigurationMapper.class})
public abstract class PrinterMapper {

    @Mapping(target = "connectionConfig", source = "apiCreatePrinterRequest.connectionConfig")
    public abstract BambuLabPrinter apiCreatePrinterRequestToBambuLabPrinter(ApiCreatePrinterRequest apiCreatePrinterRequest);
}


