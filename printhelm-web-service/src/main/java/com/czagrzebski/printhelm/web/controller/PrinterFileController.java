package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.web.service.PrinterFileService;
import com.czagrzebski.printhelm.web.service.PrinterFileService.PrinterFileDTO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/printer/{id}/files")
public class PrinterFileController {

    private final PrinterFileService fileService;

    public PrinterFileController(PrinterFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ResponseEntity<List<PrinterFileDTO>> listFiles(@PathVariable long id) throws IOException {
        return ResponseEntity.ok(fileService.listFiles(id));
    }

    @PostMapping
    public ResponseEntity<Void> uploadFile(
            @PathVariable long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        fileService.uploadFile(id, file.getOriginalFilename(), file.getInputStream());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable long id,
            @PathVariable String filename) throws IOException {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileService.downloadFile(id, filename));
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable long id,
            @PathVariable String filename) throws IOException {
        fileService.deleteFile(id, filename);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIoError(IOException ex) {
        StringBuilder msg = new StringBuilder(ex.getMessage());
        for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
            msg.append(" → [").append(t.getClass().getSimpleName()).append("] ").append(t.getMessage());
        }
        return ResponseEntity.status(502).body(msg.toString());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
