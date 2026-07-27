package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiJobOrderFileVersion;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiUpdateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiVersionFileQuantity;
import com.czagrzebski.printhelm.web.domain.JobOrderFileType;
import com.czagrzebski.printhelm.web.service.JobOrderFileService;
import com.czagrzebski.printhelm.web.service.JobOrderService;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/job-order")
public class JobOrderController {

    private final JobOrderService jobOrderService;
    private final JobOrderFileService jobOrderFileService;

    public JobOrderController(JobOrderService jobOrderService, JobOrderFileService jobOrderFileService) {
        this.jobOrderService = jobOrderService;
        this.jobOrderFileService = jobOrderFileService;
    }

    @GetMapping
    public ResponseEntity<List<ApiJobOrderResponse>> getJobOrders() {
        return ResponseEntity.ok(jobOrderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiJobOrderResponse> getJobOrderById(@PathVariable long id) {
        return ResponseEntity.ok(jobOrderService.getOrderById(id));
    }

    @PostMapping
    public ResponseEntity<ApiJobOrderResponse> createJobOrder(@RequestBody ApiCreateJobOrderRequest request) {
        return ResponseEntity.status(201).body(jobOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiJobOrderResponse> updateJobOrder(@PathVariable long id,
                                                              @RequestBody ApiUpdateJobOrderRequest request) {
        return ResponseEntity.ok(jobOrderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobOrder(@PathVariable long id) {
        jobOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/part-file")
    public ResponseEntity<ApiJobOrderResponse> uploadPartFile(@PathVariable long id,
                                                              @RequestParam("files") List<MultipartFile> files,
                                                              @RequestParam(value = "description", required = false) String description) throws IOException {
        return ResponseEntity.ok(jobOrderService.uploadPartFiles(id, files, description));
    }

    @PostMapping("/{id}/gcode-file")
    public ResponseEntity<ApiJobOrderResponse> uploadGcodeFile(@PathVariable long id,
                                                               @RequestParam("files") List<MultipartFile> files,
                                                               @RequestParam(value = "description", required = false) String description) throws IOException {
        return ResponseEntity.ok(jobOrderService.uploadGcodeFiles(id, files, description));
    }

    @GetMapping("/{id}/part-file/versions")
    public ResponseEntity<List<ApiJobOrderFileVersion>> getPartFileVersions(@PathVariable long id) {
        return ResponseEntity.ok(jobOrderService.getFileVersions(id, JobOrderFileType.PART));
    }

    @GetMapping("/{id}/part-file/versions/{versionId}")
    public ResponseEntity<Resource> downloadPartFileVersion(@PathVariable long id,
                                                            @PathVariable long versionId) throws IOException {
        return buildFileResponse(jobOrderService.getFileVersion(id, versionId, JobOrderFileType.PART).getMongoFileId());
    }

    @GetMapping("/{id}/part-file/versions/{versionId}/files/{fileIndex}")
    public ResponseEntity<Resource> downloadPartFileVersionFile(@PathVariable long id,
                                                                @PathVariable long versionId,
                                                                @PathVariable int fileIndex) throws IOException {
        return buildFileResponse(jobOrderService.getVersionFileId(id, versionId, JobOrderFileType.PART, fileIndex));
    }

    @GetMapping("/{id}/gcode-file/versions")
    public ResponseEntity<List<ApiJobOrderFileVersion>> getGcodeFileVersions(@PathVariable long id) {
        return ResponseEntity.ok(jobOrderService.getFileVersions(id, JobOrderFileType.GCODE));
    }

    @GetMapping("/{id}/gcode-file/versions/{versionId}")
    public ResponseEntity<Resource> downloadGcodeFileVersion(@PathVariable long id,
                                                             @PathVariable long versionId) throws IOException {
        return buildFileResponse(jobOrderService.getFileVersion(id, versionId, JobOrderFileType.GCODE).getMongoFileId());
    }

    @GetMapping("/{id}/gcode-file/versions/{versionId}/files/{fileIndex}")
    public ResponseEntity<Resource> downloadGcodeFileVersionFile(@PathVariable long id,
                                                                 @PathVariable long versionId,
                                                                 @PathVariable int fileIndex) throws IOException {
        return buildFileResponse(jobOrderService.getVersionFileId(id, versionId, JobOrderFileType.GCODE, fileIndex));
    }

    @PutMapping("/{id}/gcode-file/versions/{versionId}/quantities")
    public ResponseEntity<ApiJobOrderFileVersion> updateGcodeFileVersionQuantities(
            @PathVariable long id,
            @PathVariable long versionId,
            @RequestBody List<ApiVersionFileQuantity> quantities) {
        return ResponseEntity.ok(jobOrderService.updateGcodeVersionQuantities(id, versionId, quantities));
    }

    @PostMapping("/{id}/gcode-file/versions/{versionId}/select")
    public ResponseEntity<ApiJobOrderResponse> selectGcodeFileVersion(@PathVariable long id,
                                                                      @PathVariable long versionId,
                                                                      @RequestParam(value = "fileIndex", required = false, defaultValue = "0") int fileIndex) {
        return ResponseEntity.ok(jobOrderService.selectGcodeVersion(id, versionId, fileIndex));
    }

    @GetMapping("/{id}/part-file")
    public ResponseEntity<Resource> downloadPartFile(@PathVariable long id) throws IOException {
        ApiJobOrderResponse order = jobOrderService.getOrderById(id);
        if (order.getMongoPartFileId() == null) return ResponseEntity.notFound().build();
        return buildFileResponse(order.getMongoPartFileId());
    }

    @GetMapping("/{id}/gcode-file")
    public ResponseEntity<Resource> downloadGcodeFile(@PathVariable long id) throws IOException {
        ApiJobOrderResponse order = jobOrderService.getOrderById(id);
        if (order.getMongoGcodeFileId() == null) return ResponseEntity.notFound().build();
        return buildFileResponse(order.getMongoGcodeFileId());
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable long id) {
        byte[] pdf = jobOrderService.generateInvoice(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/quote")
    public ResponseEntity<byte[]> downloadQuote(@PathVariable long id) {
        byte[] pdf = jobOrderService.generateQuote(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"quote-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private ResponseEntity<Resource> buildFileResponse(String fileId) throws IOException {
        GridFsResource resource = jobOrderFileService.getFileResource(fileId);
        String filename = resource.getFilename() != null ? resource.getFilename() : fileId;
        String contentType = resource.getContentType() != null
                ? resource.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
