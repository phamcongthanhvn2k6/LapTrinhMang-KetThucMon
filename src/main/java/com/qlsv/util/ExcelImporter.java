package com.qlsv.util;

import com.qlsv.model.StudentData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trình nhập dữ liệu bảng điểm sinh viên hàng loạt từ Excel (.xlsx)
 * tích hợp kiểm tra định dạng nghiêm ngặt (Strict Validation & Deduplication).
 */
public class ExcelImporter {

    public static List<StudentData> importStudentsFromExcel(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new Exception("Tệp Excel không tồn tại!");
        }
        Map<String, StudentData> deduplicatedMap = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new Exception("File Excel không có Sheet nào!");
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new Exception("File Excel không có Sheet hợp lệ!");
            }

            int startRow = 1; // Row 0 is header
            for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell cellMaSV = row.getCell(1);
                Cell cellHoTen = row.getCell(2);
                Cell cellCC = row.getCell(3);
                Cell cellGK = row.getCell(4);
                Cell cellCK = row.getCell(5);

                String rawMaSV = getCellValueAsString(cellMaSV).trim();
                String rawHoTen = getCellValueAsString(cellHoTen).trim();

                // Bỏ qua dòng trống hoàn toàn
                if (rawMaSV.isEmpty() && rawHoTen.isEmpty()) continue;

                int displayRow = r + 1;
                try {
                    String maSV = InputValidator.normalizeAndValidateStudentId(rawMaSV);
                    String hoTen = InputValidator.normalizeAndValidateFullName(rawHoTen);
                    double diemCC = parseCellScoreStrict(cellCC, "Điểm Chuyên cần (Cột D)");
                    double diemGK = parseCellScoreStrict(cellGK, "Điểm Giữa kỳ (Cột E)");
                    double diemCK = parseCellScoreStrict(cellCK, "Điểm Thi kết thúc (Cột F)");

                    deduplicatedMap.put(maSV, new StudentData(maSV, hoTen, diemCC, diemGK, diemCK));
                } catch (IllegalArgumentException ex) {
                    throw new Exception("Lỗi dữ liệu tại Dòng " + displayRow + ": " + ex.getMessage());
                }
            }
        }

        if (deduplicatedMap.isEmpty()) {
            throw new Exception("File Excel không chứa bản ghi sinh viên hợp lệ nào!");
        }

        return new ArrayList<>(deduplicatedMap.values());
    }

    public static void createSampleTemplate(File destination) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("BangDiemTinChi_PTIT");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "STT", "Mã Sinh Viên", "Họ và Tên Sinh Viên",
                    "Chuyên Cần (10%)", "Giữa Kỳ (20%)", "Thi Kết Thúc (70%)"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            Object[][] samples = {
                    {1, "SV251", "Trần Minh Quân", 9.0, 8.5, 9.2},
                    {2, "SV252", "Lê Thu Hà", 9.5, 9.0, 8.8},
                    {3, "SV253", "Phạm Hoàng Nam", 8.0, 7.5, 7.8},
                    {4, "SV254", "Vũ Mai Phương", 4.0, 7.0, 6.5}
            };

            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = samples[r][c];
                    if (val instanceof Integer) {
                        cell.setCellValue((Integer) val);
                    } else if (val instanceof Double) {
                        cell.setCellValue((Double) val);
                    } else {
                        cell.setCellValue(String.valueOf(val));
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destination)) {
                workbook.write(fos);
            }
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private static double parseCellScoreStrict(Cell cell, String colLabel) throws IllegalArgumentException {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException("Ô " + colLabel + " bị bỏ trống!");
        }
        switch (cell.getCellType()) {
            case NUMERIC:
            case FORMULA:
                return InputValidator.validateScoreValue(cell.getNumericCellValue(), colLabel);
            case STRING:
                return InputValidator.parseAndValidateScore(cell.getStringCellValue(), colLabel);
            default:
                throw new IllegalArgumentException("Ô " + colLabel + " có kiểu dữ liệu không hợp lệ!");
        }
    }
}
