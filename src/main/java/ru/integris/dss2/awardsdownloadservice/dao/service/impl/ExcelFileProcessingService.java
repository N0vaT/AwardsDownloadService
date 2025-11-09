package ru.integris.dss2.awardsdownloadservice.dao.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.integris.dss2.awardsdownloadservice.dao.service.FileProcessingService;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelFileProcessingService implements FileProcessingService {
    
    private static final String EXCEL_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String[] EXCEL_EXTENSIONS = {".xlsx", ".xls"};
    
    private final RewardServiceImpl rewardService;
    
    public ExcelFileProcessingService(RewardServiceImpl rewardService) {
        this.rewardService = rewardService;
    }
    
    @Override
    public boolean supportsFileType(String contentType, String filename) {
        if (contentType != null && contentType.equals(EXCEL_TYPE)) {
            return true;
        }
        if (filename != null) {
            for (String extension : EXCEL_EXTENSIONS) {
                if (filename.toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public List<RewardUploadDto> processFile(MultipartFile file) throws Exception {
        List<RewardUploadDto> rewardDtos = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                RewardUploadDto dto = parseRow(row);
                if (dto != null) {
                    rewardDtos.add(dto);
                }
            }
            
            workbook.close();
        }
        
        return rewardDtos;
    }
    
    private RewardUploadDto parseRow(Row row) {
        try {
            Long employeeId = getLongCellValue(row.getCell(0));
            String employeeFullName = getStringCellValue(row.getCell(1));
            Long rewardId = getLongCellValue(row.getCell(2));
            String rewardName = getStringCellValue(row.getCell(3));
            LocalDate receiptDate = getDateCellValue(row.getCell(4));
            
            if (employeeId == null || employeeFullName == null || rewardId == null || 
                rewardName == null || receiptDate == null) {
                return null;
            }
            
            return new RewardUploadDto(employeeId, employeeFullName, rewardId, rewardName, receiptDate);
        } catch (Exception e) {
            return null;
        }
    }
    
    private Long getLongCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }
    
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }
    
    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                    } else {
                        return LocalDate.parse(getStringCellValue(cell));
                    }
                case STRING:
                    return LocalDate.parse(cell.getStringCellValue().trim());
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public UploadResponse processRewards(List<RewardUploadDto> rewardDtos) {
        return rewardService.processRewards(rewardDtos);
    }
}