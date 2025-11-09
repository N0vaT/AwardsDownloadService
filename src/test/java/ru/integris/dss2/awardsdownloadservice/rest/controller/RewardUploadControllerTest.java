package ru.integris.dss2.awardsdownloadservice.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import ru.integris.dss2.awardsdownloadservice.dao.service.impl.CsvFileProcessingService;
import ru.integris.dss2.awardsdownloadservice.dao.service.impl.ExcelFileProcessingService;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RewardUploadController.class)
class RewardUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CsvFileProcessingService csvFileProcessingService;

    @MockBean
    private ExcelFileProcessingService excelFileProcessingService;

    @Test
    void uploadRewardsFile_WithCsvFile_ShouldReturnSuccess() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rewards.csv",
                "text/csv",
                "1,Иванов Иван Иванович,101,Лучший сотрудник,2024-01-15\n".getBytes()
        );

        UploadResponse response = new UploadResponse(1, 1, 0, List.of());

        when(csvFileProcessingService.supportsFileType(anyString(), anyString()))
                .thenReturn(true);
        when(csvFileProcessingService.processFile(any()))
                .thenReturn(List.of());
        when(csvFileProcessingService.processRewards(any()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(multipart("/api/rewards/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.successfulRecords").value(1))
                .andExpect(jsonPath("$.failedRecords").value(0));
    }

    @Test
    void uploadRewardsFile_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/api/rewards/upload").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("File is empty"));
    }

    @Test
    void uploadRewardsFile_WithUnsupportedFileType_ShouldReturnBadRequest() throws Exception {
        // Given
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "rewards.txt",
                "text/plain",
                "test content".getBytes()
        );

        when(csvFileProcessingService.supportsFileType(anyString(), anyString()))
                .thenReturn(false);
        when(excelFileProcessingService.supportsFileType(anyString(), anyString()))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(multipart("/api/rewards/upload").file(unsupportedFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Unsupported file type. Supported types: CSV, Excel"));
    }
}