package com.ipjuon.backend.bank;

import com.ipjuon.backend.consultation.ConsultationRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class BankExcelExportService {

    public byte[] generateExcel(String complexName, String approvalNo,
                                 long totalLimit, List<ConsultationRequest> list) throws Exception {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 스타일 생성
            CellStyle centerStyle  = createBaseStyle(wb, HorizontalAlignment.CENTER);
            CellStyle leftStyle    = createBaseStyle(wb, HorizontalAlignment.LEFT);
            CellStyle headerStyle  = createHeaderStyle(wb);
            CellStyle greenStyle   = createColorStyle(wb, (byte)0x92, (byte)0xD0, (byte)0x50);
            CellStyle peachStyle   = createColorStyle(wb, (byte)0xF3, (byte)0xC6, (byte)0xBF);
            CellStyle dateStyle    = createDateStyle(wb);
            CellStyle numberStyle  = createNumberStyle(wb);

            // ── 접수일 시트 ──
            XSSFSheet sheet = wb.createSheet("접수일");
            setColumnWidths(sheet);

            // 제목행 (1~2행)
            createTitleRows(sheet, complexName, greenStyle, peachStyle);

            // 헤더행 (4행, index=3)
            createHeaderRow(sheet, headerStyle);

            // 데이터행 (5행~, index=4~)
            int rowIdx = 4;
            int seq = 1;
            for (ConsultationRequest r : list) {
                createDataRow(sheet, rowIdx++, seq++, r,
                        centerStyle, leftStyle, dateStyle, numberStyle);
            }

            // ── 총합 시트 ──
            createSummarySheet(wb, centerStyle, approvalNo, totalLimit);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── 스타일 헬퍼 ──

    private CellStyle createBaseStyle(XSSFWorkbook wb, HorizontalAlignment align) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setBold(true);
        s.setFont(font);
        s.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte)0xD9, (byte)0xE1, (byte)0xF2}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle createColorStyle(XSSFWorkbook wb, byte r, byte g, byte b) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setBold(true);
        s.setFont(font);
        s.setFillForegroundColor(new XSSFColor(new byte[]{r, g, b}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private CellStyle createDateStyle(XSSFWorkbook wb) {
        CellStyle s = createBaseStyle(wb, HorizontalAlignment.CENTER);
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("yyyy-mm-dd"));
        return s;
    }

    private CellStyle createNumberStyle(XSSFWorkbook wb) {
        CellStyle s = createBaseStyle(wb, HorizontalAlignment.CENTER);
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("#,##0"));
        return s;
    }

    // ── 컬럼 너비 ──
    private void setColumnWidths(XSSFSheet sheet) {
        int[] widths = {5, 30, 6, 8, 6, 6, 10, 18, 8, 8, 16,
                10, 15, 10, 10, 7, 8, 7, 7, 7,
                10, 18, 16, 17, 18, 18, 18, 22};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
    }

    // ── 제목행 ──
    private void createTitleRows(XSSFSheet sheet, String complexName,
                                  CellStyle greenStyle, CellStyle peachStyle) {
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 7));
        Row row1 = sheet.createRow(0);
        row1.setHeightInPoints(17.25f);
        Cell titleCell = row1.createCell(0);
        titleCell.setCellValue(complexName + " 접수리스트");
        titleCell.setCellStyle(greenStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 9, 10));
        Cell mojiCell = row1.createCell(9);
        mojiCell.setCellValue("모집공고일\n2022.07.29");
        mojiCell.setCellStyle(greenStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 11, 12));
        Cell mgmtCell = row1.createCell(11);
        mgmtCell.setCellValue("관리처분인가\n2021.01.29");
        mgmtCell.setCellStyle(peachStyle);

        Row row2 = sheet.createRow(1);
        row2.setHeightInPoints(36f);
    }

    // ── 헤더행 ──
    private void createHeaderRow(XSSFSheet sheet, CellStyle headerStyle) {
        Row row = sheet.createRow(3);
        row.setHeightInPoints(36f);
        String[] headers = {
                "순번", "불비 및 특이사항", "담당", "전매일", "구분", "명의",
                "고객명", "주민등록번호", "동", "호수", "연락처",
                "실행일", "대출신청금", "접수일\n(취소일)", "서류\n전달일",
                "타입", "상품", "상환\n방식", "기간", "거치",
                "공동\n명의자", "주민등록번호", "연락처",
                "고객준비금", "후불이자계좌", "잔금계좌", "옵션 계좌", "중도금가상계좌\n(타행)"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    // ── 데이터행 ──
    private void createDataRow(XSSFSheet sheet, int rowIdx, int seq,
                                ConsultationRequest r,
                                CellStyle center, CellStyle left,
                                CellStyle date, CellStyle number) {
        Row row = sheet.createRow(rowIdx);
        row.setHeightInPoints(36f);

        setCell(row, 0,  String.valueOf(seq),          center);
        setCell(row, 1,  r.getSpecial_notes(),         left);
        setCell(row, 2,  r.getManager(),               center);
        setCell(row, 3,  r.getTransfer_date(),         center);
        setCell(row, 4,  r.getDivision(),              center);
        setCell(row, 5,  r.getOwnership(),             center);
        setCell(row, 6,  r.getResident_name(),         center);
        setCell(row, 7,  r.getResident_no(),           center);
        setCell(row, 8,  r.getDong(),                  center);
        setCell(row, 9,  r.getHo(),                    center);
        setCell(row, 10, r.getResident_phone(),        center);

        // 실행일: 취소면 "취소", 아니면 날짜
        Cell execCell = row.createCell(11);
        if ("cancel".equals(r.getLoan_status())) {
            execCell.setCellValue("취소");
            execCell.setCellStyle(center);
        } else if (r.getExecution_date() != null) {
            execCell.setCellValue(r.getExecution_date().toString());
            execCell.setCellStyle(date);
        } else {
            execCell.setCellStyle(center);
        }

        // 대출신청금
        Cell amtCell = row.createCell(12);
        if (r.getLoan_amount() != null) amtCell.setCellValue(r.getLoan_amount());
        amtCell.setCellStyle(number);

        setDateCell(row, 13, r.getReceive_date(),  date, center);
        setDateCell(row, 14, r.getDocument_date(), date, center);

        setCell(row, 15, r.getApt_type(),              center);
        setCell(row, 16, r.getProduct(),               center);
        setCell(row, 17, r.getRepayment_method(),      center);
        setCell(row, 18, r.getLoan_period(),            center);
        setCell(row, 19, r.getDeferment(),             center);
        setCell(row, 20, r.getJoint_owner_name(),      center);
        setCell(row, 21, r.getJoint_owner_rrn(),       center);
        setCell(row, 22, r.getJoint_owner_tel(),       center);

        Cell depositCell = row.createCell(23);
        depositCell.setCellValue(r.getCustomer_deposit() != null ? r.getCustomer_deposit() : 0);
        depositCell.setCellStyle(number);

        setCell(row, 24, r.getDeferred_interest_account(), center);
        setCell(row, 25, r.getBalance_account(),            center);
        setCell(row, 26, r.getOption_account(),             center);
        setCell(row, 27, r.getInterim_virtual_account(),    center);
    }

    private void setCell(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val : "");
        cell.setCellStyle(style);
    }

    private void setDateCell(Row row, int col, LocalDate d, CellStyle dateStyle, CellStyle empty) {
        Cell cell = row.createCell(col);
        if (d != null) {
            cell.setCellValue(d.toString());
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellStyle(empty);
        }
    }

    // ── 총합 시트 ──
    private void createSummarySheet(XSSFWorkbook wb, CellStyle center,
                                     String approvalNo, long totalLimit) {
        Sheet ws = wb.createSheet("총합");

        Row r4 = ws.createRow(3);
        setCell(r4, 1, "승인번호 : " + approvalNo, center);
        setCell(r4, 6, "총한도 : " + (totalLimit / 100_000_000) + "억원", center);

        Row r5 = ws.createRow(4);
        setCell(r5, 1, "▣ 총접수", center);
        setCell(r5, 6, "▣ 당일접수", center);

        Row r6 = ws.createRow(5);
        setCell(r6, 1, "분   류",    center);
        setCell(r6, 2, "한도",       center);
        setCell(r6, 3, "건수",       center);
        setCell(r6, 4, "건별금액",   center);
        setCell(r6, 6, "분   류",    center);
        setCell(r6, 7, "건수",       center);
        setCell(r6, 8, "금액",       center);

        Row r7 = ws.createRow(6);
        setCell(r7, 1, "고           정", center);
        r7.createCell(3).setCellFormula("COUNTIF(접수일!$Q$5:$Q$860,\"고정\")");
        r7.createCell(4).setCellFormula("SUMIF(접수일!$Q$5:$Q$860,\"고정\",접수일!$M$5:$M$860)");
        setCell(r7, 6, "고         정", center);

        Row r8 = ws.createRow(7);
        setCell(r8, 1, "변           동", center);
        r8.createCell(3).setCellFormula("COUNTIF(접수일!$Q$5:$Q$860,\"변동\")");
        r8.createCell(4).setCellFormula("SUMIF(접수일!$Q$5:$Q$860,\"변동\",접수일!$M$5:$M$860)");

        Row r9 = ws.createRow(8);
        setCell(r9, 1, "취           소", center);
        r9.createCell(3).setCellFormula("COUNTIF(접수일!$L$5:$L$860,\"취소\")");
        r9.createCell(4).setCellFormula("SUMIF(접수일!$L$5:$L$860,\"취소\",접수일!$M$5:$M$860)");

        Row r10 = ws.createRow(9);
        setCell(r10, 1, "총  합계",   center);
        r10.createCell(3).setCellFormula("SUM(D7:D8)-D9");
        r10.createCell(4).setCellFormula("SUM(E7:E8)-E9");
        setCell(r10, 6, "당일 합계", center);
        r10.createCell(7).setCellFormula("SUM(H7:H8)");
        r10.createCell(8).setCellFormula("SUM(I7:I8)");
    }
}
