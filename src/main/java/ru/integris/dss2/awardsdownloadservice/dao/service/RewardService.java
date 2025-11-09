package ru.integris.dss2.awardsdownloadservice.dao.service;

import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.util.List;

public interface RewardService {
    UploadResponse processRewards(List<RewardUploadDto> rewardDtos);
}
