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
// Xuất bảng xếp hạng của vòng thi thành tệp Excel và tải tệp lên nơi lưu trữ.
public class ExcelExportService {
    private final RoundRepository roundRepository;
    private final CloudinaryService cloudinaryService;

    // Tạo tệp Excel bảng xếp hạng của vòng và trả về đường dẫn sau khi tải lên.
    public String exportRankingToExcel(Integer roundId, String fileType) {
        // Tìm vòng thi chứa dữ liệu xếp hạng cần xuất.
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi."));
        // Chuẩn hóa tên sự kiện để dùng an toàn trong tên tệp.
        String eventName = round.getHackathonEvent().getEventName().replaceAll("\\s+", "_");

        // Tạo sổ làm việc và vùng nhớ nhận dữ liệu tệp, cả hai sẽ tự đóng sau khi hoàn tất.
        try (Workbook workbook = new XSSFWorkbook();
             // Ghi tệp vào bộ nhớ để có thể tải trực tiếp mà không tạo tệp tạm trên ổ đĩa.
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Tạo kiểu hiển thị dùng chung cho hàng tiêu đề của từng trang tính.
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            // Tạo phông chữ đậm màu trắng để nổi bật trên nền tiêu đề.
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            // Khai báo màu xanh đậm dùng làm nền tiêu đề.
            byte[] headerBlue = new byte[]{(byte) 79, (byte) 129, (byte) 189}; // #4F81BD
            XSSFColor headerColor = new XSSFColor(headerBlue, null);

            // Khai báo màu xanh nhạt dùng làm nền các dòng dữ liệu.
            byte[] zebraBlue = new byte[]{(byte) 233, (byte) 241, (byte) 247}; // #E9F1F7
            XSSFColor zebraColor = new XSSFColor(zebraBlue, null);

            // Hoàn thiện phông chữ, màu nền và đường viền của tiêu đề.
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(headerColor);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setCellBorders(headerStyle, BorderStyle.THIN, IndexedColors.GREY_80_PERCENT.getIndex()); // Vẽ viền cho header

            // Tạo kiểu hiển thị dùng chung cho các ô dữ liệu.
            XSSFCellStyle dataStyle = (XSSFCellStyle) workbook.createCellStyle();
            dataStyle.setFillForegroundColor(zebraColor);
            dataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setCellBorders(dataStyle, BorderStyle.THIN, IndexedColors.GREY_80_PERCENT.getIndex()); // Vẽ viền cho header


            // Mỗi danh mục của vòng được xuất thành một trang tính riêng.
            for (CategoryRound cr : round.getCategoryRounds()) {
                // Ưu tiên tên danh mục, nếu thiếu thì dùng mã danh mục vòng làm tên thay thế.
                String name = cr.getCategory() != null ? cr.getCategory().getCategoryName() : "Hạng mục " + cr.getCategoryRoundId();
                // Làm sạch các ký tự không được Excel cho phép trong tên trang tính.
                String sheetName = WorkbookUtil.createSafeSheetName(name);
                XSSFSheet sheet = (XSSFSheet) workbook.createSheet(sheetName);

                // Khai báo thứ tự các cột xuất hiện trong bảng xếp hạng.
                String[] columns = {"STT", "Tên Đội Thi", "Tổng điểm", "Hạng", "Trạng thái", "Giải thưởng"};
                // Tạo hàng đầu tiên để chứa tiêu đề cột.
                Row headerRow = sheet.createRow(0);
                // Ghi tên và áp dụng kiểu tiêu đề cho từng cột.
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Sắp xếp đội theo thứ hạng, đưa đội chưa có hạng xuống cuối.
                List<TeamParticipant> participants = cr.getTeamParticipants().stream()
                        .sorted(Comparator.comparing(TeamParticipant::getRank, Comparator.nullsLast(Integer::compareTo)))
                        .toList();
                // Dữ liệu bắt đầu từ hàng thứ hai vì hàng đầu đã dành cho tiêu đề.
                int rowIndex = 1;
                // Số thứ tự hiển thị bắt đầu từ một.
                int stt = 1;
                // Ghi một hàng dữ liệu cho từng đội trong danh mục.
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

                // Tự điều chỉnh độ rộng từng cột theo nội dung đã ghi.
                for (int i = 0; i < columns.length; i++) {
                    sheet.autoSizeColumn(i);
                }
                // Khóa trang tính để hạn chế chỉnh sửa nhầm bảng xếp hạng đã công bố.
                sheet.protectSheet("BTC_Hackathon_Secret_Password_2026");

            }
            // Ghi toàn bộ sổ làm việc vào vùng nhớ.
            workbook.write(out);
            // Lấy mảng byte hoàn chỉnh của tệp Excel.
            byte[] excelBytes = out.toByteArray();
            // Tạo tên tệp duy nhất gồm sự kiện, vòng, loại kết quả và thời gian xuất.
            String fileName = eventName + " Ranking_Round_" + round.getRoundName() + "_" + fileType.toUpperCase() + "_" + System.currentTimeMillis() + ".xlsx";
            // Bọc mảng byte thành tệp tải lên với đúng loại nội dung của Excel.
            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    excelBytes
            );

            // Tải tệp lên nơi lưu trữ và trả về đường dẫn công khai.
            return cloudinaryService.uploadExcelFile(multipartFile);
        } catch (IOException e) {
            // Ghi chi tiết lỗi kỹ thuật nhưng chỉ trả thông báo tổng quát cho phía gọi.
            log.error("Lỗi chi tiết trong quá trình tạo/ghi file Excel cho vòng {}: ", roundId, e);
            throw new RuntimeException("Lỗi trong quá trình tạo file Excel");
        }
    }

    // Tạo một ô, ghi giá trị theo đúng kiểu dữ liệu và áp dụng kiểu hiển thị.
    private void createCellWithStyle(Row row, int columnCount, Object value, CellStyle style) {
        // Tạo ô tại vị trí cột được yêu cầu trong hàng hiện tại.
        Cell cell = row.createCell(columnCount);
        // Chọn hàm ghi phù hợp để Excel giữ đúng kiểu số, luận lý hoặc văn bản.
        if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        }
        // Áp dụng màu nền, phông chữ và đường viền sau khi ghi giá trị.
        cell.setCellStyle(style);
    }

    // Áp dụng cùng kiểu và màu đường viền cho bốn cạnh của ô.
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
