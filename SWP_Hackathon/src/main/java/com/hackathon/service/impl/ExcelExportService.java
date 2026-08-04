package com.hackathon.service.impl;

import com.hackathon.entity.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.RoundRepository;
import com.hackathon.service.submission.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {
    private final RoundRepository roundRepository;
    private final CloudinaryService cloudinaryService;

    public String exportRankingToExcel(Integer roundId, String fileType) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi."));
        String eventName = round.getHackathonEvent().getEventName().replaceAll("\\s+", "_");

        // Khởi tạo workBook excels  xử lý file .xlsx
        try (Workbook workbook = new XSSFWorkbook();
             // ghi dữ liệu vào mảng byte trong bộ nhớ RAM
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Tạo tiêu đề
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // xanh đậm
            byte[] headerBlue = new byte[]{(byte) 79, (byte) 129, (byte) 189}; // #4F81BD
            XSSFColor headerColor = new XSSFColor(headerBlue, null);

            // xanh nhạt
            byte[] zebraBlue = new byte[]{(byte) 233, (byte) 241, (byte) 247}; // #E9F1F7
            XSSFColor zebraColor = new XSSFColor(zebraBlue, null);

            // hêm màu nền cho Header
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(headerColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setCellBorders(headerStyle, BorderStyle.THIN, IndexedColors.GREY_80_PERCENT.getIndex()); // Vẽ viền cho header

            // Style cho dữ liệu
            XSSFCellStyle dataStyle = (XSSFCellStyle) workbook.createCellStyle();
            dataStyle.setFillForegroundColor(zebraColor);
            dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setCellBorders(dataStyle, BorderStyle.THIN, IndexedColors.GREY_80_PERCENT.getIndex()); // Vẽ viền cho header


            // Duyệt categoryRound để tạo thành từng dòng riêng biệt
            for (CategoryRound cr : round.getCategoryRounds()) {
                String name = cr.getCategory() != null ? cr.getCategory().getCategoryName() : "Hạng mục " + cr.getCategoryRoundId();
                String sheetName = WorkbookUtil.createSafeSheetName(name);
                XSSFSheet sheet = (XSSFSheet) workbook.createSheet(sheetName);

                String[] columns = {"STT", "Tên Đội Thi", "Tổng điểm", "Hạng", "Trạng thái", "Giải thưởng"};
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Lấy dữ liệu từ các đội thi
                List<TeamParticipant> participants = cr.getTeamParticipants().stream()
                        .sorted(Comparator.comparing(TeamParticipant::getRank, Comparator.nullsLast(Integer::compareTo)))
                        .toList();
                int rowIndex = 1;
                int stt = 1;
                for (TeamParticipant tp : participants) {
                    Row row = sheet.createRow(rowIndex++);
                    // chọn màu xen kẽ
//                    XSSFCellStyle currentStyle =
//                            rowIndex % 2 == 0 ? headerStyle : dataStyle;

                    createCellWithStyle(row, 0, stt++, dataStyle);
                    createCellWithStyle(row, 1, tp.getRegistration().getTeam().getTeamName(), dataStyle);
                    createCellWithStyle(row, 2, tp.getTotalScore() != null ? tp.getTotalScore().doubleValue() : 0, dataStyle);
                    createCellWithStyle(row, 3, tp.getRank() != null ? tp.getRank() : "-", dataStyle);
                    createCellWithStyle(row, 4, tp.getStatus() != null ? tp.getStatus().name() : "N/A", dataStyle);
                    createCellWithStyle(row, 5, tp.getTitleAward() != null ? tp.getTitleAward() : "N/A", dataStyle);
                }

                // Chỉnh độ rộng côt theo đồ dài của chữ
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                sheet.protectSheet("BTC_Hackathon_Secret_Password_2026");

            }
            workbook.write(out);
            byte[] excelBytes = out.toByteArray();
            String fileName = eventName + " Ranking_Round_" + round.getRoundName() + "_" + fileType.toUpperCase() + "_" + System.currentTimeMillis() + ".xlsx";
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excelBytes
            );

            return cloudinaryService.uploadExcelFile(multipartFile);
        } catch (IOException e) {
            log.error("Lỗi chi tiết trong quá trình tạo/ghi file Excel cho vòng {}: ", roundId, e);
            throw new RuntimeException("Lỗi trong quá trình tạo file Excel");
        }
    }

    // tạo ô + dữ liệu +format
    private void createCellWithStyle(Row row, int columnCount, Object value, CellStyle style) {
        Cell cell = row.createCell(columnCount);
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }

    // đường viền
    private void setCellBorders(CellStyle style, BorderStyle borderStyle, short colorIndex) {
        style.setBorderTop(borderStyle);
        style.setTopBorderColor(colorIndex);
        style.setBorderBottom(borderStyle);
        style.setBottomBorderColor(colorIndex);
        style.setBorderLeft(borderStyle);
        style.setLeftBorderColor(colorIndex);
        style.setBorderRight(borderStyle);
        style.setRightBorderColor(colorIndex);
    }


}
