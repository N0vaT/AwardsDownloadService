package ru.integris.dss2.awardsdownloadservice.dao.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class TestFileGenerator {

    public static Resource generateCsvFile() {
        String csvContent = """
                1,Иванов Иван Иванович,101,Лучший сотрудник месяца,2025-11-09
                2,Петров Петр Петрович,102,Премия года,2025-11-08
                3,Сидорова Анна Владимировна,103,Инноватор года,2025-11-07
                4,Козлова Елена Сергеевна,104,Ментор года,2025-11-07
                5,Николаев Дмитрий Алексеевич,105,Проект года,2025-11-07
                """;

        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(csvBytes);
    }

    public static Resource generateExcelFile() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Награды");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID сотрудника", "ФИО", "ID награды", "Название награды", "Дата получения"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        Object[][] data = {
                {1L, "Иванов Иван Иванович", 101L, "Лучший сотрудник месяца", LocalDate.now()},
                {2L, "Петров Петр Петрович", 102L, "Премия года", LocalDate.now()},
                {3L, "Сидорова Анна Владимировна", 103L, "Инноватор года", LocalDate.now()},
                {4L, "Козлова Елена Сергеевна", 104L, "Ментор года", LocalDate.now()},
                {5L, "Николаев Дмитрий Алексеевич", 105L, "Проект года", LocalDate.now()}
        };

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue((Long) data[i][0]);
            row.createCell(1).setCellValue((String) data[i][1]);
            row.createCell(2).setCellValue((Long) data[i][2]);
            row.createCell(3).setCellValue((String) data[i][3]);
            row.createCell(4).setCellValue(data[i][4].toString());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new ByteArrayResource(outputStream.toByteArray());
    }
}