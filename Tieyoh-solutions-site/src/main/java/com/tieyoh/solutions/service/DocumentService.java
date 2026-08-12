package com.tieyoh.solutions.service;

import com.tieyoh.solutions.domain.DocumentAsset;
import com.tieyoh.solutions.repository.DocumentAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentAssetRepository documentAssetRepository;

    @Value("${app.uploads-dir}")
    private String uploadsDir;

    public List<DocumentAsset> listAll() {
        return documentAssetRepository.findAllByOrderByCreatedAtDesc();
    }

    public DocumentAsset getById(Long id) {
        return documentAssetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    @Transactional
    public DocumentAsset save(String title, String description, String category, String externalUrl,
                               MultipartFile file, Long uploadedBy) throws IOException {
        DocumentAsset.DocumentAssetBuilder builder = DocumentAsset.builder()
                .title(title)
                .description(description)
                .category(category)
                .externalUrl(externalUrl)
                .uploadedBy(uploadedBy);

        if (file != null && !file.isEmpty()) {
            Path dir = Paths.get(uploadsDir);
            Files.createDirectories(dir);
            String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path target = dir.resolve(safeName);
            file.transferTo(target);
            builder.filePath(safeName);
        }

        return documentAssetRepository.save(builder.build());
    }

    @Transactional
    public void delete(Long id) {
        documentAssetRepository.deleteById(id);
    }

    public String uploadsDir() {
        return uploadsDir;
    }
}
