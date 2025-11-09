package ru.integris.dss2.awardsdownloadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    private List<String> errors;
}