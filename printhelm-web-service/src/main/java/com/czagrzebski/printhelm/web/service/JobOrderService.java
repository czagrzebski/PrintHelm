package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiGcodeFilamentInfo;
import com.czagrzebski.printhelm.model.ApiGcodeMetadata;
import com.czagrzebski.printhelm.model.ApiJobOrderFileType;
import com.czagrzebski.printhelm.model.ApiJobOrderFileVersion;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiJobOrderVersionFile;
import com.czagrzebski.printhelm.model.ApiVersionFileQuantity;
import com.czagrzebski.printhelm.model.ApiQuoteLineItem;
import com.czagrzebski.printhelm.model.ApiUpdateJobOrderRequest;
import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderFileType;
import com.czagrzebski.printhelm.web.domain.JobOrderFileVersion;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.JobOrderVersionFile;
import com.czagrzebski.printhelm.web.domain.QuoteLineItem;
import com.czagrzebski.printhelm.web.mapper.JobOrderMapper;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderFileVersionRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobOrderService {

    private final JobOrderRepository jobOrderRepository;
    private final JobOrderMapper jobOrderMapper;
    private final JobOrderFileService jobOrderFileService;
    private final ThreeMfParserService threeMfParserService;
    private final GcodeMetadataRepository gcodeMetadataRepository;
    private final JobOrderFileVersionRepository fileVersionRepository;
    private final InvoiceService invoiceService;
    private final QuoteService quoteService;
    private final AuditLogService auditLogService;

    public JobOrderService(JobOrderRepository jobOrderRepository,
                           JobOrderMapper jobOrderMapper,
                           JobOrderFileService jobOrderFileService,
                           ThreeMfParserService threeMfParserService,
                           GcodeMetadataRepository gcodeMetadataRepository,
                           JobOrderFileVersionRepository fileVersionRepository,
                           InvoiceService invoiceService,
                           QuoteService quoteService,
                           AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
        this.jobOrderRepository = jobOrderRepository;
        this.jobOrderMapper = jobOrderMapper;
        this.jobOrderFileService = jobOrderFileService;
        this.threeMfParserService = threeMfParserService;
        this.gcodeMetadataRepository = gcodeMetadataRepository;
        this.fileVersionRepository = fileVersionRepository;
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
        JobOrder saved = jobOrderRepository.save(order);
        auditLogService.record("JOB_ORDER_CREATED", "JobOrder", saved.getOrderId(),
                "Customer: " + saved.getCustomerName());
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ApiJobOrderResponse updateOrder(long id, ApiUpdateJobOrderRequest request) {
        JobOrder order = findOrThrow(id);
        JobOrderStatus previousStatus = order.getStatus();
        if (request.getCustomerName() != null) order.setCustomerName(request.getCustomerName());
        if (request.getCustomerEmail() != null) order.setCustomerEmail(request.getCustomerEmail());
        if (request.getDescription() != null) order.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            JobOrderStatus next = JobOrderStatus.valueOf(request.getStatus().getValue());
            if (next == JobOrderStatus.PRINTING) {
                throw new IllegalArgumentException("Status PRINTING is managed by the scheduler and cannot be set manually");
            }
            if (next == JobOrderStatus.READY_TO_PRINT && order.getMongoGcodeFileId() == null) {
                throw new IllegalArgumentException("A GCode or 3MF file must be uploaded and selected before the order can be marked Ready to Print");
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
        if (request.getQuotedPrintTimeHours() != null) order.setQuotedPrintTimeHours(BigDecimal.valueOf(request.getQuotedPrintTimeHours()));
        if (request.getQuotedFilamentGrams() != null) order.setQuotedFilamentGrams(BigDecimal.valueOf(request.getQuotedFilamentGrams()));
        if (request.getQuotedLeadTimeDays() != null) order.setQuotedLeadTimeDays(request.getQuotedLeadTimeDays());
        if (request.getDesignNotes() != null) order.setDesignNotes(request.getDesignNotes());
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
        JobOrder saved = jobOrderRepository.save(order);
        auditLogService.record("JOB_ORDER_UPDATED", "JobOrder", id,
                saved.getStatus() != previousStatus
                        ? "Status: " + previousStatus + " → " + saved.getStatus()
                        : "Details updated");
        return toEnrichedResponse(saved);
    }

    @Transactional
    public ApiJobOrderResponse uploadPartFiles(long id, List<MultipartFile> files, String description) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            String lower = name != null ? name.toLowerCase() : "";
            if (!lower.endsWith(".glb") && !lower.endsWith(".gltf")) {
                throw new IllegalArgumentException("Unsupported file type: " + name + " (only .glb and .gltf are accepted)");
            }
        }
        JobOrder order = findOrThrow(id);
        backfillInitialVersion(order, JobOrderFileType.PART);

        List<JobOrderVersionFile> versionFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileId = jobOrderFileService.storeFile(id, "part", file);
            versionFiles.add(new JobOrderVersionFile(file.getOriginalFilename(), fileId));
        }
        createVersion(order, JobOrderFileType.PART, versionFiles, null, description);

        order.setMongoPartFileId(versionFiles.get(0).getMongoFileId());
        order.setPartFilename(versionFiles.get(0).getFilename());
        auditLogService.record("PART_FILE_UPLOADED", "JobOrder", id,
                versionFiles.stream().map(JobOrderVersionFile::getFilename).collect(Collectors.joining(", ")));
        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    @Transactional
    public ApiJobOrderResponse uploadGcodeFiles(long id, List<MultipartFile> files, String description) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            String lower = name != null ? name.toLowerCase() : "";
            if (!lower.endsWith(".gcode") && !lower.endsWith(".3mf")) {
                throw new IllegalArgumentException("Unsupported file type: " + name + " (only .gcode and .3mf are accepted)");
            }
        }
        JobOrder order = findOrThrow(id);
        backfillInitialVersion(order, JobOrderFileType.GCODE);

        List<JobOrderVersionFile> versionFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileId = jobOrderFileService.storeFile(id, "gcode", file);
            versionFiles.add(new JobOrderVersionFile(file.getOriginalFilename(), fileId,
                    extractGcodeMetadataId(id, file)));
        }
        createVersion(order, JobOrderFileType.GCODE, versionFiles,
                versionFiles.get(0).getMongoGcodeMetadataId(), description);

        // The first file of a new upload becomes the file selected for printing
        order.setMongoGcodeFileId(versionFiles.get(0).getMongoFileId());
        order.setGcodeFilename(versionFiles.get(0).getFilename());
        order.setMongoGcodeMetadataId(versionFiles.get(0).getMongoGcodeMetadataId());

        auditLogService.record("GCODE_FILE_UPLOADED", "JobOrder", id,
                versionFiles.stream().map(JobOrderVersionFile::getFilename).collect(Collectors.joining(", ")));
        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    /** Parses filament/color metadata out of a .3mf upload; null for plain gcode or when absent */
    private String extractGcodeMetadataId(long orderId, MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".3mf")) return null;
        ThreeMfParserService.ParsedThreeMfData parsed = threeMfParserService.parse(file.getInputStream());
        if (parsed.getFilaments().isEmpty()) return null;
        GcodeMetadata meta = new GcodeMetadata();
        meta.setOrderId(orderId);
        meta.setFilaments(parsed.getFilaments());
        meta.setMultiColor(parsed.isMultiColor());
        meta.setColorCount(parsed.getColorCount());
        meta.setEstimatedDurationSeconds(parsed.getEstimatedDurationSeconds());
        meta.setTotalWeightGrams(parsed.getTotalWeightGrams());
        return gcodeMetadataRepository.save(meta).getId();
    }

    public List<ApiJobOrderFileVersion> getFileVersions(long id, JobOrderFileType fileType) {
        JobOrder order = findOrThrow(id);
        String activeFileId = fileType == JobOrderFileType.PART
                ? order.getMongoPartFileId()
                : order.getMongoGcodeFileId();
        return fileVersionRepository.findByOrderIdAndFileTypeOrderByVersionNumberDesc(id, fileType).stream()
                .map(v -> toApiFileVersion(v, activeFileId))
                .collect(Collectors.toList());
    }

    public JobOrderFileVersion getFileVersion(long id, long versionId, JobOrderFileType fileType) {
        findOrThrow(id);
        JobOrderFileVersion version = fileVersionRepository.findByVersionIdAndOrderId(versionId, id)
                .orElseThrow(() -> new IllegalArgumentException("File version not found: " + versionId));
        if (version.getFileType() != fileType) {
            throw new IllegalArgumentException("File version " + versionId + " is not a " + fileType + " file");
        }
        return version;
    }

    @Transactional
    public ApiJobOrderResponse selectGcodeVersion(long id, long versionId, int fileIndex) {
        JobOrder order = findOrThrow(id);
        if (order.getStatus() == JobOrderStatus.PRINTING) {
            throw new IllegalStateException("Cannot change the gcode version while the job is printing");
        }
        JobOrderFileVersion version = getFileVersion(id, versionId, JobOrderFileType.GCODE);
        List<JobOrderVersionFile> files = version.getEffectiveFiles();
        if (fileIndex < 0 || fileIndex >= files.size()) {
            throw new IllegalArgumentException("File index " + fileIndex + " not found in version " + versionId);
        }
        JobOrderVersionFile file = files.get(fileIndex);

        order.setMongoGcodeFileId(file.getMongoFileId());
        order.setGcodeFilename(file.getFilename());
        order.setMongoGcodeMetadataId(file.getMongoGcodeMetadataId());
        return toEnrichedResponse(jobOrderRepository.save(order));
    }

    public byte[] generateInvoice(long id) {
        return invoiceService.generate(findOrThrow(id));
    }

    public byte[] generateQuote(long id) {
        return quoteService.generate(findOrThrow(id));
    }

    @Transactional
    public void deleteOrder(long id) {
        JobOrder order = findOrThrow(id);
        List<JobOrderFileVersion> versions = fileVersionRepository.findByOrderId(id);

        Set<String> fileIds = new HashSet<>();
        Set<String> metadataIds = new HashSet<>();
        if (order.getMongoPartFileId() != null) fileIds.add(order.getMongoPartFileId());
        if (order.getMongoGcodeFileId() != null) fileIds.add(order.getMongoGcodeFileId());
        if (order.getMongoGcodeMetadataId() != null) metadataIds.add(order.getMongoGcodeMetadataId());
        for (JobOrderFileVersion version : versions) {
            fileIds.add(version.getMongoFileId());
            for (JobOrderVersionFile file : version.getEffectiveFiles()) {
                fileIds.add(file.getMongoFileId());
                if (file.getMongoGcodeMetadataId() != null) metadataIds.add(file.getMongoGcodeMetadataId());
            }
            if (version.getMongoGcodeMetadataId() != null) metadataIds.add(version.getMongoGcodeMetadataId());
        }

        fileIds.forEach(jobOrderFileService::deleteFile);
        metadataIds.forEach(gcodeMetadataRepository::deleteById);
        fileVersionRepository.deleteAll(versions);
        jobOrderRepository.delete(order);
        auditLogService.record("JOB_ORDER_DELETED", "JobOrder", id,
                "Customer: " + order.getCustomerName());
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
        api.setEstimatedDurationSeconds(meta.getEstimatedDurationSeconds());
        api.setTotalWeightGrams(meta.getTotalWeightGrams());
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
        api.setUsedGrams(f.getUsedGrams());
        return api;
    }

    /**
     * Orders created before file versioning have a file pointer on the JobOrder but no
     * version records. Capture that file as version 1 so it survives the next upload.
     */
    private void backfillInitialVersion(JobOrder order, JobOrderFileType fileType) {
        String existingFileId = fileType == JobOrderFileType.PART
                ? order.getMongoPartFileId()
                : order.getMongoGcodeFileId();
        if (existingFileId == null) return;
        if (fileVersionRepository.findFirstByOrderIdAndFileTypeOrderByVersionNumberDesc(order.getOrderId(), fileType).isPresent()) {
            return;
        }
        String existingFilename = fileType == JobOrderFileType.PART
                ? order.getPartFilename()
                : order.getGcodeFilename();
        String metadataId = fileType == JobOrderFileType.GCODE ? order.getMongoGcodeMetadataId() : null;
        createVersion(order, fileType, List.of(new JobOrderVersionFile(existingFilename, existingFileId)),
                metadataId, "Initial upload");
    }

    private JobOrderFileVersion createVersion(JobOrder order, JobOrderFileType fileType,
                                              List<JobOrderVersionFile> files,
                                              String mongoGcodeMetadataId, String description) {
        int nextNumber = fileVersionRepository
                .findFirstByOrderIdAndFileTypeOrderByVersionNumberDesc(order.getOrderId(), fileType)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);
        JobOrderFileVersion version = new JobOrderFileVersion();
        version.setOrderId(order.getOrderId());
        version.setFileType(fileType);
        version.setVersionNumber(nextNumber);
        version.setFilename(files.get(0).getFilename());
        version.setMongoFileId(files.get(0).getMongoFileId());
        version.setFiles(new ArrayList<>(files));
        version.setMongoGcodeMetadataId(mongoGcodeMetadataId);
        version.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        return fileVersionRepository.save(version);
    }

    /**
     * Sets the required print count per file of a gcode version. Legacy single-file rows are
     * materialized into the files collection first so the quantity has somewhere to live.
     */
    @Transactional
    public ApiJobOrderFileVersion updateGcodeVersionQuantities(long id, long versionId, List<ApiVersionFileQuantity> quantities) {
        JobOrder order = findOrThrow(id);
        JobOrderFileVersion version = getFileVersion(id, versionId, JobOrderFileType.GCODE);
        if (version.getFiles() == null || version.getFiles().isEmpty()) {
            version.setFiles(new ArrayList<>(version.getEffectiveFiles()));
        }
        List<JobOrderVersionFile> files = version.getFiles();
        for (ApiVersionFileQuantity entry : quantities) {
            Integer index = entry.getFileIndex();
            Integer quantity = entry.getQuantity();
            if (index == null || index < 0 || index >= files.size()) {
                throw new IllegalArgumentException("File index " + index + " not found in version " + versionId);
            }
            if (quantity == null || quantity < 0 || quantity > 999) {
                throw new IllegalArgumentException("Quantity must be between 0 and 999");
            }
            files.get(index).setPrintQuantity(quantity);
        }
        fileVersionRepository.save(version);
        return toApiFileVersion(version, order.getMongoGcodeFileId());
    }

    /** The gcode version whose files include the given Mongo file id, or null (legacy orders) */
    public JobOrderFileVersion findGcodeVersionContainingFile(long orderId, String mongoFileId) {
        if (mongoFileId == null) return null;
        return fileVersionRepository.findByOrderIdAndFileTypeOrderByVersionNumberDesc(orderId, JobOrderFileType.GCODE)
                .stream()
                .filter(v -> v.getEffectiveFiles().stream()
                        .anyMatch(f -> Objects.equals(f.getMongoFileId(), mongoFileId)))
                .findFirst()
                .orElse(null);
    }

    /** Resolve the Mongo file id for a single file within a version (legacy rows expose index 0). */
    public String getVersionFileId(long id, long versionId, JobOrderFileType fileType, int fileIndex) {
        JobOrderFileVersion version = getFileVersion(id, versionId, fileType);
        List<JobOrderVersionFile> files = version.getEffectiveFiles();
        if (fileIndex < 0 || fileIndex >= files.size()) {
            throw new IllegalArgumentException("File index " + fileIndex + " not found in version " + versionId);
        }
        return files.get(fileIndex).getMongoFileId();
    }

    private ApiJobOrderFileVersion toApiFileVersion(JobOrderFileVersion version, String activeFileId) {
        ApiJobOrderFileVersion api = new ApiJobOrderFileVersion();
        api.setVersionId(version.getVersionId());
        api.setVersionNumber(version.getVersionNumber());
        api.setFileType(ApiJobOrderFileType.valueOf(version.getFileType().name()));
        api.setFilename(version.getFilename());
        api.setDescription(version.getDescription());
        api.setCreatedAt(version.getCreatedAt() != null ? version.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        List<JobOrderVersionFile> files = version.getEffectiveFiles();
        List<ApiJobOrderVersionFile> apiFiles = new ArrayList<>(files.size());
        boolean anyActive = false;
        for (int i = 0; i < files.size(); i++) {
            ApiJobOrderVersionFile apiFile = new ApiJobOrderVersionFile();
            apiFile.setFileIndex(i);
            apiFile.setFilename(files.get(i).getFilename());
            apiFile.setPrintQuantity(files.get(i).effectiveQuantity());
            apiFile.setCompletedPrints(files.get(i).effectiveCompleted());
            boolean fileActive = Objects.equals(files.get(i).getMongoFileId(), activeFileId);
            apiFile.setActive(fileActive);
            anyActive |= fileActive;
            apiFiles.add(apiFile);
        }
        api.setFiles(apiFiles);
        api.setActive(anyActive || Objects.equals(version.getMongoFileId(), activeFileId));
        return api;
    }

    private JobOrder findOrThrow(long id) {
        return jobOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job order not found: " + id));
    }
}
