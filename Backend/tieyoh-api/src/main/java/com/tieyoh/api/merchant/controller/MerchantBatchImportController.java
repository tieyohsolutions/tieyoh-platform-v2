package com.tieyoh.api.merchant.controller;

import com.tieyoh.api.merchant.dto.MerchantBatchImportResult;
import com.tieyoh.api.merchant.dto.MerchantImportRecord;
import com.tieyoh.api.merchant.importer.OutscraperExcelReader;
import com.tieyoh.api.merchant.service.MerchantBatchImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/merchant-batch")
public class MerchantBatchImportController {

    private final OutscraperExcelReader outscraperExcelReader;
    private final MerchantBatchImportService merchantBatchImportService;

    public MerchantBatchImportController(
            OutscraperExcelReader outscraperExcelReader,
            MerchantBatchImportService merchantBatchImportService) {

        this.outscraperExcelReader = outscraperExcelReader;
        this.merchantBatchImportService = merchantBatchImportService;
    }

    @PostMapping("/upload")
    public ResponseEntity<MerchantBatchImportResult> uploadMerchantFile(
            @RequestParam("file") MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            List<MerchantImportRecord> records =
                    outscraperExcelReader.read(inputStream);

            MerchantBatchImportResult result =
                    merchantBatchImportService.importRecords(records);

            return ResponseEntity.ok(result);

        } catch (Exception ex) {

            ex.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}