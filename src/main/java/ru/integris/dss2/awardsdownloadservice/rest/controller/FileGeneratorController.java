package ru.integris.dss2.awardsdownloadservice.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.integris.dss2.awardsdownloadservice.dao.util.TestFileGenerator;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/file")
@Tag(name = "FilesGenerator", description = "API для генерации тестовых файлов (csv, xlsx)")
public class FileGeneratorController {

    @Operation(
            summary = "Сгенерировать тестовый CSV файл",
            description = "Структура Excel: ID сотрудника, ФИО, Название награды, Дата получения (ISO-8601)"
    )
    @GetMapping("/generate-csv")
    public ResponseEntity<Resource> generateCsvFile() {
        Resource resource = TestFileGenerator.generateCsvFile();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test_rewards.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @Operation(
            summary = "Сгенерировать тестовый Excel файл",
            description = "Структура Excel: ID сотрудника, ФИО, Название награды, Дата получения (ISO-8601)"
    )
    @GetMapping("/generate-excel")
    public ResponseEntity<Resource> generateExcelFile() throws IOException {
        Resource resource = TestFileGenerator.generateExcelFile();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=test_rewards.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}