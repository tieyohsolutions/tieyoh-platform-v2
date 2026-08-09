package com.tieyoh.api.merchant.dto;

public class MerchantBatchImportResult {

    private int rowsRead;
    private int rowsProcessed;
    private int successful;
    private int errors;

    public MerchantBatchImportResult() {
    }

    public int getRowsRead() {
        return rowsRead;
    }

    public void setRowsRead(int rowsRead) {
        this.rowsRead = rowsRead;
    }

    public int getRowsProcessed() {
        return rowsProcessed;
    }

    public void setRowsProcessed(int rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }
}