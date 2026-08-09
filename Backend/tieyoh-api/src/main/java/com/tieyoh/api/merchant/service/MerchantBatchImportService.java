package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.dto.MerchantBatchImportResult;
import com.tieyoh.api.merchant.dto.MerchantImportRecord;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantBatchImportService {

    private final MerchantImportService merchantImportService;

    public MerchantBatchImportService(
            MerchantImportService merchantImportService) {

        this.merchantImportService = merchantImportService;
    }

    public MerchantBatchImportResult importRecords(
            List<MerchantImportRecord> records) {

        MerchantBatchImportResult result =
                new MerchantBatchImportResult();

        result.setRowsRead(records.size());

        for (MerchantImportRecord record : records) {

            result.setRowsProcessed(
                    result.getRowsProcessed() + 1
            );

            try {

                merchantImportService.importMerchant(record);

                result.setSuccessful(
                        result.getSuccessful() + 1
                );

            } catch (Exception ex) {

                result.setErrors(
                        result.getErrors() + 1
                );

                ex.printStackTrace();
            }
        }

        return result;
    }
}