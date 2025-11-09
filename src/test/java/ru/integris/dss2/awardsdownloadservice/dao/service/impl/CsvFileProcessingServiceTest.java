package ru.integris.dss2.awardsdownloadservice.dao.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvFileProcessingServiceTest {

    @Mock
    private RewardServiceImpl rewardService;

    private CsvFileProcessingService csvFileProcessingService;

    @BeforeEach
    void setUp() {
        csvFileProcessingService = new CsvFileProcessingService(rewardService);
    }

    // ======= Тесты типов ==========

    @ParameterizedTest
    @ValueSource(strings = {"text/csv", "text/csv; charset=utf-8", "text/csv;charset=UTF-8"})
    void supportsFileType_WithValidContentType_ShouldReturnTrue(String contentType) {
        assertTrue(csvFileProcessingService.supportsFileType(contentType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rewards.csv", "data.CSV", "file.Csv", "test.data.csv", "UPPERCASE.CSV"})
    void supportsFileType_WithCsvFilename_ShouldReturnTrue(String filename) {
        assertTrue(csvFileProcessingService.supportsFileType(null, filename));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "text/plain", "application/vnd.ms-excel",
            "image/png", "application/octet-stream", "text/html"})
    void supportsFileType_WithInvalidContentType_ShouldReturnFalse(String contentType) {
        assertFalse(csvFileProcessingService.supportsFileType(contentType, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rewards.txt", "data.xlsx", "file.doc", "test.data", "archive.zip",
            "image.png", "document.pdf"})
    void supportsFileType_WithNonCsvFilename_ShouldReturnFalse(String filename) {
        assertFalse(csvFileProcessingService.supportsFileType(null, filename));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void supportsFileType_WithNullOrEmptyFilename_ShouldReturnFalse(String filename) {
        assertFalse(csvFileProcessingService.supportsFileType(null, filename));
    }

    @Test
    void supportsFileType_WithNullContentTypeAndNullFilename_ShouldReturnFalse() {
        assertFalse(csvFileProcessingService.supportsFileType(null, null));
    }

    // ========== Тесты для processFile ==========

    @Test
    void processFile_WithValidCsvContent_ShouldReturnListOfDtos() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван Иванович,101,Лучший сотрудник,2024-01-15
                2,Петров Петр Петрович,102,Премия года,2024-02-20
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rewards.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8) // Явно указываем UTF-8
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        RewardUploadDto firstDto = result.get(0);
        assertEquals(1L, firstDto.getEmployeeId());
        assertEquals("Иванов Иван Иванович", firstDto.getEmployeeFullName()); // Теперь будет правильно
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
    void processFile_WithCsvContainingEmptyLines_ShouldIgnoreEmptyLines() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван Иванович,101,Лучший сотрудник,2024-01-15
                            
                2,Петров Петр Петрович,102,Премия года,2024-02-20
                            
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "rewards.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void processFile_WithEmptyCsv_ShouldReturnEmptyList() throws Exception {
        // Given
        String csvContent = "";
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void processFile_WithSingleRecord_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = "1,Иванов Иван,101,Награда,2024-01-15";
        MockMultipartFile file = new MockMultipartFile(
                "file", "single.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
        assertEquals("Иванов Иван", result.get(0).getEmployeeFullName());
    }

    @Test
    void processFile_WithLargeCsv_ShouldParseAllRecords() throws Exception {
        // Given
        StringBuilder csvContent = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            csvContent.append(String.format("%d,Сотрудник %d,%d,Награда %d,2024-01-%02d\n",
                    i, i, 100 + i, i, (i % 30) + 1));
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "large.csv", "text/csv",
                csvContent.toString().getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(100, result.size());
        assertEquals(1L, result.get(0).getEmployeeId());
        assertEquals(100L, result.get(99).getEmployeeId());
        assertEquals("Сотрудник 1", result.get(0).getEmployeeFullName());
        assertEquals("Сотрудник 100", result.get(99).getEmployeeFullName());
    }

    @Test
    void processFile_WithUtf8Characters_ShouldHandleCorrectly() throws Exception {
        // Given
        String csvContent = "1,Иванов Иван Иванович,101,Сотрудник года 🏆,2024-01-15";
        MockMultipartFile file = new MockMultipartFile(
                "file", "utf8.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Сотрудник года 🏆", result.get(0).getRewardName());
    }

    @Test
    void processFile_WithQuotedFields_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = """
                    \"1\",\"Иванов, Иван Иванович\",\"101\",\"Лучший, сотрудник\",\"2024-01-15\"
                    \"2\",\"Петров \"\"Петр\"\" Петрович\",\"102\",\"Премия \"\"года\"\"\",\"2024-02-20\"
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "quoted.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Иванов, Иван Иванович", result.get(0).getEmployeeFullName());
        assertEquals("Петров \"Петр\" Петрович", result.get(1).getEmployeeFullName());
    }

// ====== Тесты ISO-8601 дат =========

    @Test
    void processFile_WithBasicIso8601Dates_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван Иванович,101,Лучший сотрудник,2024-01-15
                2,Петров Петр Петрович,102,Премия года,2024-02-20
                3,Сидорова Анна,103,Награда,2024-12-31
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "rewards.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(LocalDate.of(2024, 1, 15), result.get(0).getReceiptDate());
        assertEquals(LocalDate.of(2024, 2, 20), result.get(1).getReceiptDate());
        assertEquals(LocalDate.of(2024, 12, 31), result.get(2).getReceiptDate());
    }

    @Test
    void processFile_WithIso8601DateTime_ShouldParseDatePartOnly() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2024-01-15T10:30:00
                2,Петров Петр,102,Премия,2024-02-20T15:45:30Z
                3,Сидорова Анна,103,Бонус,2024-03-25T08:00:00+03:00
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "datetime.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(LocalDate.of(2024, 1, 15), result.get(0).getReceiptDate());
        assertEquals(LocalDate.of(2024, 2, 20), result.get(1).getReceiptDate());
        assertEquals(LocalDate.of(2024, 3, 25), result.get(2).getReceiptDate());
    }

    @Test
    void processFile_WithIso8601OrdinalDate_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2024-001
                2,Петров Петр,102,Премия,2024-032
                3,Сидорова Анна,103,Бонус,2024-366
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "ordinal.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(LocalDate.of(2024, 1, 1), result.get(0).getReceiptDate());
        assertEquals(LocalDate.of(2024, 2, 1), result.get(1).getReceiptDate());
        assertEquals(LocalDate.of(2024, 12, 31), result.get(2).getReceiptDate());
    }

    @Test
    void processFile_WithIso8601WeekDate_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2024-W01-1
                2,Петров Петр,102,Премия,2024-W05-3
                3,Сидорова Анна,103,Бонус,2024-W52-7
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "weekdate.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(LocalDate.of(2024, 1, 1), result.get(0).getReceiptDate());
        assertEquals(LocalDate.of(2024, 1, 31), result.get(1).getReceiptDate());
        assertEquals(LocalDate.of(2024, 12, 29), result.get(2).getReceiptDate());
    }

    @ParameterizedTest
    @CsvSource({
            "2024-01-15,       2024-01-15",
            "2024-02-29,       2024-02-29",
            "2024-12-31,       2024-12-31",
            "2024-01-15T10:30, 2024-01-15",
            "2024-01-15T10:30:45, 2024-01-15",
            "2024-01-15T10:30:45Z, 2024-01-15",
            "2024-01-15T10:30:45+03:00, 2024-01-15",
            "2024-001,          2024-01-01",
            "2024-366,          2024-12-31",
            "2024-W01-1,        2024-01-01",
            "2024-W52-7,        2024-12-29"
    })
    void processFile_WithVariousIso8601Formats_ShouldParseCorrectly(
            String isoDate, LocalDate expectedDate) throws Exception {

        // Given
        String csvContent = String.format("1,Иванов Иван,101,Награда,%s", isoDate);

        MockMultipartFile file = new MockMultipartFile(
                "file", "varied.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedDate, result.get(0).getReceiptDate());
        assertEquals("Иванов Иван", result.get(0).getEmployeeFullName()); // Проверяем русский текст
    }

    @Test
    void processFile_WithInvalidIso8601Date_ShouldThrowException() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2024/01/15
                2,Петров Петр,102,Премия,15.01.2024
                3,Сидорова Анна,103,Бонус,January 15, 2024
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_dates.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(Exception.class, () -> csvFileProcessingService.processFile(file));
    }

    @Test
    void processFile_WithOutOfRangeDate_ShouldThrowException() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2024-02-30
                2,Петров Петр,102,Премия,2023-02-29
                3,Сидорова Анна,103,Бонус,2024-13-01
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_range.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(Exception.class, () -> csvFileProcessingService.processFile(file));
    }

    @Test
    void processFile_WithMixedValidAndInvalidDates_ShouldFailOnFirstInvalid() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2024-01-15
                2,Петров Петр,102,Премия,invalid-date
                3,Сидорова Анна,103,Бонус,2024-03-20
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "mixed.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(Exception.class, () -> csvFileProcessingService.processFile(file));
    }

    @Test
    void processFile_WithHistoricalDates_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2000-01-01
                2,Петров Петр,102,Премия,1999-12-31
                3,Сидорова Анна,103,Бонус,1900-01-01
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "historical.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        assertEquals(LocalDate.of(2000, 1, 1), result.get(0).getReceiptDate());
        assertEquals(LocalDate.of(1999, 12, 31), result.get(1).getReceiptDate());
        assertEquals(LocalDate.of(1900, 1, 1), result.get(2).getReceiptDate());
    }

    @Test
    void processFile_WithFutureDates_ShouldParseCorrectly() throws Exception {
        // Given
        String csvContent = """
                1,Иванов Иван,101,Награда,2030-01-01
                2,Петров Петр,102,Премия,2050-12-31
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "future.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When
        List<RewardUploadDto> result = csvFileProcessingService.processFile(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(LocalDate.of(2030, 1, 1), result.get(0).getReceiptDate());
        assertEquals(LocalDate.of(2050, 12, 31), result.get(1).getReceiptDate());
    }

// ========== Тесты rewardService ==========

    @Test
    void processRewards_WithValidDtos_ShouldDelegateToRewardService() {
        // Given
        List<RewardUploadDto> rewardDtos = Arrays.asList(
                new RewardUploadDto(1L, "Иванов Иван", 101L, "Награда", LocalDate.parse("2024-01-15")),
                new RewardUploadDto(2L, "Петров Петр", 102L, "Премия", LocalDate.parse("2024-02-20"))
        );

        UploadResponse expectedResponse = new UploadResponse(2, 2, 0, List.of());
        when(rewardService.processRewards(rewardDtos)).thenReturn(expectedResponse);

        // When
        UploadResponse result = csvFileProcessingService.processRewards(rewardDtos);

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
        UploadResponse result = csvFileProcessingService.processRewards(emptyList);

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
        UploadResponse result = csvFileProcessingService.processRewards(null);

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(rewardService, times(1)).processRewards(null);
    }

// ========== Тесты Exception ==========

    @Test
    void processFile_WithNullFile_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> csvFileProcessingService.processFile(null));
    }

    @Test
    void processFile_WithIOExceptionOnRead_ShouldThrowException() throws Exception {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenThrow(new IOException("File read error"));

        // When & Then
        assertThrows(Exception.class, () -> csvFileProcessingService.processFile(mockFile));
    }

    @Test
    void processFile_WithMalformedCsv_ShouldThrowException() throws Exception {
        // Given - невалидный CSV с незакрытыми кавычками
        String csvContent = "1,\"Иванов Иван,101,\"Награда\",2024-01-15";
        MockMultipartFile file = new MockMultipartFile(
                "file", "malformed.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(Exception.class, () -> csvFileProcessingService.processFile(file));
    }

    @Test
    void processFile_WithInvalidNumberFormat_ShouldThrowException() throws Exception {
        // Given - невалидные числовые значения
        String csvContent = """
                invalid,Иванов Иван,101,Награда,2024-01-15
                2,Петров Петр,not_number,Премия,2024-02-20
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid_numbers.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        // When & Then
        assertThrows(Exception.class, () -> csvFileProcessingService.processFile(file));
    }
}