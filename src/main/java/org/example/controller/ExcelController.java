package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.bean.dto.CommentReportDto;
import org.example.bean.dto.MediaCommentDetailDto;
import org.example.entity.IgUser;
import org.example.exception.ApiException;
import org.example.exception.SysCode;
import org.example.service.IgUserService;
import org.example.service.MediaCommentService;
import org.example.service.MediaService;
import org.example.utils.ExcelUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Excel controller", description = "開發導出Excel用API")
@RestController
@RequestMapping("excel")
public class ExcelController extends BaseController {
    private final IgUserService igUserService;
    private final MediaCommentService mediaCommentService;
    private final MediaService mediaService;

    public ExcelController(IgUserService igUserService, MediaCommentService mediaCommentService, MediaService mediaService) {
        this.igUserService = igUserService;
        this.mediaCommentService = mediaCommentService;
        this.mediaService = mediaService;
    }

    @GetMapping(value = "/exportComment/{igUserName}")
    @Operation(summary = "倒出user comment excel", description = "倒出user comment excel")
    public void export(HttpServletResponse response, @PathVariable String igUserName) {
        IgUser igUser = igUserService.findUserByIgUserName(igUserName)
                .orElseThrow(() -> new ApiException(SysCode.IG_USER_NOT_FOUND));
        Map<String, Long> hashTagMap = mediaService.analyzeHashtagsAndSort(igUser);
        // 創建Excel工作簿和工作表
        Workbook workbook = new XSSFWorkbook();
        // 設置第一個工作表
        setSheetFirst(workbook, igUser, hashTagMap);
        // 設置第二個工作表
        setSheetSecond(workbook, igUser);
        // 設置第三個工作表
        setSheetThird(workbook, igUser);

        // 設置響應頭部，告訴瀏覽器這是一個需要下載的檔案
        String fileName = URLEncoder.encode(igUser.getUserName(), StandardCharsets.UTF_8) + ".xlsx";
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            // 將工作簿寫入HTTP響應流
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (Exception e) {
            log.error("export excel error", e);
            throw new ApiException(SysCode.EXCEL_OUTPUT_FAILED);
        }
    }


    //private

    private void setSheetFirst(Workbook workbook, IgUser igUser, Map<String, Long> hashTagMap) {
        Sheet sheet = workbook.createSheet("目錄Index");
        // 填充數據到工作表
        fillUserData(sheet, igUser, hashTagMap);
        // 設置合併單元格和樣式
        setCellStylesAndMergeCellsForCommentIndex(sheet);
    }

    private void setSheetSecond(Workbook workbook, IgUser igUser) {
        Sheet sheet = workbook.createSheet("統計資料CountData");
        List<CommentReportDto> commentIntegration = mediaCommentService.findCommentSummary(igUser);
        // 填充數據到工作表
        fillUserData(sheet, commentIntegration);
        // 設單元格樣式
        setCellStylesAndMergeCellsForCommentIntegration(sheet);
    }

    private void setSheetThird(Workbook workbook, IgUser igUser) {
        Sheet sheet = workbook.createSheet("明細資料DetailData");
        List<MediaCommentDetailDto> commentDetail = mediaCommentService.findCommentDetail(igUser);
        // 填充數據到工作表
        fillDetailData(sheet, commentDetail);
        // 設單元格樣式
        setCellStylesForCommentDetail(sheet);
    }

    private void fillUserData(Sheet sheet, IgUser igUser, Map<String, Long> hashTagMap) {
        // 創建表頭行
        Row headerRow = sheet.createRow(0);

        // 創建表頭單元格
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Easy Insta"); // 設定你的表頭內容

        // 合併A0和B0
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // 設定行資料
        int rowNum = 1; // 從第二行開始，因為第一行是表頭
        // 範例的用戶資料，依您的IgUser實體的屬性進行調整
        String[] headers = {"目標帳號", "用戶全名", "貼文數量", "粉絲數", "生產日期", "常用HashTag"};
        String[] data = {
                igUser.getUserName(),
                igUser.getFullName(),
                String.valueOf(igUser.getMediaCount()),
                String.valueOf(igUser.getFollowerCount()),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                hashTagMap.toString()
        };

        // 創建並填充資料行
        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue(headers[i]);
            cell = row.createCell(1);
            cell.setCellValue(data[i]);
        }
    }

    private void fillUserData(Sheet sheet, List<CommentReportDto> commentIntegration) {
        Row commentHeaderRow = sheet.createRow(0); // 評論報告從第四行開始
        String[] commentHeaders = {"帳號", "留言次數", "留言被按讚數"};
        for (int i = 0; i < commentHeaders.length; i++) {
            Cell headerCell = commentHeaderRow.createCell(i);
            headerCell.setCellValue(commentHeaders[i]);
        }

        // 填充評論整合數據
        int rowNum = 1;
        for (CommentReportDto comment : commentIntegration) {
            log.info("comment: {}", comment);
            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue(comment.getUserName());

            cell = row.createCell(1);
            cell.setCellValue(comment.getCommentCount());

            cell = row.createCell(2);
            cell.setCellValue(comment.getLikeCount());
        }
    }

    private void fillDetailData(Sheet sheet, List<MediaCommentDetailDto> commentDetail) {
        Row commentHeaderRow = sheet.createRow(0);
        String[] commentHeaders = {"留言貼文", "留言文章id", "留言帳號", "帳號全名", "留言內容", "公開帳號", "Meta驗證", "當下是否有發限動", "留言被按讚數"};
        for (int i = 0; i < commentHeaders.length; i++) {
            Cell headerCell = commentHeaderRow.createCell(i);
            headerCell.setCellValue(commentHeaders[i]);
        }

        // 填充評論數據
        int rowNum = 1;
        for (MediaCommentDetailDto comment : commentDetail) {
            List<Object> values = List.of(
                    comment.getMessage() != null ? comment.getMessage() : "N/A",
                    comment.getMediaPk().toString(),
                    comment.getCommenterUserName(),
                    comment.getCommenterFullName() != null ? comment.getCommenterFullName() : "N/A",
                    comment.getCommentText() != null ? comment.getCommentText() : "N/A",
                    Boolean.TRUE.equals(comment.getIsPrivateAccount()) ? "是" : "否",
                    Boolean.TRUE.equals(comment.getIsVerifiedAccount()) ? "是" : "否",
                    comment.getLatestReelMedia() != null ? "是" : "否",
                    comment.getLikeCount().toString()
            );
            ExcelUtils.createRowAndFillData(sheet, rowNum++, values);
        }
    }


    private void setCellStylesAndMergeCellsForCommentIndex(Sheet sheet) {
        // 取得工作簿
        Workbook workbook = sheet.getWorkbook();
        // 取得自訂顏色
        XSSFColor customColor = ExcelUtils.getCustomColor((byte) 133, (byte) 223, (byte) 255);

        // 建立表頭樣式和儲存格樣式
        CellStyle headerStyle = ExcelUtils.createHeaderCellStyle(workbook, customColor, (short) 24);
        CellStyle cellStyle = ExcelUtils.createCellStyle(workbook, customColor, (short) 15, HorizontalAlignment.RIGHT);
        // 應用程式樣式到工作表
        ExcelUtils.applyStylesToSheet(sheet, headerStyle, cellStyle);
        // 調整列寬
        ExcelUtils.setAutoColumnWidth(sheet);
        ExcelUtils.setAutoColumnWidthForChinese(sheet);
    }

    private void setCellStylesAndMergeCellsForCommentIntegration(Sheet sheet) {
        // 取得工作簿
        Workbook workbook = sheet.getWorkbook();
        // 取得自訂顏色
        XSSFColor customColor = ExcelUtils.getCustomColor((byte) 241, (byte) 169, (byte) 131);
        XSSFColor whiteColor = ExcelUtils.getCustomColor((byte) 255, (byte) 255, (byte) 255);

        // 建立表頭樣式和儲存格樣式
        CellStyle headerStyle = ExcelUtils.createHeaderCellStyle(workbook, customColor, (short) 15);
        CellStyle cellStyle = ExcelUtils.createCellStyle(workbook, whiteColor, (short) 13, HorizontalAlignment.LEFT);
        // 應用程式樣式到工作表
        ExcelUtils.applyStylesToSheet(sheet, headerStyle, cellStyle);

        // 調整列寬
        Map<Integer, Integer> columnWidths = Map.of(
                0, 40,
                1, 60,
                2, 60
        );
        ExcelUtils.setCustomColumnWidth(sheet, columnWidths);
    }

    private void setCellStylesForCommentDetail(Sheet sheet) {
        // 取得工作簿
        Workbook workbook = sheet.getWorkbook();
        // 取得自訂顏色
        XSSFColor customColor = ExcelUtils.getCustomColor((byte) 241, (byte) 169, (byte) 131);
        XSSFColor whiteColor = ExcelUtils.getCustomColor((byte) 255, (byte) 255, (byte) 255);

        // 建立表頭樣式和儲存格樣式
        CellStyle headerStyle = ExcelUtils.createHeaderCellStyle(workbook, customColor, (short) 15);
        CellStyle cellStyle = ExcelUtils.createCellStyle(workbook, whiteColor, (short) 13, HorizontalAlignment.LEFT);
        // 應用程式樣式到工作表
        ExcelUtils.applyStylesToSheet(sheet, headerStyle, cellStyle);

        // 調整列寬
        Map<Integer, Integer> columnWidths = Map.of(
                0, 40,
                1, 30,
                2, 40,
                3, 40,
                4, 30,
                5, 16,
                6, 16,
                7, 30,
                8, 20
        );
        ExcelUtils.setCustomColumnWidth(sheet, columnWidths);
    }
}