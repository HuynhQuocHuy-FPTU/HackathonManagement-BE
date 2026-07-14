package com.hackathon.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.enums.RoundStatus;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {
    private final RoundRepository roundRepository;
    private final CloudinaryService cloudinaryService;

    public String exportRankingToExcel(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi."));
        String eventName = round.getHackathonEvent().getEventName().replaceAll("\\s+", "_");

        // Khởi tạo workBook excels
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Tạo tiêu đề
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);

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

                // Tạo row tiêu đề
                Row headerRow = sheet.createRow(0);
                String[] colums = {"STT", "Tên Đội Thi", "Tổng điểm", "Hạng", "Trạng thái"};
                for (int i = 0; i < colums.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(colums[i]);
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
                    createCellWithStyle(row, 0, stt++, dataStyle);
                    createCellWithStyle(row, 1, tp.getRegistration().getTeam().getTeamName(), dataStyle);
                    createCellWithStyle(row, 2, tp.getTotalScore() != null ? tp.getTotalScore().doubleValue() : 0, dataStyle);
                    createCellWithStyle(row, 3, tp.getRank() != null ? tp.getRank() : "-", dataStyle);
                    createCellWithStyle(row, 4, tp.getStatus().name(), dataStyle);
                }

                // Chỉnh độ rộng cọt theo đồ dài của chữ
                for (int i = 0; i < colums.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                sheet.protectSheet("BTC_Hackathon_Secret_Password_2026");

            }
            workbook.write(out);
            byte[] excelBytes = out.toByteArray();
            String rankingType;

            if (round.getStatus() == RoundStatus.DRAFT_APPROVED) {
                rankingType = "Draft";
            } else if (round.getStatus() == RoundStatus.COMPLETED) {
                rankingType = "Final";
            } else {
                throw new BadRequestException(
                        "Kết quả vòng thi chưa được duyệt hoặc chưa công bố, không thể xuất file"
                );
            }
            String fileName = eventName + " Ranking_Round_" + round.getRoundName() + "_" + rankingType + "_" + System.currentTimeMillis() + ".xlsx";
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excelBytes
            );
//            Path path = Paths.get(System.getProperty("user.home"), "Desktop", "ranking_test.xlsx");
//            System.out.println("Saving to: " + path.toAbsolutePath());
//
//            Files.write(path, excelBytes);
//            System.out.println("Saved successfully");
            return cloudinaryService.uploadExcelFile(multipartFile);
        } catch (IOException e) {
            log.error("Lỗi chi tiết trong quá trình tạo/ghi file Excel cho vòng {}: ", roundId, e);
            throw new RuntimeException("Lỗi trong quá trình tạo file Excel");
        }
    }

    // tạo ô
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
