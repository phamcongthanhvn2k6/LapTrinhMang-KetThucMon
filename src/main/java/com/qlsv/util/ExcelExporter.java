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
        String[] headers = {
                "STT", "Mã SV", "Họ và Tên", "Lớp Học Phần",
                "Chuyên Cần (10%)", "Giữa Kỳ (20%)", "Cuối Kỳ (70%)",
                "Điểm HP (10)", "GPA Hệ 4", "Điểm Chữ", "Xếp Loại", "Trạng Thái"
        };
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
            row.createCell(3).setCellValue(s.getClassName() != null ? s.getClassName() : "D21CQCN01-N");
            row.createCell(4).setCellValue(s.getScoreCC());
            row.createCell(5).setCellValue(s.getScoreGK());
            row.createCell(6).setCellValue(s.getScoreCK());
            row.createCell(7).setCellValue(s.getAverageScore());
            row.createCell(8).setCellValue(s.getGpa4());
            row.createCell(9).setCellValue(s.getLetterGrade());
            row.createCell(10).setCellValue(s.getAcademicRankCredit());
            row.createCell(11).setCellValue(s.getAcademicStatus());
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

    public static File exportTranscriptToExcel(com.qlsv.model.StudentTranscript t, File fileToSave) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Phieu Diem Tin Chi");

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 13);
        titleFont.setColor(IndexedColors.DARK_RED.getIndex());
        titleStyle.setFont(titleFont);

        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)");
        c0.setCellStyle(titleStyle);

        Row r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("PHIẾU KẾT QUẢ HỌC TẬP TÍN CHỈ SINH VIÊN");

        Row r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("Mã SV: " + t.getStudentId());
        r3.createCell(2).setCellValue("Họ và Tên: " + t.getFullName());
        r3.createCell(5).setCellValue("Lớp: " + t.getClassName());

        Row r4 = sheet.createRow(4);
        r4.createCell(0).setCellValue("Số TC tích lũy: " + t.getTotalCredits());
        r4.createCell(2).setCellValue("ĐTB Học phần (10): " + t.getCumulativeGpa10());
        r4.createCell(5).setCellValue("GPA Hệ 4: " + t.getCumulativeGpa4() + " (" + t.getAcademicRank() + ")");

        CellStyle headerStyle = workbook.createCellStyle();
        Font hf = workbook.createFont();
        hf.setBold(true);
        hf.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(hf);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headRow = sheet.createRow(6);
        String[] cols = {"STT", "Mã Học Phần", "Tên Học Phần", "Số TC", "Điểm Quá Trình", "Điểm Thi Hết Môn", "Điểm Học Phần (10)", "GPA Hệ 4", "Điểm Chữ", "Kết Quả"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = headRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 7;
        int stt = 1;
        for (com.qlsv.model.CourseGrade g : t.getGrades()) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(stt++);
            r.createCell(1).setCellValue(g.getCourseCode());
            r.createCell(2).setCellValue(g.getCourseName());
            r.createCell(3).setCellValue(g.getCredits());
            r.createCell(4).setCellValue(g.getScoreProcess());
            r.createCell(5).setCellValue(g.getScoreExam());
            r.createCell(6).setCellValue(g.getTotalScore());
            r.createCell(7).setCellValue(g.getGpa4());
            r.createCell(8).setCellValue(g.getLetterGrade());
            r.createCell(9).setCellValue(g.isPassed() ? "ĐẠT" : "HỌC LẠI");
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream out = new FileOutputStream(fileToSave)) {
            workbook.write(out);
        }
        workbook.close();
        return fileToSave;
    }
}
