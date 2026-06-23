package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiGcodeFilamentInfo;
import com.czagrzebski.printhelm.model.ApiGcodeMetadata;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiQuoteLineItem;
import com.czagrzebski.printhelm.model.ApiUpdateJobOrderRequest;
import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.QuoteLineItem;
import com.czagrzebski.printhelm.web.mapper.JobOrderMapper;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobOrderService {

    private final JobOrderRepository jobOrderRepository;
    private final JobOrderMapper jobOrderMapper;
    private final JobOrderFileService jobOrderFileService;
    private final ThreeMfParserService threeMfParserService;
    private final GcodeMetadataRepository gcodeMetadataRepository;
    private final InvoiceService invoiceService;
    private final QuoteService quoteService;

    public JobOrderService(JobOrderRepository jobOrderRepository,
                           JobOrderMapper jobOrderMapper,
                           JobOrderFileService jobOrderFileService,
                           ThreeMfParserService threeMfParserService,
                           GcodeMetadataRepository gcodeMetadataRepository,
                           InvoiceService invoiceService,
                           QuoteService quoteService) {
        this.jobOrderRepository = jobOrderRepository;
        this.jobOrderMapper = jobOrderMapper;
        this.jobOrderFileService = jobOrderFileService;
        this.threeMfParserService = threeMfParserService;
        this.gcodeMetadataRepository = gcodeMetadataRepository;
        this.invoiceService = invoiceService;
        this.quoteService = quoteService;
    }

    public List<ApiJobOrderResponse> getAllOrders() {
        return jobOrderRepository.findAll().stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    public ApiJobOrderResponse getOrderById(long id) {
        return toEnrichedResponse(findOrThrow(id));
    }

    public ApiJobOrderResponse createOrder(ApiCreateJobOrderRequest request) {
        JobOrder order = jobOrderMapper.apiCreateJobOrderRequestToJobOrder(request);
        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    @Transactional
    public ApiJobOrderResponse updateOrder(long id, ApiUpdateJobOrderRequest request) {
        JobOrder order = findOrThrow(id);
        if (request.getCustomerName() != null) order.setCustomerName(request.getCustomerName());
        if (request.getCustomerEmail() != null) order.setCustomerEmail(request.getCustomerEmail());
        if (request.getDescription() != null) order.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            JobOrderStatus next = JobOrderStatus.valueOf(request.getStatus().getValue());
            if (next == JobOrderStatus.PRINTING) {
                throw new IllegalArgumentException("Status PRINTING is managed by the scheduler and cannot be set manually");
            }
            order.setStatus(next);
            if (next == JobOrderStatus.QUOTED && order.getQuotedAt() == null) {
                order.setQuotedAt(LocalDateTime.now());
            }
            if (next == JobOrderStatus.INVOICED && order.getInvoicedAt() == null) {
                order.setInvoicedAt(LocalDateTime.now());
            }
        }
        if (request.getRequirements() != null) order.setRequirements(request.getRequirements());
        if (request.getRequiresCustomDesign() != null) order.setRequiresCustomDesign(request.getRequiresCustomDesign());
        if (request.getQuotedMaterialCost() != null) order.setQuotedMaterialCost(BigDecimal.valueOf(request.getQuotedMaterialCost()));
        if (request.getQuotedCostPerUnit() != null) order.setQuotedCostPerUnit(BigDecimal.valueOf(request.getQuotedCostPerUnit()));
        if (request.getQuotedQuantity() != null) order.setQuotedQuantity(request.getQuotedQuantity());
        if (request.getQuotedLaborCost() != null) order.setQuotedLaborCost(BigDecimal.valueOf(request.getQuotedLaborCost()));
        if (request.getQuotedSetupFee() != null) order.setQuotedSetupFee(BigDecimal.valueOf(request.getQuotedSetupFee()));
        if (request.getQuotedDiscount() != null) order.setQuotedDiscount(BigDecimal.valueOf(request.getQuotedDiscount()));
        if (request.getQuoteNotes() != null) order.setQuoteNotes(request.getQuoteNotes());
        if (request.getQuoteExpiresAt() != null) order.setQuoteExpiresAt(request.getQuoteExpiresAt());
        if (request.getQuoteMaterials() != null) order.setQuoteMaterials(request.getQuoteMaterials());
        if (request.getQuoteLineItems() != null) {
            List<QuoteLineItem> lineItems = request.getQuoteLineItems().stream()
                    .map(api -> new QuoteLineItem(api.getLabel(), api.getAmount() != null ? BigDecimal.valueOf(api.getAmount()) : null))
                    .collect(Collectors.toList());
            order.setQuoteLineItems(lineItems);
        }
        if (request.getMaterialCost() != null) order.setMaterialCost(BigDecimal.valueOf(request.getMaterialCost()));
        if (request.getLaborCost() != null) order.setLaborCost(BigDecimal.valueOf(request.getLaborCost()));
        if (request.getSetupFee() != null) order.setSetupFee(BigDecimal.valueOf(request.getSetupFee()));
        if (request.getDiscount() != null) order.setDiscount(BigDecimal.valueOf(request.getDiscount()));
        if (request.getInvoiceNotes() != null) order.setInvoiceNotes(request.getInvoiceNotes());
        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    @Transactional
    public ApiJobOrderResponse uploadPartFile(long id, MultipartFile file) throws IOException {
        JobOrder order = findOrThrow(id);
        if (order.getMongoPartFileId() != null) {
            jobOrderFileService.deleteFile(order.getMongoPartFileId());
        }
        String fileId = jobOrderFileService.storeFile(id, "part", file);
        order.setMongoPartFileId(fileId);
        order.setPartFilename(file.getOriginalFilename());
        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    @Transactional
    public ApiJobOrderResponse uploadGcodeFile(long id, MultipartFile file) throws IOException {
        JobOrder order = findOrThrow(id);
        if (order.getMongoGcodeFileId() != null) {
            jobOrderFileService.deleteFile(order.getMongoGcodeFileId());
        }
        if (order.getMongoGcodeMetadataId() != null) {
            gcodeMetadataRepository.deleteById(order.getMongoGcodeMetadataId());
            order.setMongoGcodeMetadataId(null);
        }

        String fileId = jobOrderFileService.storeFile(id, "gcode", file);
        order.setMongoGcodeFileId(fileId);
        order.setGcodeFilename(file.getOriginalFilename());

        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".3mf")) {
            ThreeMfParserService.ParsedThreeMfData parsed = threeMfParserService.parse(file.getInputStream());
            if (!parsed.getFilaments().isEmpty()) {
                GcodeMetadata meta = new GcodeMetadata();
                meta.setOrderId(id);
                meta.setFilaments(parsed.getFilaments());
                meta.setMultiColor(parsed.isMultiColor());
                meta.setColorCount(parsed.getColorCount());
                GcodeMetadata saved = gcodeMetadataRepository.save(meta);
                order.setMongoGcodeMetadataId(saved.getId());
            }
        }

        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    public byte[] generateInvoice(long id) {
        return invoiceService.generate(findOrThrow(id));
    }

    public byte[] generateQuote(long id) {
        return quoteService.generate(findOrThrow(id));
    }

    public void deleteOrder(long id) {
        JobOrder order = findOrThrow(id);
        if (order.getMongoPartFileId() != null) {
            jobOrderFileService.deleteFile(order.getMongoPartFileId());
        }
        if (order.getMongoGcodeFileId() != null) {
            jobOrderFileService.deleteFile(order.getMongoGcodeFileId());
        }
        if (order.getMongoGcodeMetadataId() != null) {
            gcodeMetadataRepository.deleteById(order.getMongoGcodeMetadataId());
        }
        jobOrderRepository.delete(order);
    }

    private ApiJobOrderResponse toEnrichedResponse(JobOrder order) {
        ApiJobOrderResponse response = jobOrderMapper.jobOrderToApiJobOrderResponse(order);
        if (order.getMongoGcodeMetadataId() != null) {
            gcodeMetadataRepository.findById(order.getMongoGcodeMetadataId()).ifPresent(meta ->
                    response.setGcodeMetadata(toApiGcodeMetadata(meta)));
        }
        if (order.getQuoteLineItems() != null && !order.getQuoteLineItems().isEmpty()) {
            List<ApiQuoteLineItem> apiItems = order.getQuoteLineItems().stream()
                    .map(item -> {
                        ApiQuoteLineItem api = new ApiQuoteLineItem();
                        api.setLabel(item.getLabel());
                        api.setAmount(item.getAmount() != null ? item.getAmount().doubleValue() : null);
                        return api;
                    })
                    .collect(Collectors.toList());
            response.setQuoteLineItems(apiItems);
        }
        return response;
    }

    private ApiGcodeMetadata toApiGcodeMetadata(GcodeMetadata meta) {
        ApiGcodeMetadata api = new ApiGcodeMetadata();
        api.setMultiColor(meta.isMultiColor());
        api.setColorCount(meta.getColorCount());
        if (meta.getFilaments() != null) {
            api.setFilaments(meta.getFilaments().stream()
                    .map(this::toApiFilamentInfo)
                    .collect(Collectors.toList()));
        }
        return api;
    }

    private ApiGcodeFilamentInfo toApiFilamentInfo(GcodeFilamentInfo f) {
        ApiGcodeFilamentInfo api = new ApiGcodeFilamentInfo();
        api.setSlotIndex(f.getSlotIndex());
        api.setType(f.getType());
        api.setColor(f.getColor());
        return api;
    }

    private JobOrder findOrThrow(long id) {
        return jobOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job order not found: " + id));
    }
}
