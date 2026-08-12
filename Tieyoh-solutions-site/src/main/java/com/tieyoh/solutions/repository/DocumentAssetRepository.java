package com.tieyoh.solutions.repository;

import com.tieyoh.solutions.domain.DocumentAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentAssetRepository extends JpaRepository<DocumentAsset, Long> {
    List<DocumentAsset> findAllByOrderByCreatedAtDesc();
}
