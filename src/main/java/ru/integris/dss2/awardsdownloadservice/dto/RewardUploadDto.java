package ru.integris.dss2.awardsdownloadservice.dto;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.integris.dss2.awardsdownloadservice.dao.util.LocalDateConverter;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardUploadDto {

    @CsvBindByPosition(position = 0)
    private Long employeeId;

    @CsvBindByPosition(position = 1)
    private String employeeFullName;

    @CsvBindByPosition(position = 2)
    private Long rewardId;

    @CsvBindByPosition(position = 3)
    private String rewardName;

    @CsvCustomBindByPosition(position = 4, converter = LocalDateConverter.class)
    private LocalDate receiptDate;
}