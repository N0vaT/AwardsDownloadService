package ru.integris.dss2.awardsdownloadservice.dao.service.impl;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.integris.dss2.awardsdownloadservice.dao.service.FileProcessingService;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvFileProcessingService implements FileProcessingService {

    private static final String CSV_TYPE = "text/csv";
    private static final String[] CSV_EXTENSIONS = {".csv"};

    private final RewardServiceImpl rewardService;

    public CsvFileProcessingService(RewardServiceImpl rewardService) {
        this.rewardService = rewardService;
    }

    @Override
    public boolean supportsFileType(String contentType, String filename) {
        if (contentType != null && contentType.equals(CSV_TYPE)) {
            return true;
        }
        if (filename != null) {
            for (String extension : CSV_EXTENSIONS) {
                if (filename.toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<RewardUploadDto> processFile(MultipartFile file) throws Exception {
        try (Reader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CsvToBean<RewardUploadDto> csvToBean = new CsvToBeanBuilder<RewardUploadDto>(reader)
                    .withType(RewardUploadDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            return csvToBean.parse();
        }
    }

    @Override
    public UploadResponse processRewards(List<RewardUploadDto> rewardDtos) {
        return rewardService.processRewards(rewardDtos);
    }
}