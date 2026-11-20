package com.qlsv.util;

import com.qlsv.model.StudentData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelImporter {

    public static List<StudentData> importStudentsFromExcel(File file) throws Exception {
        List<StudentData> list = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

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
                Cell cellToan = row.getCell(3);
                Cell cellVan = row.getCell(4);
                Cell cellAnh = row.getCell(5);

                if (cellMaSV == null || cellHoTen == null) continue;

                String maSV = getCellValueAsString(cellMaSV).trim();
                String hoTen = getCellValueAsString(cellHoTen).trim();

                if (maSV.isEmpty() || hoTen.isEmpty()) continue;

                double toan = getCellValueAsDouble(cellToan, 0.0);
                double van = getCellValueAsDouble(cellVan, 0.0);
                double anh = getCellValueAsDouble(cellAnh, 0.0);

                if (toan < 0 || toan > 10 || van < 0 || van > 10 || anh < 0 || anh > 10) {
                    throw new Exception("Dòng " + (r + 1) + ": Điểm số phải trong khoảng từ 0.0 đến 10.0 (Mã SV: " + maSV + ")");
                }

                list.add(new StudentData(maSV, hoTen, toan, van, anh));
            }
        }

        if (list.isEmpty()) {
            throw new Exception("File Excel không chứa bản ghi sinh viên hợp lệ nào!");
        }

        return list;
    }

    public static void createSampleTemplate(File destination) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("DanhSachSinhVien");

            // Header style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "Mã Sinh Viên", "Họ Tên Sinh Viên", "Điểm Toán", "Điểm Văn", "Điểm Anh"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample rows
            Object[][] samples = {
                    {1, "B21DCCN101", "Trần Minh Quân", 8.5, 7.8, 9.0},
                    {2, "B21DCCN102", "Lê Thu Hà", 9.0, 8.5, 8.8},
                    {3, "B21DCCN103", "Phạm Hoàng Nam", 7.0, 6.5, 7.5},
                    {4, "B21DCCN104", "Vũ Mai Phương", 8.0, 8.0, 8.5}
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
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private static double getCellValueAsDouble(Cell cell, double def) {
        if (cell == null) return def;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    return Double.parseDouble(cell.getStringCellValue().trim());
                default:
                    return def;
            }
        } catch (Exception e) {
            return def;
        }
    }
}
