package com.hackathon.service;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Round;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.enums.FileType;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.RoundRepository;
import com.hackathon.service.submission.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {
    private final RoundRepository roundRepository;
    private final CloudinaryService cloudinaryService;

    public String exportRankingToExcel(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi."));
        // Khởi tạo workBook excels
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Tạo tiêu đề
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            // hêm màu nền cho Header
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
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
                    row.createCell(0).setCellValue(stt++);
                    row.createCell(1).setCellValue(tp.getRegistration().getTeam().getTeamName());
                    row.createCell(2).setCellValue(  tp.getTotalScore() != null ? tp.getTotalScore().doubleValue() : 0);
                    row.createCell(3).setCellValue(tp.getRank());
                    row.createCell(4).setCellValue(tp.getStatus().name());

                }

                // Chỉnh độ rộng cọt theo đồ dài của chữ
                for (int i = 0; i < colums.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                sheet.protectSheet("BTC_Hackathon_Secret_Password_2026");

            }
            workbook.write(out);
           byte[] excelBytes = out.toByteArray();
           String fileName ="ranking_round_"+roundId+"_"+ System.currentTimeMillis()+".xlsx";
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excelBytes
            );
            Path path = Paths.get(System.getProperty("user.home"), "Desktop", "ranking_test.xlsx");
            System.out.println("Saving to: " + path.toAbsolutePath());

            Files.write(path, excelBytes);
            System.out.println("Saved successfully");
            return cloudinaryService.uploadExcelFile(multipartFile);
        } catch (IOException e) {
            log.error("Lỗi chi tiết trong quá trình tạo/ghi file Excel cho vòng {}: ", roundId, e);
            throw new RuntimeException("Lỗi trong quá trình tạo file Excel");
        }
    }

}
