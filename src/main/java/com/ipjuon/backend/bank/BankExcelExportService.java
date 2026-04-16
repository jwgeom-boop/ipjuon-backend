package com.ipjuon.backend.bank;

import com.ipjuon.backend.consultation.ConsultationRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
public class BankExcelExportService {

    private static final BorderStyle THIN   = BorderStyle.THIN;
    private static final BorderStyle MEDIUM = BorderStyle.MEDIUM;
    private static final BorderStyle DASHED = BorderStyle.DASHED;
    private static final BorderStyle NONE   = BorderStyle.NONE;

    public byte[] generateExcel(String complexName, String approvalNo,
                                 long totalLimit, List<ConsultationRequest> list) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── 스타일 ──
            XSSFCellStyle centerStyle  = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null);
            XSSFCellStyle leftStyle    = baseStyle(wb, HorizontalAlignment.LEFT,   THIN, THIN, THIN, THIN, null);
            XSSFCellStyle headerStyle  = headerStyle(wb);
            XSSFCellStyle greenStyle   = colorStyle(wb, "92D050", true);
            XSSFCellStyle peachStyle   = colorStyle(wb, "F3C6BF", true);
            XSSFCellStyle dateStyle    = dateStyle(wb);
            XSSFCellStyle numberStyle  = numberStyle(wb);

            // ── 접수일 시트 ──
            XSSFSheet sheet = wb.createSheet("접수일");
            setColumnWidths(sheet);
            createTitleRows(sheet, complexName, greenStyle, peachStyle);
            createHeaderRow(sheet, headerStyle);

            int rowIdx = 4;
            int seq = 1;
            for (ConsultationRequest r : list) {
                createDataRow(sheet, rowIdx++, seq++, r, centerStyle, leftStyle, dateStyle, numberStyle);
            }

            // ── 총합 시트 ──
            createSummarySheet(wb, approvalNo, totalLimit);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ──────────────────────────────────────────
    //  스타일 팩토리
    // ──────────────────────────────────────────

    private XSSFCellStyle baseStyle(XSSFWorkbook wb, HorizontalAlignment align,
                                     BorderStyle top, BorderStyle bottom,
                                     BorderStyle left, BorderStyle right,
                                     String hexRGB) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        s.setBorderTop(top);
        s.setBorderBottom(bottom);
        s.setBorderLeft(left);
        s.setBorderRight(right);
        if (hexRGB != null) setFill(s, hexRGB);
        return s;
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "D9E1F2");
        Font f = wb.createFont(); f.setBold(true);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle colorStyle(XSSFWorkbook wb, String hexRGB, boolean bold) {
        XSSFCellStyle s = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, hexRGB);
        if (bold) { Font f = wb.createFont(); f.setBold(true); s.setFont(f); }
        return s;
    }

    private XSSFCellStyle dateStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null);
        s.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
        return s;
    }

    private XSSFCellStyle numberStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        return s;
    }

    private void setFill(XSSFCellStyle s, String hexRGB) {
        byte[] rgb = new byte[]{
            (byte) Integer.parseInt(hexRGB.substring(0, 2), 16),
            (byte) Integer.parseInt(hexRGB.substring(2, 4), 16),
            (byte) Integer.parseInt(hexRGB.substring(4, 6), 16)
        };
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private XSSFCellStyle make(XSSFWorkbook wb, HorizontalAlignment align,
                                BorderStyle t, BorderStyle b, BorderStyle l, BorderStyle r,
                                String hex, boolean bold, String numFmt) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        s.setBorderTop(t); s.setBorderBottom(b);
        s.setBorderLeft(l); s.setBorderRight(r);
        if (hex != null) setFill(s, hex);
        if (bold) { Font f = wb.createFont(); f.setBold(true); s.setFont(f); }
        if (numFmt != null) s.setDataFormat(wb.createDataFormat().getFormat(numFmt));
        return s;
    }

    // ──────────────────────────────────────────
    //  접수일 시트
    // ──────────────────────────────────────────

    private void setColumnWidths(XSSFSheet sheet) {
        int[] widths = {5, 30, 6, 8, 6, 6, 10, 18, 8, 8, 16,
                10, 15, 10, 10, 7, 8, 7, 7, 7,
                10, 18, 16, 17, 18, 18, 18, 22};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private void createTitleRows(XSSFSheet sheet, String complexName,
                                  XSSFCellStyle greenStyle, XSSFCellStyle peachStyle) {
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 7));
        Row r1 = sheet.createRow(0);
        r1.setHeightInPoints(17.25f);
        Cell c = r1.createCell(0);
        c.setCellValue(complexName + " 접수리스트");
        c.setCellStyle(greenStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 9, 10));
        Cell m = r1.createCell(9);
        m.setCellValue("모집공고일\n2022.07.29");
        m.setCellStyle(greenStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 11, 12));
        Cell mg = r1.createCell(11);
        mg.setCellValue("관리처분인가\n2021.01.29");
        mg.setCellStyle(peachStyle);

        sheet.createRow(1).setHeightInPoints(36f);
    }

    private void createHeaderRow(XSSFSheet sheet, XSSFCellStyle headerStyle) {
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

    private void createDataRow(XSSFSheet sheet, int rowIdx, int seq,
                                ConsultationRequest r,
                                XSSFCellStyle center, XSSFCellStyle left,
                                XSSFCellStyle date, XSSFCellStyle number) {
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

    // ──────────────────────────────────────────
    //  총합 시트 (원본 구조 정확 재현)
    // ──────────────────────────────────────────

    private void createSummarySheet(XSSFWorkbook wb, String approvalNo, long totalLimit) {
        XSSFSheet ws = wb.createSheet("총합");

        // 컬럼 너비
        ws.setColumnWidth(0, (int)(2.7  * 256));
        ws.setColumnWidth(1, (int)(14.7 * 256));
        ws.setColumnWidth(2, (int)(8.0  * 256));
        ws.setColumnWidth(3, (int)(10.7 * 256));
        ws.setColumnWidth(4, (int)(20.7 * 256));
        ws.setColumnWidth(5, (int)(3.0  * 256));
        ws.setColumnWidth(6, (int)(14.7 * 256));
        ws.setColumnWidth(7, (int)(10.7 * 256));
        ws.setColumnWidth(8, (int)(22.5 * 256));
        ws.setColumnWidth(9, (int)(2.3  * 256));

        Font boldFont = wb.createFont(); boldFont.setBold(true);

        // 공통 스타일 (경계 없음)
        XSSFCellStyle centerNB = wb.createCellStyle();
        centerNB.setAlignment(HorizontalAlignment.CENTER);
        centerNB.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFCellStyle leftNB = wb.createCellStyle();
        leftNB.setAlignment(HorizontalAlignment.LEFT);
        leftNB.setVerticalAlignment(VerticalAlignment.CENTER);

        // 헤더 스타일 (medium 테두리 + 연파랑)
        XSSFCellStyle hdrBLMR   = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, THIN,   "D9E1F2", true, null);
        XSSFCellStyle hdrMid    = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, NONE,   THIN,   "D9E1F2", true, null);
        XSSFCellStyle hdrLthin  = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, THIN,   THIN,   "D9E1F2", true, null);
        XSSFCellStyle hdrRight  = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, NONE,   MEDIUM, "D9E1F2", true, null);
        XSSFCellStyle hdrRLthin = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, THIN,   MEDIUM, "D9E1F2", true, null);

        // 데이터 스타일 (dashed + 연파랑)
        XSSFCellStyle dataBLMR  = make(wb, HorizontalAlignment.CENTER, DASHED, DASHED, MEDIUM, THIN,   "D9E1F2", false, null);
        XSSFCellStyle dataMid   = make(wb, HorizontalAlignment.RIGHT,  DASHED, DASHED, NONE,   THIN,   "D9E1F2", false, null);
        XSSFCellStyle dataLthin = make(wb, HorizontalAlignment.RIGHT,  DASHED, DASHED, THIN,   THIN,   "D9E1F2", false, "#,##0");
        XSSFCellStyle dataRight = make(wb, HorizontalAlignment.RIGHT,  DASHED, DASHED, THIN,   MEDIUM, "D9E1F2", false, "#,##0");

        // 취소행 스타일 (하단 medium)
        XSSFCellStyle cancelBLMR  = make(wb, HorizontalAlignment.CENTER, DASHED, MEDIUM, MEDIUM, THIN,   "D9E1F2", false, null);
        XSSFCellStyle cancelMid   = make(wb, HorizontalAlignment.LEFT,   DASHED, MEDIUM, NONE,   THIN,   "D9E1F2", false, null);
        XSSFCellStyle cancelLthin = make(wb, HorizontalAlignment.RIGHT,  DASHED, MEDIUM, THIN,   THIN,   "D9E1F2", false, "#,##0");
        XSSFCellStyle cancelRight = make(wb, HorizontalAlignment.RIGHT,  DASHED, MEDIUM, THIN,   MEDIUM, "D9E1F2", false, "#,##0");

        // 합계행 스타일 (medium + 노랑)
        XSSFCellStyle sumBLMR  = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, THIN,   "FFFF00", true,  null);
        XSSFCellStyle sumMid   = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, NONE,   THIN,   "FFFF00", false, null);
        XSSFCellStyle sumLthin = make(wb, HorizontalAlignment.RIGHT,  MEDIUM, MEDIUM, THIN,   THIN,   "FFFF00", false, "#,##0");
        XSSFCellStyle sumRight = make(wb, HorizontalAlignment.RIGHT,  MEDIUM, MEDIUM, THIN,   MEDIUM, "FFFF00", false, "#,##0");

        // 월별 스타일
        XSSFCellStyle mFirst     = make(wb, HorizontalAlignment.CENTER, MEDIUM, THIN,   MEDIUM, THIN, null, false, null);
        XSSFCellStyle mMid       = make(wb, HorizontalAlignment.CENTER, THIN,   THIN,   MEDIUM, THIN, null, false, null);
        XSSFCellStyle mLast      = make(wb, HorizontalAlignment.CENTER, THIN,   MEDIUM, MEDIUM, THIN, null, false, null);
        XSSFCellStyle mCellFirst = make(wb, HorizontalAlignment.CENTER, MEDIUM, THIN,   THIN,   THIN, null, false, "#,##0");
        XSSFCellStyle mCellMid   = make(wb, HorizontalAlignment.CENTER, THIN,   THIN,   THIN,   THIN, null, false, "#,##0");
        XSSFCellStyle mCellLast  = make(wb, HorizontalAlignment.CENTER, THIN,   MEDIUM, THIN,   THIN, null, false, "#,##0");
        XSSFCellStyle mRFirst    = make(wb, HorizontalAlignment.CENTER, MEDIUM, THIN,   THIN, MEDIUM,  null, false, "#,##0");
        XSSFCellStyle mRMid      = make(wb, HorizontalAlignment.CENTER, THIN,   THIN,   THIN, MEDIUM,  null, false, "#,##0");
        XSSFCellStyle mRLast     = make(wb, HorizontalAlignment.CENTER, THIN,   MEDIUM, THIN, MEDIUM,  null, false, "#,##0");

        // ── row2~3: 제목 병합 ──
        ws.addMergedRegion(new CellRangeAddress(1, 2, 1, 8));
        Row r2 = ws.createRow(1); r2.setHeightInPoints(14.5f);
        Cell titleCell = r2.createCell(1);
        titleCell.setCellFormula("접수일!A1");
        titleCell.setCellStyle(centerNB);
        RegionUtil.setBorderTop(THIN,    new CellRangeAddress(1, 2, 1, 8), ws);
        RegionUtil.setBorderBottom(THIN, new CellRangeAddress(1, 2, 1, 8), ws);
        RegionUtil.setBorderLeft(THIN,   new CellRangeAddress(1, 2, 1, 8), ws);
        RegionUtil.setBorderRight(THIN,  new CellRangeAddress(1, 2, 1, 8), ws);
        Row r3 = ws.createRow(2); r3.setHeightInPoints(37.5f);

        // ── row4: 승인번호 / 총한도 ──
        Row r4 = ws.createRow(3); r4.setHeightInPoints(19.5f);
        ws.addMergedRegion(new CellRangeAddress(3, 3, 1, 3));
        Cell apvCell = r4.createCell(1);
        apvCell.setCellValue("승인번호 : " + approvalNo);
        apvCell.setCellStyle(centerNB);
        RegionUtil.setBorderTop(THIN,    new CellRangeAddress(3, 3, 1, 3), ws);
        RegionUtil.setBorderBottom(THIN, new CellRangeAddress(3, 3, 1, 3), ws);

        ws.addMergedRegion(new CellRangeAddress(3, 3, 6, 7));
        Cell limCell = r4.createCell(6);
        limCell.setCellValue("총한도 : " + (totalLimit / 100_000_000L) + "억원");
        limCell.setCellStyle(leftNB);
        RegionUtil.setBorderTop(THIN,    new CellRangeAddress(3, 3, 6, 7), ws);
        RegionUtil.setBorderBottom(THIN, new CellRangeAddress(3, 3, 6, 7), ws);

        // ── row5: ▣ 총접수 / ▣ 당일접수 / TODAY() ──
        Row r5 = ws.createRow(4); r5.setHeightInPoints(30.5f);
        ws.addMergedRegion(new CellRangeAddress(4, 4, 1, 4));
        Cell totalLbl = r5.createCell(1);
        totalLbl.setCellValue("▣ 총접수");
        totalLbl.setCellStyle(leftNB);
        RegionUtil.setBorderTop(THIN,      new CellRangeAddress(4, 4, 1, 4), ws);
        RegionUtil.setBorderBottom(MEDIUM, new CellRangeAddress(4, 4, 1, 4), ws);

        ws.addMergedRegion(new CellRangeAddress(4, 4, 6, 7));
        Cell dailyLbl = r5.createCell(6);
        dailyLbl.setCellValue("▣ 당일접수");
        dailyLbl.setCellStyle(leftNB);
        RegionUtil.setBorderTop(THIN,      new CellRangeAddress(4, 4, 6, 7), ws);
        RegionUtil.setBorderBottom(MEDIUM, new CellRangeAddress(4, 4, 6, 7), ws);

        XSSFCellStyle todayStyle = wb.createCellStyle();
        todayStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));
        Cell todayCell = r5.createCell(8);
        todayCell.setCellFormula("TODAY()");
        todayCell.setCellStyle(todayStyle);

        // ── row6: 헤더 ──
        Row r6 = ws.createRow(5); r6.setHeightInPoints(28.0f);
        setC(r6, 1, "분   류",    hdrBLMR);
        setC(r6, 2, "한도",       hdrMid);
        setC(r6, 3, "건수",       hdrLthin);
        setC(r6, 4, "건별금액",   hdrRight);
        setC(r6, 6, "분   류",    hdrBLMR);
        setC(r6, 7, "건수",       hdrLthin);
        setC(r6, 8, "금액",       hdrRLthin);

        // ── row7: 고정 ──
        Row r7 = ws.createRow(6); r7.setHeightInPoints(30.0f);
        setC(r7, 1, "고            정", dataBLMR);
        r7.createCell(2).setCellStyle(dataMid);
        formula(r7, 3, "COUNTIF(접수일!$Q$5:$Q$860,\"고정\")", dataLthin);
        formula(r7, 4, "SUMIF(접수일!$Q$5:$Q$860,\"고정\",접수일!$M$5:$M$860)", dataRight);
        setC(r7, 6, "고          정", dataBLMR);
        formula(r7, 7, "COUNTIFS(접수일!$N$5:$N$860,$I$5,접수일!$Q$5:$Q$860,\"고정\",접수일!$L$5:$L$860,\"<>취소\")", dataLthin);
        formula(r7, 8, "SUMIFS(접수일!$M$5:$M$860,접수일!$N$5:$N$860,총합!$I$5,접수일!$Q$5:$Q$860,\"고정\",접수일!$L$5:$L$860,\"<>취소\")", dataRight);

        // ── row8: 변동 ──
        Row r8 = ws.createRow(7); r8.setHeightInPoints(30.0f);
        setC(r8, 1, "변           동", dataBLMR);
        r8.createCell(2).setCellStyle(dataMid);
        formula(r8, 3, "COUNTIF(접수일!$Q$5:$Q$860,\"변동\")", dataLthin);
        formula(r8, 4, "SUMIF(접수일!$Q$5:$Q$860,\"변동\",접수일!$M$5:$M$860)", dataRight);
        setC(r8, 6, "변         동", dataBLMR);
        formula(r8, 7, "COUNTIFS(접수일!$N$5:$N$860,$I$5,접수일!$Q$5:$Q$860,\"변동\",접수일!$L$5:$L$860,\"<>취소\")", dataLthin);
        formula(r8, 8, "SUMIFS(접수일!$M$5:$M$860,접수일!$N$5:$N$860,총합!$I$5,접수일!$Q$5:$Q$860,\"변동\",접수일!$L$5:$L$860,\"<>취소\")", dataRight);

        // ── row9: 취소 ──
        Row r9 = ws.createRow(8); r9.setHeightInPoints(30.0f);
        setC(r9, 1, "취           소", cancelBLMR);
        r9.createCell(2).setCellStyle(cancelMid);
        formula(r9, 3, "COUNTIF(접수일!$L$5:$L$860,\"취소\")", cancelLthin);
        formula(r9, 4, "SUMIF(접수일!$L$5:$L$860,\"취소\",접수일!$M$5:$M$860)", cancelRight);
        setC(r9, 6, "취         소", cancelBLMR);
        r9.createCell(7).setCellStyle(cancelLthin);
        r9.createCell(8).setCellStyle(cancelRight);

        // ── row10: 합계 (노랑) ──
        Row r10 = ws.createRow(9); r10.setHeightInPoints(30.0f);
        setC(r10, 1, "총  합계",   sumBLMR);
        r10.createCell(2).setCellStyle(sumMid);
        formula(r10, 3, "SUM(D7:D8)-D9", sumLthin);
        formula(r10, 4, "SUM(E7:E8)-E9", sumRight);
        setC(r10, 6, "당일 합계", sumBLMR);
        formula(r10, 7, "SUM(H7:H8)-H9", sumLthin);
        formula(r10, 8, "SUM(I7:I8)-I9", sumRight);

        // ── row11: 빈 행 ──
        ws.createRow(10).setHeightInPoints(8.25f);

        // ── row12: 월별 실행예정 라벨 ──
        Row r12 = ws.createRow(11); r12.setHeightInPoints(28.5f);
        Cell mLbl = r12.createCell(6);
        mLbl.setCellValue("▣ 월별 실행예정");
        mLbl.setCellStyle(centerNB);

        // ── row13~17: 월별 데이터 ──
        String[][] months = {
            {"4월", "일별실행건수!E35",  "일별실행건수!F35"},
            {"5월", "일별실행건수!E66",  "일별실행건수!F66"},
            {"6월", null,               null},
            {"7월", "일별실행건수!E127", "일별실행건수!F127"},
            {"8월", "일별실행건수!E158", "일별실행건수!F158"},
        };
        for (int i = 0; i < months.length; i++) {
            Row row = ws.createRow(12 + i); row.setHeightInPoints(30.0f);
            boolean isFirst = (i == 0), isLast = (i == months.length - 1);

            XSSFCellStyle lblS  = isFirst ? mFirst     : (isLast ? mLast     : mMid);
            XSSFCellStyle cntS  = isFirst ? mCellFirst : (isLast ? mCellLast : mCellMid);
            XSSFCellStyle amtS  = isFirst ? mRFirst    : (isLast ? mRLast    : mRMid);

            setC(row, 6, months[i][0], lblS);
            Cell cntCell = row.createCell(7); cntCell.setCellStyle(cntS);
            Cell amtCell = row.createCell(8); amtCell.setCellStyle(amtS);

            if (i == 2) { // 6월 하드코딩
                cntCell.setCellValue(2);
                amtCell.setCellValue(496_000_000);
            } else {
                cntCell.setCellFormula(months[i][1]);
                amtCell.setCellFormula(months[i][2]);
            }
        }
    }

    // ──────────────────────────────────────────
    //  셀 헬퍼
    // ──────────────────────────────────────────

    private void setCell(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val : "");
        cell.setCellStyle(style);
    }

    private void setDateCell(Row row, int col, LocalDate d, CellStyle dateStyle, CellStyle empty) {
        Cell cell = row.createCell(col);
        if (d != null) { cell.setCellValue(d.toString()); cell.setCellStyle(dateStyle); }
        else cell.setCellStyle(empty);
    }

    private void setC(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val : "");
        c.setCellStyle(style);
    }

    private void formula(Row row, int col, String f, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellFormula(f);
        c.setCellStyle(style);
    }
}
