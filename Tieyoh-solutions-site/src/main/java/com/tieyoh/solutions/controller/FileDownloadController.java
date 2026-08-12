package com.tieyoh.solutions.controller;

import com.tieyoh.solutions.domain.DocumentAsset;
import com.tieyoh.solutions.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
public class FileDownloadController {

    private final DocumentService documentService;

    @GetMapping("/member/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentAsset doc = documentService.getById(id);
        if (doc.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        Path path = Paths.get(documentService.uploadsDir()).resolve(doc.getFilePath());
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFilePath() + "\"")
                .body(resource);
    }
}
