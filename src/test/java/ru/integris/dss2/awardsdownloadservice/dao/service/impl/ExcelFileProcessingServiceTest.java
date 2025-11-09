package ru.integris.dss2.awardsdownloadservice.dao.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelFileProcessingServiceTest {

    @Mock
    private RewardServiceImpl rewardService;

    private ExcelFileProcessingService excelFileProcessingService;

    @BeforeEach
    void setUp() {
        excelFileProcessingService = new ExcelFileProcessingService(rewardService);
    }

    // ======= Тесты типов ==========

    @ParameterizedTest
    @ValueSource(strings = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    })
    void supportsFileType_WithValidContentType_ShouldReturnTrue(String contentType) {
        assertTrue(excelFileProcessingService.supportsFileType(contentType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rewards.xlsx", "data.XLSX", "file.Xls", "test.data.xlsx", "UPPERCASE.XLS"})
    void supportsFileType_WithExcelFilename_ShouldReturnTrue(String filename) {
        assertTrue(excelFileProcessingService.supportsFileType(null, filename));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/json", "text/plain", "text/csv",
            "image/png", "application/octet-stream", "text/html"
    })
    void supportsFileType_WithInvalidContentType_ShouldReturnFalse(String contentType) {
        assertFalse(excelFileProcessingService.supportsFileType(contentType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rewards.txt", "data.csv", "file.doc", "test.data",
            "archive.zip", "image.png", "document.pdf"
    })
    void supportsFileType_WithNonExcelFilename_ShouldReturnFalse(String filename) {
        assertFalse(excelFileProcessingService.supportsFileType(null, filename));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void supportsFileType_WithNullOrEmptyFilename_ShouldReturnFalse(String filename) {
        assertFalse(excelFileProcessingService.supportsFileType(null, filename));
    }

    @Test
    void supportsFileType_WithNullContentTypeAndNullFilename_ShouldReturnFalse() {
        assertFalse(excelFileProcessingService.supportsFileType(null, null));
    }

    @Test
    void supportsFileType_WithBothValidContentTypeAndFilename_ShouldReturnTrue() {
        assertTrue(excelFileProcessingService.supportsFileType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "rewards.xlsx"
        ));
    }

    // ========== Тесты для processFile ==========

    @Test
    void processFile_WithValidExcelContent_ShouldReturnListOfDtos() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1L, "Иванов Иван Иванович", 101L, "Лучший сотрудник", LocalDate.of(2024, 1, 15)},
                        {2L, "Петров Петр Петрович", 102L, "Премия года", LocalDate.of(2024, 2, 20)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "rewards.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        RewardUploadDto firstDto = result.get(0);
        assertEquals(1L, firstDto.getEmployeeId());
        assertEquals("Иванов Иван Иванович", firstDto.getEmployeeFullName());
        assertEquals(101L, firstDto.getRewardId());
        assertEquals("Лучший сотрудник", firstDto.getRewardName());
        assertEquals(LocalDate.of(2024, 1, 15), firstDto.getReceiptDate());

        RewardUploadDto secondDto = result.get(1);
        assertEquals(2L, secondDto.getEmployeeId());
        assertEquals("Петров Петр Петрович", secondDto.getEmployeeFullName());
        assertEquals(102L, secondDto.getRewardId());
        assertEquals("Премия года", secondDto.getRewardName());
        assertEquals(LocalDate.of(2024, 2, 20), secondDto.getReceiptDate());
    }

    @Test
    void processFile_WithEmptyRows_ShouldSkipEmptyRows() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1L, "Иванов Иван", 101L, "Награда", LocalDate.of(2024, 1, 15)},
                        {null, null, null, null, null}, // Пустая строка
                        {2L, "Петров Петр", 102L, "Премия", LocalDate.of(2024, 2, 20)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "with_empty_rows.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // Должны быть проигнорированы пустые строки
    }

    @Test
    void processFile_WithOnlyHeader_ShouldReturnEmptyList() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "only_header.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void processFile_WithSingleRecord_ShouldParseCorrectly() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1L, "Иванов Иван", 101L, "Награда", LocalDate.of(2024, 1, 15)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "single.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
        assertEquals("Иванов Иван", result.get(0).getEmployeeFullName());
    }

    @Test
    void processFile_WithLargeExcel_ShouldParseAllRecords() throws Exception {
        // Given
        Object[][] data = new Object[101][5];
        data[0] = new Object[]{"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"};

        for (int i = 1; i <= 100; i++) {
            data[i] = new Object[]{
                    (long) i,
                    "Сотрудник " + i,
                    100L + i,
                    "Награда " + i,
                    LocalDate.of(2024, 1, (i % 28) + 1)
            };
        }

        byte[] excelContent = createTestExcelFile(data);

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(100, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
        assertEquals(100L, result.get(99).getEmployeeId());
        assertEquals("Сотрудник 1", result.get(0).getEmployeeFullName());
        assertEquals("Сотрудник 100", result.get(99).getEmployeeFullName());
    }

    @Test
    void processFile_WithDifferentDataTypes_ShouldParseCorrectly() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFileWithMixedDataTypes(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1.0, "Иванов Иван", 101.0, "Награда", LocalDate.of(2024, 1, 15)}, // Числа как double
                        {"2", "Петров Петр", "102", "Премия", "2024-02-20"} // Числа как строки
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "mixed_types.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
        assertEquals(2L, result.get(1).getEmployeeId());
        assertEquals(101L, result.get(0).getRewardId());
        assertEquals(102L, result.get(1).getRewardId());
    }

    @Test
    void processFile_WithDateAsExcelDate_ShouldParseCorrectly() throws Exception {
        // Given
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Награды");

        // Заголовок
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID сотрудника");
        headerRow.createCell(1).setCellValue("ФИО");
        headerRow.createCell(2).setCellValue("ID награды");
        headerRow.createCell(3).setCellValue("Название награды");
        headerRow.createCell(4).setCellValue("Дата получения");

        // Данные с датами в формате Excel
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue(1);
        dataRow.createCell(1).setCellValue("Иванов Иван");
        dataRow.createCell(2).setCellValue(101);
        dataRow.createCell(3).setCellValue("Награда");

        Cell dateCell = dataRow.createCell(4);
        dateCell.setCellValue(Date.from(
                LocalDate.of(2024, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()
        ));

        // Создаем стиль для даты
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
        dateCell.setCellStyle(dateStyle);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file", "excel_dates.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2024, 1, 15), result.get(0).getReceiptDate());
    }

    @Test
    void processFile_WithIncompleteRecords_ShouldSkipInvalidRows() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1L, "Иванов Иван", 101L, "Награда", LocalDate.of(2024, 1, 15)},
                        {null, "Петров Петр", 102L, "Премия", LocalDate.of(2024, 2, 20)}, // Нет employeeId
                        {3L, null, 103L, "Бонус", LocalDate.of(2024, 3, 25)}, // Нет ФИО
                        {4L, "Сидорова Анна", null, "Приз", LocalDate.of(2024, 4, 30)}, // Нет rewardId
                        {5L, "Козлов Дмитрий", 105L, null, LocalDate.of(2024, 5, 15)}, // Нет названия награды
                        {6L, "Николаева Ольга", 106L, "Премия", null}, // Нет даты
                        {7L, "Федоров Сергей", 107L, "Награда", LocalDate.of(2024, 6, 20)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "incomplete.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
        assertEquals(7L, result.get(1).getEmployeeId());
    }

    @Test
    void processFile_WithInvalidNumbers_ShouldSkipInvalidRows() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {"не число", "Иванов Иван", 101L, "Награда", LocalDate.of(2024, 1, 15)},
                        {2L, "Петров Петр", "не число", "Премия", LocalDate.of(2024, 2, 20)},
                        {3L, "Сидорова Анна", 103L, "Бонус", LocalDate.of(2024, 3, 25)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_numbers.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getEmployeeId());
    }

    @Test
    void processFile_WithInvalidDates_ShouldSkipInvalidRows() throws Exception {
        // Given
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1L, "Иванов Иван", 101L, "Награда", "не дата"},
                        {2L, "Петров Петр", 102L, "Премия", "2024/01/15"},
                        {3L, "Сидорова Анна", 103L, "Бонус", LocalDate.of(2024, 1, 15)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_dates.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getEmployeeId());
    }

    @Test
    void processFile_WithWhitespaceInStrings_ShouldTrimWhitespace() throws Exception {
        // Given - строки с пробелами
        byte[] excelContent = createTestExcelFile(
                new Object[][]{
                        {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"},
                        {1L, "  Иванов Иван  ", 101L, "  Награда  ", LocalDate.of(2024, 1, 15)}
                }
        );

        MockMultipartFile file = new MockMultipartFile(
                "file", "whitespace.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelContent
        );

        // When
        List<RewardUploadDto> result = excelFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Иванов Иван", result.get(0).getEmployeeFullName());
        assertEquals("Награда", result.get(0).getRewardName());
    }

// ========== Тесты rewardService ==========


    @Test
    void processRewards_WithValidDtos_ShouldDelegateToRewardService() {
        // Given
        List<RewardUploadDto> rewardDtos = Arrays.asList(
                new RewardUploadDto(1L, "Иванов Иван", 101L, "Награда", LocalDate.of(2024, 1, 15)),
                new RewardUploadDto(2L, "Петров Петр", 102L, "Премия", LocalDate.of(2024, 2, 20))
        );

        UploadResponse expectedResponse = new UploadResponse(2, 2, 0, List.of());
        when(rewardService.processRewards(rewardDtos)).thenReturn(expectedResponse);

        // When
        UploadResponse result = excelFileProcessingService.processRewards(rewardDtos);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(rewardService, times(1)).processRewards(rewardDtos);
    }

    @Test
    void processRewards_WithEmptyList_ShouldDelegateToRewardService() {
        // Given
        List<RewardUploadDto> emptyList = Collections.emptyList();
        UploadResponse expectedResponse = new UploadResponse(0, 0, 0, List.of("No records"));
        when(rewardService.processRewards(emptyList)).thenReturn(expectedResponse);

        // When
        UploadResponse result = excelFileProcessingService.processRewards(emptyList);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(rewardService, times(1)).processRewards(emptyList);
    }

    @Test
    void processRewards_WithNullList_ShouldDelegateToRewardService() {
        // Given
        UploadResponse expectedResponse = new UploadResponse(0, 0, 0, List.of("Null list"));
        when(rewardService.processRewards(null)).thenReturn(expectedResponse);

        // When
        UploadResponse result = excelFileProcessingService.processRewards(null);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(rewardService, times(1)).processRewards(null);
    }

    // ========== Тесты Exception ==========

    @Test
    void processFile_WithNullFile_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> excelFileProcessingService.processFile(null));
    }

    @Test
    void processFile_WithIOExceptionOnRead_ShouldThrowException() throws Exception {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenThrow(new IOException("File read error"));

        // When & Then
        assertThrows(Exception.class, () -> excelFileProcessingService.processFile(mockFile));
    }

    @Test
    void processFile_WithInvalidExcelFile_ShouldThrowException() {
        // Given - невалидный Excel файл
        byte[] invalidContent = "This is not an Excel file".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                invalidContent
        );

        // When & Then
        assertThrows(Exception.class, () -> excelFileProcessingService.processFile(file));
    }

    @Test
    void processFile_WithEmptyFile_ShouldThrowException() {
        // Given
        byte[] emptyContent = new byte[0];
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                emptyContent
        );

        // When & Then
        assertThrows(Exception.class, () -> excelFileProcessingService.processFile(file));
    }

    // ========== Вспомогательные методы ==========

    private byte[] createTestExcelFile(Object[][] data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Награды");

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                Object value = data[i][j];

                if (value == null) {
                    cell.setCellValue((String) null);
                } else if (value instanceof Long) {
                    cell.setCellValue((Long) value);
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else if (value instanceof String) {
                    cell.setCellValue((String) value);
                } else if (value instanceof LocalDate) {
                    cell.setCellValue((String) value.toString());
                }
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private byte[] createTestExcelFileWithMixedDataTypes(Object[][] data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Награды");

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                Object value = data[i][j];

                if (value == null) {
                    cell.setCellValue((String) null);
                } else if (value instanceof Long) {
                    cell.setCellValue((Long) value);
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else if (value instanceof String) {
                    cell.setCellValue((String) value);
                } else if (value instanceof LocalDate) {
                    cell.setCellValue((String) value.toString());
                }
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}