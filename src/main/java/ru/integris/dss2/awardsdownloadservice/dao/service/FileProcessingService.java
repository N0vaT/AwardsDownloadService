package ru.integris.dss2.awardsdownloadservice.dao.service;

import org.springframework.web.multipart.MultipartFile;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.util.List;

public interface FileProcessingService {
    boolean supportsFileType(String contentType, String filename);
    List<RewardUploadDto> processFile(MultipartFile file) throws Exception;
    UploadResponse processRewards(List<RewardUploadDto> rewardDtos);
}