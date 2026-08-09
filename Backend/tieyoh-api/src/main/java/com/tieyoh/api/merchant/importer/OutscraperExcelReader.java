package com.tieyoh.api.merchant.importer;

import com.tieyoh.api.merchant.dto.MerchantImportRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OutscraperExcelReader {

    public List<MerchantImportRecord> read(InputStream inputStream)
            throws Exception {

        List<MerchantImportRecord> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getPhysicalNumberOfRows() == 0) {
                return records;
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columns = buildColumnMap(headerRow);

            for (int rowNumber = 1;
                 rowNumber <= sheet.getLastRowNum();
                 rowNumber++) {

                Row row = sheet.getRow(rowNumber);

                if (row == null) {
                    continue;
                }

                String name = getString(row, columns, "name");

                if (name == null || name.isBlank()) {
                    continue;
                }

                MerchantImportRecord record =
                        new MerchantImportRecord();

                record.setSource("OUTSCRAPER");

                record.setSourceMerchantId(
                        getString(row, columns, "place_id")
                );

                record.setBrandName(name);
                record.setLocationName(name);

                record.setCategory(
                        getString(row, columns, "type")
                );

                record.setPhone(
                        getString(row, columns, "phone")
                );

                record.setWebsite(
                        getString(row, columns, "website")
                );

                record.setLogoUrl(
                        getString(row, columns, "logo")
                );

                record.setAddressLine1(
                        getString(row, columns, "street")
                );

                record.setCity(
                        getString(row, columns, "city")
                );

                record.setState(
                        getString(row, columns, "state_code")
                );

                record.setPostalCode(
                        getString(row, columns, "postal_code")
                );

                record.setCountry(
                        getString(row, columns, "country_code")
                );

                record.setLatitude(
                        getDecimal(row, columns, "latitude")
                );

                record.setLongitude(
                        getDecimal(row, columns, "longitude")
                );

                records.add(record);
            }
        }

        return records;
    }

    private Map<String, Integer> buildColumnMap(Row headerRow) {

        Map<String, Integer> columns = new HashMap<>();

        for (Cell cell : headerRow) {

            String columnName =
                    cell.getStringCellValue()
                            .trim()
                            .toLowerCase();

            columns.put(columnName, cell.getColumnIndex());
        }

        return columns;
    }

    private String getString(
            Row row,
            Map<String, Integer> columns,
            String columnName) {

        Integer columnIndex = columns.get(columnName);

        if (columnIndex == null) {
            return null;
        }

        Cell cell = row.getCell(columnIndex);

        if (cell == null) {
            return null;
        }

        DataFormatter formatter = new DataFormatter();

        String value = formatter.formatCellValue(cell);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private BigDecimal getDecimal(
            Row row,
            Map<String, Integer> columns,
            String columnName) {

        String value = getString(row, columns, columnName);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}