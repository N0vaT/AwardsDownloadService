package ru.integris.dss2.awardsdownloadservice.dao.util;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class LocalDateConverter extends AbstractBeanField<LocalDate, String> {

    private static final DateTimeFormatter ISO_8601_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd['T'HH:mm:ss[.SSS][XXXXX][XXXX]]")
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
            .toFormatter();

    @Override
    protected LocalDate convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return parseIso8601Date(value.trim());
        } catch (Exception e) {
            throw new CsvDataTypeMismatchException(value, LocalDate.class, 
                    "Unable to parse date: " + value + ". Supported formats: ISO-8601");
        }
    }
    
    private LocalDate parseIso8601Date(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e1) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.toLocalDate();
            } catch (Exception e2) {
                try {
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    return offsetDateTime.toLocalDate();
                } catch (Exception e3) {
                    try {
                        if (value.matches("\\d{4}-\\d{3}")) {
                            return LocalDate.parse(value, DateTimeFormatter.ISO_ORDINAL_DATE);
                        }
                        else if (value.matches("\\d{4}-W\\d{2}-[1-7]")) {
                            return LocalDate.parse(value, DateTimeFormatter.ISO_WEEK_DATE);
                        }
                        else {
                            return LocalDate.parse(value, ISO_8601_FORMATTER);
                        }
                    } catch (Exception e4) {
                        throw new IllegalArgumentException("Unsupported date format: " + value);
                    }
                }
            }
        }
    }
}