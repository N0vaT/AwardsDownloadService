package ru.integris.dss2.awardsdownloadservice.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.integris.dss2.awardsdownloadservice.dao.service.FileProcessingService;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rewards")
@Tag(name = "Reward Upload API", description = "API для загрузки наград сотрудников из файлов")
public class RewardUploadController {

    private final List<FileProcessingService> fileProcessingServices;

    @Operation(
            summary = "Загрузка файла с наградами сотрудников",
            description = """
                    Загрузка Excel или CSV файла с информацией о наградах сотрудников.
                    Формат файла: ID сотрудника, ФИО, ID награды, название награды, дата получения (ISO-8601)
                    """,
            parameters = {
                    @Parameter(
                            name = "file",
                            description = "Excel (.xlsx, .xls) или CSV файл с данными о наградах",
                            required = true,
                            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
                    )
            }
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UploadResponse> uploadRewardsFile(@RequestParam("file") MultipartFile file) {
        log.info("Received file upload request: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(0, 0, 0, List.of("File is empty")));
        }

        FileProcessingService processor = findFileProcessor(file.getContentType(), file.getOriginalFilename());

        if (processor == null) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(0, 0, 0,
                            List.of("Unsupported file type. Supported types: CSV, Excel")));
        }

        try {
            var rewardDtos = processor.processFile(file);
            UploadResponse response = processor.processRewards(rewardDtos);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new UploadResponse(0, 0, 0,
                            List.of("Error processing file: " + e.getMessage())));
        }
    }

    private FileProcessingService findFileProcessor(String contentType, String filename) {
        return fileProcessingServices.stream()
                .filter(processor -> processor.supportsFileType(contentType, filename))
                .findFirst()
                .orElse(null);
    }
}