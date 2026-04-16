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

    public byte[] generateExcel(
            String complexName,
            String complexFullName,
            String approvalNo,
            long totalLimit,
            String bankName,
            String bankManager,
            String bankPhone,
            String bankFax,
            List<ConsultationRequest> list) throws Exception {

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            DataFormat fmt = wb.createDataFormat();

            // ── 날짜 포맷 상수 ──
            short FMT_MONTHLY   = fmt.getFormat("m\"월\" d\"일\";@");                    // 접수일 시트 날짜
            short FMT_DAILY     = fmt.getFormat("yyyy-mm-dd");                            // 일별 시트 날짜
            short FMT_TODAY     = fmt.getFormat("mm-dd-yy");                             // TODAY() 셀
            short FMT_COUNT     = fmt.getFormat("0_);[Red]\\(0\\)");                    // 건수
            short FMT_AMOUNT    = fmt.getFormat("_-* #,##0_-;\\-* #,##0_-;_-* \"-\"_-;_-@_-"); // 금액

            // ── 공통 스타일 ──
            XSSFCellStyle centerStyle = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false);
            XSSFCellStyle leftStyle   = baseStyle(wb, HorizontalAlignment.LEFT,   THIN, THIN, THIN, THIN, null, true);  // B열 wrap=true
            XSSFCellStyle headerStyle = headerStyle(wb);
            XSSFCellStyle greenStyle  = colorStyle(wb, "FF92D050", true);
            XSSFCellStyle peachStyle  = colorStyle(wb, "FFF3C6BF", true);

            // 접수일 날짜 스타일 (m월 d일)
            XSSFCellStyle dateMonthly = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false);
            dateMonthly.setDataFormat(FMT_MONTHLY);

            XSSFCellStyle numberStyle = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false);
            numberStyle.setDataFormat(fmt.getFormat("#,##0"));

            // ── Sheet 1: 접수일 ──
            XSSFSheet receiptSheet = wb.createSheet("접수일");
            setColumnWidths(receiptSheet);
            createTitleRows(receiptSheet, complexName, greenStyle, peachStyle, wb);
            createHeaderRow(receiptSheet, headerStyle);
            int rowIdx = 4, seq = 1;
            for (ConsultationRequest r : list) {
                createDataRow(receiptSheet, rowIdx++, seq++, r, centerStyle, leftStyle, dateMonthly, numberStyle);
            }

            // ── Sheet 2: 총합 ──
            createSummarySheet(wb, approvalNo, totalLimit, FMT_TODAY);

            // ── Sheet 3: 일별접수건수 ──
            createDailySheet(wb, "일별접수건수", "N", FMT_DAILY, FMT_COUNT, FMT_AMOUNT);

            // ── Sheet 4: 일별실행건수 ──
            createDailySheet(wb, "일별실행건수", "L", FMT_DAILY, FMT_COUNT, FMT_AMOUNT);

            // ── Sheet 5: 상환예정내역표 ──
            createRepaymentSheet(wb, fmt);

            // ── Sheet 6: 잔금조회 ──
            createBalanceInquirySheet(wb, bankName, complexFullName, bankManager, bankPhone, bankFax, fmt, FMT_MONTHLY, FMT_TODAY, FMT_AMOUNT);

            // ── Sheet 7: 중도금조회 ──
            createProgressPaymentSheet(wb, bankName, complexFullName, bankManager, bankPhone, bankFax, fmt, FMT_MONTHLY, FMT_TODAY, FMT_AMOUNT);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ══════════════════════════════════════════════
    //  스타일 팩토리
    // ══════════════════════════════════════════════

    private XSSFCellStyle baseStyle(XSSFWorkbook wb, HorizontalAlignment align,
                                     BorderStyle top, BorderStyle bottom,
                                     BorderStyle left, BorderStyle right,
                                     String hexARGB, boolean wrap) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(wrap);
        s.setBorderTop(top); s.setBorderBottom(bottom);
        s.setBorderLeft(left); s.setBorderRight(right);
        if (hexARGB != null) setFill(s, hexARGB);
        return s;
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "FFD9E1F2", true);
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        return s;
    }

    private XSSFCellStyle colorStyle(XSSFWorkbook wb, String hexARGB, boolean bold) {
        XSSFCellStyle s = baseStyle(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, hexARGB, true);
        if (bold) { Font f = wb.createFont(); f.setBold(true); s.setFont(f); }
        return s;
    }

    private void setFill(XSSFCellStyle s, String hexARGB) {
        byte[] argb = new byte[]{
            (byte) Integer.parseInt(hexARGB.substring(0, 2), 16),
            (byte) Integer.parseInt(hexARGB.substring(2, 4), 16),
            (byte) Integer.parseInt(hexARGB.substring(4, 6), 16),
            (byte) Integer.parseInt(hexARGB.substring(6, 8), 16)
        };
        s.setFillForegroundColor(new XSSFColor(argb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    private XSSFCellStyle make(XSSFWorkbook wb, HorizontalAlignment align,
                                BorderStyle t, BorderStyle b, BorderStyle l, BorderStyle r,
                                String hexARGB, boolean bold, short numFmt) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(false);
        s.setBorderTop(t); s.setBorderBottom(b);
        s.setBorderLeft(l); s.setBorderRight(r);
        if (hexARGB != null) setFill(s, hexARGB);
        if (bold) { Font f = wb.createFont(); f.setBold(true); s.setFont(f); }
        if (numFmt > 0) s.setDataFormat(numFmt);
        return s;
    }

    private XSSFCellStyle makeStr(XSSFWorkbook wb, HorizontalAlignment align,
                                   BorderStyle t, BorderStyle b, BorderStyle l, BorderStyle r,
                                   String hexARGB, boolean bold, String numFmtStr) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(false);
        s.setBorderTop(t); s.setBorderBottom(b);
        s.setBorderLeft(l); s.setBorderRight(r);
        if (hexARGB != null) setFill(s, hexARGB);
        if (bold) { Font f = wb.createFont(); f.setBold(true); s.setFont(f); }
        if (numFmtStr != null) s.setDataFormat(wb.createDataFormat().getFormat(numFmtStr));
        return s;
    }

    // ══════════════════════════════════════════════
    //  접수일 시트
    // ══════════════════════════════════════════════

    private void setColumnWidths(XSSFSheet sheet) {
        double[] widths = {5.4140625, 30, 8, 8.4140625, 6, 6, 10, 18, 8, 8, 16,
                10, 15, 10, 10, 7, 8, 7, 7, 7,
                10, 18, 16, 17.25, 18, 18, 18, 21.58203125};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, (int)(widths[i] * 256));
    }

    private void createTitleRows(XSSFSheet sheet, String complexName,
                                  XSSFCellStyle greenStyle, XSSFCellStyle peachStyle,
                                  XSSFWorkbook wb) {
        Row r1 = sheet.createRow(0); r1.setHeightInPoints(17.25f);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 7));
        Cell title = r1.createCell(0);
        title.setCellValue(complexName + " 접수리스트");
        title.setCellStyle(greenStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 9, 10));
        Cell notice = r1.createCell(9);
        notice.setCellValue("모집공고일\n2022.07.29");
        notice.setCellStyle(greenStyle);

        sheet.addMergedRegion(new CellRangeAddress(0, 1, 11, 12));
        Cell mgmt = r1.createCell(11);
        mgmt.setCellValue("관리처분인가                  2021.01.29");
        mgmt.setCellStyle(peachStyle);

        XSSFCellStyle centerNB = wb.createCellStyle();
        centerNB.setAlignment(HorizontalAlignment.CENTER);
        centerNB.setVerticalAlignment(VerticalAlignment.CENTER);
        Cell z1 = r1.createCell(25); z1.setCellValue("조합계좌"); z1.setCellStyle(centerNB);
        Cell aa1 = r1.createCell(26); aa1.setCellValue("일반계좌"); aa1.setCellStyle(centerNB);

        sheet.createRow(1).setHeightInPoints(36f);
        sheet.createRow(2).setHeightInPoints(18.65f);
    }

    private void createHeaderRow(XSSFSheet sheet, XSSFCellStyle headerStyle) {
        Row row = sheet.createRow(3); row.setHeightInPoints(36f);
        String[] headers = {
            "순번", "불비 및 특이사항", "담당", "전매일", "구분", "명의",
            "고객명", "주민등록번호", "동", "호수", "연락처",
            "실행일", "대출신청금", "접수일\n(취소일)", "서류\n전달일",
            "타입", "상품", "상환\n방식", "기간", "거치",
            "공동\n명의자", "주민등록번호", "연락처",
            "고객준비금", "후불이자계좌", "잔금계좌", "옵션 계좌", "중도금가상계좌\n(타행)"
        };
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRow(XSSFSheet sheet, int rowIdx, int seq,
                                ConsultationRequest r,
                                XSSFCellStyle center, XSSFCellStyle left,
                                XSSFCellStyle date, XSSFCellStyle number) {
        Row row = sheet.createRow(rowIdx); row.setHeightInPoints(36f);

        setCell(row, 0,  String.valueOf(seq),   center);
        setCell(row, 1,  r.getSpecial_notes(),  left);
        setCell(row, 2,  r.getManager(),        center);
        setCell(row, 3,  r.getTransfer_date(),  center);
        setCell(row, 4,  r.getDivision(),       center);
        setCell(row, 5,  r.getOwnership(),      center);
        setCell(row, 6,  r.getResident_name(),  center);
        setCell(row, 7,  r.getResident_no(),    center);
        setCell(row, 8,  r.getDong(),           center);
        setCell(row, 9,  r.getHo(),             center);
        setCell(row, 10, r.getResident_phone(), center);

        // L열(11): 실행일 → m월 d일
        Cell execCell = row.createCell(11);
        if ("cancel".equals(r.getLoan_status())) {
            execCell.setCellValue("취소"); execCell.setCellStyle(center);
        } else if (r.getExecution_date() != null) {
            execCell.setCellValue(java.sql.Date.valueOf(r.getExecution_date())); execCell.setCellStyle(date);
        } else { execCell.setCellStyle(center); }

        Cell amtCell = row.createCell(12);
        if (r.getLoan_amount() != null) amtCell.setCellValue(r.getLoan_amount());
        amtCell.setCellStyle(number);

        // N열(13): 접수일, O열(14): 서류전달일 → m월 d일
        setDateCell(row, 13, r.getReceive_date(),  date, center);
        setDateCell(row, 14, r.getDocument_date(), date, center);

        setCell(row, 15, r.getApt_type(),          center);
        setCell(row, 16, r.getProduct(),           center);
        setCell(row, 17, r.getRepayment_method(),  center);
        setCell(row, 18, r.getLoan_period(),        center);
        setCell(row, 19, r.getDeferment(),         center);
        setCell(row, 20, r.getJoint_owner_name(),  center);
        setCell(row, 21, r.getJoint_owner_rrn(),   center);
        setCell(row, 22, r.getJoint_owner_tel(),   center);

        Cell depositCell = row.createCell(23);
        depositCell.setCellValue(r.getCustomer_deposit() != null ? r.getCustomer_deposit() : 0);
        depositCell.setCellStyle(number);

        setCell(row, 24, r.getDeferred_interest_account(), center);
        setCell(row, 25, r.getBalance_account(),            center);
        setCell(row, 26, r.getOption_account(),             center);
        setCell(row, 27, r.getInterim_virtual_account(),    center);
    }

    // ══════════════════════════════════════════════
    //  총합 시트
    // ══════════════════════════════════════════════

    private void createSummarySheet(XSSFWorkbook wb, String approvalNo, long totalLimit, short fmtToday) {
        XSSFSheet ws = wb.createSheet("총합");

        ws.setColumnWidth(0, (int)(2.6640625  * 256));
        ws.setColumnWidth(1, (int)(14.6640625 * 256));
        ws.setColumnWidth(2, (int)(8.0        * 256));
        ws.setColumnWidth(3, (int)(10.6640625 * 256));
        ws.setColumnWidth(4, (int)(20.6640625 * 256));
        ws.setColumnWidth(5, (int)(3.0        * 256));
        ws.setColumnWidth(6, (int)(14.6640625 * 256));
        ws.setColumnWidth(7, (int)(10.6640625 * 256));
        ws.setColumnWidth(8, (int)(22.5       * 256));
        ws.setColumnWidth(9, (int)(2.33203125 * 256));

        XSSFCellStyle centerNB = wb.createCellStyle();
        centerNB.setAlignment(HorizontalAlignment.CENTER);
        centerNB.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFCellStyle leftNB = wb.createCellStyle();
        leftNB.setAlignment(HorizontalAlignment.LEFT);
        leftNB.setVerticalAlignment(VerticalAlignment.CENTER);

        short z = (short)0;
        XSSFCellStyle hdrBLMR  = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, THIN,   "FFD9E1F2", true,  z);
        XSSFCellStyle hdrMid   = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, NONE,   THIN,   "FFD9E1F2", true,  z);
        XSSFCellStyle hdrLthin = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, THIN,   THIN,   "FFD9E1F2", true,  z);
        XSSFCellStyle hdrRight = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, NONE,   MEDIUM, "FFD9E1F2", true,  z);
        XSSFCellStyle hdrRLthin= make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, THIN,   MEDIUM, "FFD9E1F2", true,  z);

        DataFormat fmt = wb.createDataFormat();
        short fmtNum = fmt.getFormat("#,##0");

        XSSFCellStyle dataBLMR = make(wb, HorizontalAlignment.CENTER, DASHED, DASHED, MEDIUM, THIN,   "FFD9E1F2", false, z);
        XSSFCellStyle dataMid  = make(wb, HorizontalAlignment.RIGHT,  DASHED, DASHED, NONE,   THIN,   "FFD9E1F2", false, z);
        XSSFCellStyle dataLthin= make(wb, HorizontalAlignment.RIGHT,  DASHED, DASHED, THIN,   THIN,   "FFD9E1F2", false, fmtNum);
        XSSFCellStyle dataRight= make(wb, HorizontalAlignment.RIGHT,  DASHED, DASHED, THIN,   MEDIUM, "FFD9E1F2", false, fmtNum);

        XSSFCellStyle cxBLMR   = make(wb, HorizontalAlignment.CENTER, DASHED, MEDIUM, MEDIUM, THIN,   "FFD9E1F2", false, z);
        XSSFCellStyle cxMid    = make(wb, HorizontalAlignment.LEFT,   DASHED, MEDIUM, NONE,   THIN,   "FFD9E1F2", false, z);
        XSSFCellStyle cxLthin  = make(wb, HorizontalAlignment.RIGHT,  DASHED, MEDIUM, THIN,   THIN,   "FFD9E1F2", false, fmtNum);
        XSSFCellStyle cxRight  = make(wb, HorizontalAlignment.RIGHT,  DASHED, MEDIUM, THIN,   MEDIUM, "FFD9E1F2", false, fmtNum);

        XSSFCellStyle sumBLMR  = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, THIN,   "FFFFFF00", true,  z);
        XSSFCellStyle sumMid   = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, NONE,   THIN,   "FFFFFF00", false, z);
        XSSFCellStyle sumLthin = make(wb, HorizontalAlignment.RIGHT,  MEDIUM, MEDIUM, THIN,   THIN,   "FFFFFF00", false, fmtNum);
        XSSFCellStyle sumRight = make(wb, HorizontalAlignment.RIGHT,  MEDIUM, MEDIUM, THIN,   MEDIUM, "FFFFFF00", false, fmtNum);

        XSSFCellStyle mFirst   = make(wb, HorizontalAlignment.CENTER, MEDIUM, THIN,   MEDIUM, THIN,   null, false, z);
        XSSFCellStyle mMid     = make(wb, HorizontalAlignment.CENTER, THIN,   THIN,   MEDIUM, THIN,   null, false, z);
        XSSFCellStyle mLast    = make(wb, HorizontalAlignment.CENTER, THIN,   MEDIUM, MEDIUM, THIN,   null, false, z);
        XSSFCellStyle mCFirst  = make(wb, HorizontalAlignment.CENTER, MEDIUM, THIN,   THIN,   THIN,   null, false, fmtNum);
        XSSFCellStyle mCMid    = make(wb, HorizontalAlignment.CENTER, THIN,   THIN,   THIN,   THIN,   null, false, fmtNum);
        XSSFCellStyle mCLast   = make(wb, HorizontalAlignment.CENTER, THIN,   MEDIUM, THIN,   THIN,   null, false, fmtNum);
        XSSFCellStyle mRFirst  = make(wb, HorizontalAlignment.CENTER, MEDIUM, THIN,   THIN,   MEDIUM, null, false, fmtNum);
        XSSFCellStyle mRMid    = make(wb, HorizontalAlignment.CENTER, THIN,   THIN,   THIN,   MEDIUM, null, false, fmtNum);
        XSSFCellStyle mRLast   = make(wb, HorizontalAlignment.CENTER, THIN,   MEDIUM, THIN,   MEDIUM, null, false, fmtNum);

        // row2~3 제목
        Row r2 = ws.createRow(1); r2.setHeightInPoints(14.5f);
        Row r3 = ws.createRow(2); r3.setHeightInPoints(37.5f);
        CellRangeAddress titleRange = new CellRangeAddress(1, 2, 1, 8);
        ws.addMergedRegion(titleRange);
        Cell titleCell = r2.createCell(1); titleCell.setCellFormula("접수일!A1"); titleCell.setCellStyle(centerNB);
        RegionUtil.setBorderTop(THIN, titleRange, ws); RegionUtil.setBorderBottom(THIN, titleRange, ws);
        RegionUtil.setBorderLeft(THIN, titleRange, ws); RegionUtil.setBorderRight(THIN, titleRange, ws);

        // row4 승인번호/총한도
        Row r4 = ws.createRow(3); r4.setHeightInPoints(19.5f);
        CellRangeAddress apvRange = new CellRangeAddress(3, 3, 1, 3);
        ws.addMergedRegion(apvRange);
        Cell apvCell = r4.createCell(1); apvCell.setCellValue("승인번호 : " + approvalNo); apvCell.setCellStyle(centerNB);
        RegionUtil.setBorderTop(THIN, apvRange, ws); RegionUtil.setBorderBottom(THIN, apvRange, ws);

        XSSFCellStyle e4Style = wb.createCellStyle(); e4Style.setBorderTop(THIN);
        r4.createCell(4).setCellStyle(e4Style);

        CellRangeAddress limRange = new CellRangeAddress(3, 3, 6, 7);
        ws.addMergedRegion(limRange);
        Cell limCell = r4.createCell(6); limCell.setCellValue("총한도 : " + (totalLimit / 100_000_000L) + "억원"); limCell.setCellStyle(leftNB);
        RegionUtil.setBorderTop(THIN, limRange, ws); RegionUtil.setBorderBottom(THIN, limRange, ws);

        // row5 ▣ 총접수/당일접수 + TODAY()
        Row r5 = ws.createRow(4); r5.setHeightInPoints(30.5f);
        CellRangeAddress totalRange = new CellRangeAddress(4, 4, 1, 4);
        ws.addMergedRegion(totalRange);
        Cell totalLbl = r5.createCell(1); totalLbl.setCellValue("▣ 총접수"); totalLbl.setCellStyle(leftNB);
        RegionUtil.setBorderTop(THIN, totalRange, ws); RegionUtil.setBorderBottom(MEDIUM, totalRange, ws);

        CellRangeAddress dailyRange = new CellRangeAddress(4, 4, 6, 7);
        ws.addMergedRegion(dailyRange);
        Cell dailyLbl = r5.createCell(6); dailyLbl.setCellValue("▣ 당일접수"); dailyLbl.setCellStyle(leftNB);
        RegionUtil.setBorderTop(THIN, dailyRange, ws); RegionUtil.setBorderBottom(MEDIUM, dailyRange, ws);

        // I5: TODAY() - mm-dd-yy 포맷
        XSSFCellStyle todayStyle = wb.createCellStyle(); todayStyle.setDataFormat(fmtToday);
        Cell todayCell = r5.createCell(8); todayCell.setCellFormula("TODAY()"); todayCell.setCellStyle(todayStyle);

        // row6 헤더
        Row r6 = ws.createRow(5); r6.setHeightInPoints(28.0f);
        setC(r6, 1, "분   류",  hdrBLMR); setC(r6, 2, "한도", hdrMid);
        setC(r6, 3, "건수",     hdrLthin); setC(r6, 4, "건별금액", hdrRight);
        setC(r6, 6, "분   류",  hdrBLMR); setC(r6, 7, "건수", hdrLthin); setC(r6, 8, "금액", hdrRLthin);

        // row7 고정
        Row r7 = ws.createRow(6); r7.setHeightInPoints(30.0f);
        setC(r7, 1, "고           정", dataBLMR); r7.createCell(2).setCellStyle(dataMid);
        formula(r7, 3, "COUNTIF(접수일!$Q$5:$Q$860,\"고정\")", dataLthin);
        formula(r7, 4, "SUMIF(접수일!$Q$5:$Q$860,\"고정\",접수일!$M$5:$M$860)", dataRight);
        setC(r7, 6, "고         정", dataBLMR);
        formula(r7, 7, "COUNTIFS(접수일!$N$5:$N$860,$I$5,접수일!$Q$5:$Q$860,\"고정\",접수일!$L$5:$L$860,\"<>취소\")", dataLthin);
        formula(r7, 8, "SUMIFS(접수일!$M$5:$M$860,접수일!$N$5:$N$860,총합!$I$5,접수일!$Q$5:$Q$860,\"고정\",접수일!$L$5:$L$860,\"<>취소\")", dataRight);

        // row8 변동
        Row r8 = ws.createRow(7); r8.setHeightInPoints(30.0f);
        setC(r8, 1, "변           동", dataBLMR); r8.createCell(2).setCellStyle(dataMid);
        formula(r8, 3, "COUNTIF(접수일!$Q$5:$Q$860,\"변동\")", dataLthin);
        formula(r8, 4, "SUMIF(접수일!$Q$5:$Q$860,\"변동\",접수일!$M$5:$M$860)", dataRight);
        setC(r8, 6, "변         동", dataBLMR);
        formula(r8, 7, "COUNTIFS(접수일!$N$5:$N$860,$I$5,접수일!$Q$5:$Q$860,\"변동\",접수일!$L$5:$L$860,\"<>취소\")", dataLthin);
        formula(r8, 8, "SUMIFS(접수일!$M$5:$M$860,접수일!$N$5:$N$860,총합!$I$5,접수일!$Q$5:$Q$860,\"변동\",접수일!$L$5:$L$860,\"<>취소\")", dataRight);

        // row9 취소
        Row r9 = ws.createRow(8); r9.setHeightInPoints(30.0f);
        setC(r9, 1, "취           소", cxBLMR); r9.createCell(2).setCellStyle(cxMid);
        formula(r9, 3, "COUNTIF(접수일!$L$5:$L$860,\"취소\")", cxLthin);
        formula(r9, 4, "SUMIF(접수일!$L$5:$L$860,\"취소\",접수일!$M$5:$M$860)", cxRight);
        setC(r9, 6, "취         소", cxBLMR); r9.createCell(7).setCellStyle(cxLthin); r9.createCell(8).setCellStyle(cxRight);

        // row10 합계
        Row r10 = ws.createRow(9); r10.setHeightInPoints(30.0f);
        setC(r10, 1, "총  합계",   sumBLMR); r10.createCell(2).setCellStyle(sumMid);
        formula(r10, 3, "SUM(D7:D8)-D9", sumLthin); formula(r10, 4, "SUM(E7:E8)-E9", sumRight);
        setC(r10, 6, "당일 합계", sumBLMR);
        formula(r10, 7, "SUM(H7:H8)-H9", sumLthin); formula(r10, 8, "SUM(I7:I8)-I9", sumRight);

        ws.createRow(10).setHeightInPoints(8.25f);

        Row r12 = ws.createRow(11); r12.setHeightInPoints(28.5f);
        Cell mLbl = r12.createCell(6); mLbl.setCellValue("▣ 월별 실행예정"); mLbl.setCellStyle(centerNB);

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
            setC(row, 6, months[i][0], isFirst ? mFirst : (isLast ? mLast : mMid));
            Cell cntCell = row.createCell(7); cntCell.setCellStyle(isFirst ? mCFirst : (isLast ? mCLast : mCMid));
            Cell amtCell = row.createCell(8); amtCell.setCellStyle(isFirst ? mRFirst : (isLast ? mRLast : mRMid));
            if (i == 2) { cntCell.setCellValue(2); amtCell.setCellValue(496_000_000); }
            else { cntCell.setCellFormula(months[i][1]); amtCell.setCellFormula(months[i][2]); }
        }
    }

    // ══════════════════════════════════════════════
    //  일별접수건수 / 일별실행건수
    // ══════════════════════════════════════════════

    private void createDailySheet(XSSFWorkbook wb, String sheetName, String refCol,
                                   short fmtDate, short fmtCount, short fmtAmount) {
        XSSFSheet ws = wb.createSheet(sheetName);

        ws.setColumnWidth(0, 4 * 256);
        ws.setColumnWidth(1, (int)(13.0 * 256));  // B열: 날짜 (yyyy-mm-dd)
        ws.setColumnWidth(2, 10 * 256);
        ws.setColumnWidth(3, 16 * 256);
        ws.setColumnWidth(4, 12 * 256);
        ws.setColumnWidth(5, 18 * 256);

        XSSFCellStyle hdr    = makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFD9E1F2", true,  null);
        XSSFCellStyle dateS  = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false, fmtDate);
        XSSFCellStyle countS = make(wb, HorizontalAlignment.RIGHT,  THIN, THIN, THIN, THIN, null, false, fmtCount);
        XSSFCellStyle amtS   = make(wb, HorizontalAlignment.RIGHT,  THIN, THIN, THIN, THIN, null, false, fmtAmount);
        XSSFCellStyle sumS   = makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFFFFF00", true, null);
        XSSFCellStyle sumCnt = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFFFFF00", true, fmtCount);
        XSSFCellStyle sumAmt = make(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFFFFF00", true, fmtAmount);

        // 타이틀
        Row titleRow = ws.createRow(0);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 1, 5));
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(sheetName);
        titleCell.setCellStyle(makeStr(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "FF92D050", true, null));

        // 헤더 (row5, index 4)
        Row headerRow = ws.createRow(4);
        String[] cols = {"", "접수일자", "일계 건수", "일계 금 액", "월별 누적계 건수", "월별 누적계 금 액"};
        for (int i = 0; i < cols.length; i++) {
            Cell c = headerRow.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hdr);
        }

        // 월별 데이터 (index 5~157)
        int[][] monthDays = {{4, 30}, {5, 31}, {6, 30}, {7, 31}, {8, 31}};
        int dataIdx = 5;
        for (int[] md : monthDays) {
            int month = md[0], days = md[1];
            for (int day = 1; day <= days; day++) {
                int idx = dataIdx++;
                int rowNum = idx + 1;
                Row row = ws.createRow(idx);

                Cell dateCell = row.createCell(1);
                try { dateCell.setCellValue(java.sql.Date.valueOf(LocalDate.of(2025, month, day))); }
                catch (Exception e) { dateCell.setCellValue(""); }
                dateCell.setCellStyle(dateS);

                formula(row, 2, "IFERROR(COUNTIF(접수일!$" + refCol + "$5:$" + refCol + "$860,B" + rowNum + "),0)", countS);
                formula(row, 3, "IFERROR(SUMIF(접수일!$" + refCol + "$5:$" + refCol + "$860,B" + rowNum + ",접수일!$M$5:$M$860),0)", amtS);

                if (day == 1) {
                    formula(row, 4, "C" + rowNum, countS);
                    formula(row, 5, "D" + rowNum, amtS);
                } else {
                    formula(row, 4, "E" + (rowNum - 1) + "+C" + rowNum, countS);
                    formula(row, 5, "F" + (rowNum - 1) + "+D" + rowNum, amtS);
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    //  상환예정내역표 (VLOOKUP)
    // ══════════════════════════════════════════════

    private void createRepaymentSheet(XSSFWorkbook wb, DataFormat fmt) {
        XSSFSheet ws = wb.createSheet("상환예정내역표");
        int[] colWidths = {6, 12, 20, 16, 14, 14, 12, 12, 14, 14};
        for (int i = 0; i < colWidths.length; i++) ws.setColumnWidth(i, colWidths[i] * 256);

        short fmtNum  = fmt.getFormat("#,##0");
        short fmtDate = fmt.getFormat("m\"월\" d\"일\";@");
        XSSFCellStyle hdr   = makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFD9E1F2", true, null);
        XSSFCellStyle ctr   = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null,       false, (short)0);
        XSSFCellStyle num   = make(wb, HorizontalAlignment.RIGHT,  THIN, THIN, THIN, THIN, null,       false, fmtNum);
        XSSFCellStyle dateS = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null,       false, fmtDate);
        XSSFCellStyle inp   = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "FFFFFF00", false, (short)0);

        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));
        Row t = ws.createRow(0); t.setHeightInPoints(28f);
        Cell tc = t.createCell(0); tc.setCellValue("상환예정내역표");
        tc.setCellStyle(makeStr(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "FF92D050", true, null));

        ws.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));
        Row info = ws.createRow(1);
        info.createCell(0).setCellValue("※ 성명 입력 시 대출 정보가 자동 조회됩니다. (접수일 시트 기준)");

        Row header = ws.createRow(3); header.setHeightInPoints(36f);
        String[] heads = {"순번", "성명", "주민등록번호", "연락처", "실행일", "대출신청금", "상품", "상환방식", "기간", "거치"};
        for (int i = 0; i < heads.length; i++) {
            Cell c = header.createCell(i); c.setCellValue(heads[i]); c.setCellStyle(hdr);
        }

        for (int i = 0; i < 50; i++) {
            int idx = 4 + i; int rowNum = idx + 1;
            Row row = ws.createRow(idx); row.setHeightInPoints(22f);
            Cell seqCell = row.createCell(0); seqCell.setCellValue(i + 1); seqCell.setCellStyle(ctr);
            row.createCell(1).setCellStyle(inp);
            String lkp = "B" + rowNum + ",접수일!$G$5:$T$860,";
            formula(row, 2, "IFERROR(VLOOKUP(" + lkp + "2,0),\"\")",  ctr);
            formula(row, 3, "IFERROR(VLOOKUP(" + lkp + "5,0),\"\")",  ctr);
            formula(row, 4, "IFERROR(VLOOKUP(" + lkp + "6,0),\"\")",  dateS);
            formula(row, 5, "IFERROR(VLOOKUP(" + lkp + "7,0),\"\")",  num);
            formula(row, 6, "IFERROR(VLOOKUP(" + lkp + "11,0),\"\")", ctr);
            formula(row, 7, "IFERROR(VLOOKUP(" + lkp + "12,0),\"\")", ctr);
            formula(row, 8, "IFERROR(VLOOKUP(" + lkp + "13,0),\"\")", ctr);
            formula(row, 9, "IFERROR(VLOOKUP(" + lkp + "14,0),\"\")", ctr);
        }
    }

    // ══════════════════════════════════════════════
    //  잔금조회 (동적 은행명/단지명/담당자)
    // ══════════════════════════════════════════════

    private void createBalanceInquirySheet(XSSFWorkbook wb,
                                            String bankName, String complexFullName,
                                            String bankManager, String bankPhone, String bankFax,
                                            DataFormat fmt, short fmtMonthly, short fmtToday, short fmtAmount) {
        XSSFSheet ws = wb.createSheet("잔금조회");

        int[] widths = {6, 14, 18, 14, 14, 16, 12, 12, 16};
        for (int i = 0; i < widths.length; i++) ws.setColumnWidth(i, widths[i] * 256);

        short fmtNum = fmt.getFormat("#,##0");
        XSSFCellStyle hdr   = makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFD9E1F2", true,  null);
        XSSFCellStyle ctr   = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false, (short)0);
        XSSFCellStyle lft   = make(wb, HorizontalAlignment.LEFT,   THIN, THIN, THIN, THIN, null, false, (short)0);
        XSSFCellStyle num   = make(wb, HorizontalAlignment.RIGHT,  THIN, THIN, THIN, THIN, null, false, fmtAmount);
        XSSFCellStyle inp   = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "FFFFFF00", false, (short)0);
        XSSFCellStyle dateS = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false, fmtMonthly);

        // row1: A1=은행명, D1=단지명+제목
        Row r1 = ws.createRow(0); r1.setHeightInPoints(28f);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        Cell bankCell = r1.createCell(0);
        bankCell.setCellValue(bankName);
        bankCell.setCellStyle(makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFD9E1F2", true, null));

        ws.addMergedRegion(new CellRangeAddress(0, 0, 3, 8));
        Cell titleCell = r1.createCell(3);
        titleCell.setCellValue(complexFullName + " 잔금조회 요청서");
        titleCell.setCellStyle(makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, THIN, MEDIUM, "FF92D050", true, null));

        // row2: C2=TODAY(), G2=담당자, H2=HP, J2=FAX
        Row r2 = ws.createRow(1); r2.setHeightInPoints(22f);
        Cell todayCell = r2.createCell(2); todayCell.setCellFormula("TODAY()");
        XSSFCellStyle todayStyle = wb.createCellStyle(); todayStyle.setDataFormat(fmtToday);
        todayCell.setCellStyle(todayStyle);
        r2.createCell(6).setCellValue(bankManager);
        r2.createCell(7).setCellValue("HP : " + bankPhone);
        ws.addMergedRegion(new CellRangeAddress(1, 1, 7, 8));

        // row3: 헤더
        Row r3 = ws.createRow(2); r3.setHeightInPoints(36f);
        String[] heads = {"순번", "고객명", "주민등록번호", "동", "호수", "연락처", "대출일자", "후불이자", "잔금"};
        for (int i = 0; i < heads.length; i++) {
            Cell c = r3.createCell(i); c.setCellValue(heads[i]); c.setCellStyle(hdr);
        }

        // row4~: 데이터 (VLOOKUP)
        for (int i = 0; i < 50; i++) {
            int idx = 3 + i; int rowNum = idx + 1;
            Row row = ws.createRow(idx); row.setHeightInPoints(22f);
            Cell seqC = row.createCell(0); seqC.setCellValue(i + 1); seqC.setCellStyle(ctr);
            row.createCell(1).setCellStyle(inp);
            String lkp = "B" + rowNum + ",접수일!$G$5:$J$860,";
            formula(row, 2, "IFERROR(VLOOKUP(" + lkp + "2,0),\"\")", ctr);
            formula(row, 3, "IFERROR(VLOOKUP(" + lkp + "3,0),\"\")", ctr);
            formula(row, 4, "IFERROR(VLOOKUP(" + lkp + "4,0),\"\")", ctr);
            // 연락처 (K열 = offset 5 from G)
            formula(row, 5, "IFERROR(VLOOKUP(B" + rowNum + ",접수일!$G$5:$K$860,5,0),\"\")", ctr);
            // 대출일자 = 실행일
            formula(row, 6, "IFERROR(VLOOKUP(B" + rowNum + ",접수일!$G$5:$L$860,6,0),\"\")", dateS);
            row.createCell(7).setCellStyle(num);
            row.createCell(8).setCellStyle(num);
        }
    }

    // ══════════════════════════════════════════════
    //  중도금조회 (동적 은행명/단지명/담당자)
    // ══════════════════════════════════════════════

    private void createProgressPaymentSheet(XSSFWorkbook wb,
                                             String bankName, String complexFullName,
                                             String bankManager, String bankPhone, String bankFax,
                                             DataFormat fmt, short fmtMonthly, short fmtToday, short fmtAmount) {
        XSSFSheet ws = wb.createSheet("중도금조회");

        int[] widths = {6, 14, 18, 14, 14, 16, 16, 14, 16, 12};
        for (int i = 0; i < widths.length; i++) ws.setColumnWidth(i, widths[i] * 256);

        short fmtNum = fmt.getFormat("#,##0");
        XSSFCellStyle hdr   = makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFD9E1F2", true,  null);
        XSSFCellStyle ctr   = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false, (short)0);
        XSSFCellStyle num   = make(wb, HorizontalAlignment.RIGHT,  THIN, THIN, THIN, THIN, null, false, fmtAmount);
        XSSFCellStyle inp   = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, "FFFFFF00", false, (short)0);
        XSSFCellStyle dateS = make(wb, HorizontalAlignment.CENTER, THIN, THIN, THIN, THIN, null, false, fmtMonthly);

        // row1: A1=은행명, D1=단지명+제목
        Row r1 = ws.createRow(0); r1.setHeightInPoints(28f);
        ws.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        Cell bankCell = r1.createCell(0);
        bankCell.setCellValue(bankName);
        bankCell.setCellStyle(makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, MEDIUM, MEDIUM, "FFD9E1F2", true, null));

        ws.addMergedRegion(new CellRangeAddress(0, 0, 3, 9));
        Cell titleCell = r1.createCell(3);
        titleCell.setCellValue(complexFullName + " 중도금조회 요청서");
        titleCell.setCellStyle(makeStr(wb, HorizontalAlignment.CENTER, MEDIUM, MEDIUM, THIN, MEDIUM, "FFF3C6BF", true, null));

        // row2
        Row r2 = ws.createRow(1); r2.setHeightInPoints(22f);
        Cell todayCell = r2.createCell(2); todayCell.setCellFormula("TODAY()");
        XSSFCellStyle todayStyle = wb.createCellStyle(); todayStyle.setDataFormat(fmtToday);
        todayCell.setCellStyle(todayStyle);
        r2.createCell(5).setCellValue(bankManager);
        r2.createCell(6).setCellValue("HP : " + bankPhone);
        r2.createCell(8).setCellValue("FAX : " + bankFax);

        // row3: 헤더
        Row r3 = ws.createRow(2); r3.setHeightInPoints(36f);
        String[] heads = {"순번", "고객명", "주민등록번호", "동", "호수", "연락처", "대출일자", "중도금", "중도금이자", "합계"};
        for (int i = 0; i < heads.length; i++) {
            Cell c = r3.createCell(i); c.setCellValue(heads[i]); c.setCellStyle(hdr);
        }

        // row4~: 데이터
        for (int i = 0; i < 50; i++) {
            int idx = 3 + i; int rowNum = idx + 1;
            Row row = ws.createRow(idx); row.setHeightInPoints(22f);
            Cell seqC = row.createCell(0); seqC.setCellValue(i + 1); seqC.setCellStyle(ctr);
            row.createCell(1).setCellStyle(inp);
            String lkp = "B" + rowNum + ",접수일!$G$5:$J$860,";
            formula(row, 2, "IFERROR(VLOOKUP(" + lkp + "2,0),\"\")", ctr);
            formula(row, 3, "IFERROR(VLOOKUP(" + lkp + "3,0),\"\")", ctr);
            formula(row, 4, "IFERROR(VLOOKUP(" + lkp + "4,0),\"\")", ctr);
            formula(row, 5, "IFERROR(VLOOKUP(B" + rowNum + ",접수일!$G$5:$K$860,5,0),\"\")", ctr);
            formula(row, 6, "IFERROR(VLOOKUP(B" + rowNum + ",접수일!$G$5:$L$860,6,0),\"\")", dateS);
            row.createCell(7).setCellStyle(num);
            row.createCell(8).setCellStyle(num);
            formula(row, 9, "H" + rowNum + "+I" + rowNum, num);
        }
    }

    // ══════════════════════════════════════════════
    //  셀 헬퍼
    // ══════════════════════════════════════════════

    private void setCell(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val : "");
        cell.setCellStyle(style);
    }

    private void setDateCell(Row row, int col, LocalDate d, CellStyle dateStyle, CellStyle empty) {
        Cell cell = row.createCell(col);
        if (d != null) { cell.setCellValue(java.sql.Date.valueOf(d)); cell.setCellStyle(dateStyle); }
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
