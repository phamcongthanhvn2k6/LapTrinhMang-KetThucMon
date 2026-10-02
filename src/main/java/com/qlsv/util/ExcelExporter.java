package com.qlsv.util;

import com.qlsv.model.StudentResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelExporter {

    public static File exportStudentsToExcel(List<StudentResult> students, File fileToSave) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Danh Sách Sinh Viên");

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // Header Row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"STT", "Mã SV", "Họ VÀ Tên", "Điểm Toán", "Điểm Văn", "Điểm Anh", "Điểm TB", "Xếp Loại"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowNum = 1;
        for (StudentResult s : students) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(rowNum);
            row.createCell(1).setCellValue(s.getStudentId());
            row.createCell(2).setCellValue(s.getFullName());
            row.createCell(3).setCellValue(s.getScoreMath());
            row.createCell(4).setCellValue(s.getScoreLiterature());
            row.createCell(5).setCellValue(s.getScoreEnglish());
            row.createCell(6).setCellValue(s.getAverageScore());
            row.createCell(7).setCellValue(s.getAcademicRank());
            rowNum++;
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream out = new FileOutputStream(fileToSave)) {
            workbook.write(out);
        }
        workbook.close();
        return fileToSave;
    }
}
