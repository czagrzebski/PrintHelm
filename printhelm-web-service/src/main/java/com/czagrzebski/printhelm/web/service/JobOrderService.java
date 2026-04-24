package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiGcodeFilamentInfo;
import com.czagrzebski.printhelm.model.ApiGcodeMetadata;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiUpdateJobOrderRequest;
import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.mapper.JobOrderMapper;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobOrderService {

    private final JobOrderRepository jobOrderRepository;
    private final JobOrderMapper jobOrderMapper;
    private final JobOrderFileService jobOrderFileService;
    private final ThreeMfParserService threeMfParserService;
    private final GcodeMetadataRepository gcodeMetadataRepository;

    public JobOrderService(JobOrderRepository jobOrderRepository,
                           JobOrderMapper jobOrderMapper,
                           JobOrderFileService jobOrderFileService,
                           ThreeMfParserService threeMfParserService,
                           GcodeMetadataRepository gcodeMetadataRepository) {
        this.jobOrderRepository = jobOrderRepository;
        this.jobOrderMapper = jobOrderMapper;
        this.jobOrderFileService = jobOrderFileService;
        this.threeMfParserService = threeMfParserService;
        this.gcodeMetadataRepository = gcodeMetadataRepository;
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
        }
        if (request.getRequirements() != null) order.setRequirements(request.getRequirements());
        if (request.getRequiresCustomDesign() != null) order.setRequiresCustomDesign(request.getRequiresCustomDesign());
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
