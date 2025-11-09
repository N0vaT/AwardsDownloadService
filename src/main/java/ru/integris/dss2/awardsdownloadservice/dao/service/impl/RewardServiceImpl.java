package ru.integris.dss2.awardsdownloadservice.dao.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.integris.dss2.awardsdownloadservice.dao.entity.Employee;
import ru.integris.dss2.awardsdownloadservice.dao.entity.Reward;
import ru.integris.dss2.awardsdownloadservice.dao.repository.EmployeeRepository;
import ru.integris.dss2.awardsdownloadservice.dao.repository.RewardRepository;
import ru.integris.dss2.awardsdownloadservice.dao.service.RewardService;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    
    private final EmployeeRepository employeeRepository;
    private final RewardRepository rewardRepository;
    
    @Transactional
    @Override
    public UploadResponse processRewards(List<RewardUploadDto> rewardDtos) {
        List<String> errors = new ArrayList<>();
        int successfulRecords = 0;
        int totalRecords = rewardDtos.size();
        
        if (rewardDtos.isEmpty()) {
            errors.add("No valid records found in the file");
            return new UploadResponse(totalRecords, successfulRecords, totalRecords, errors);
        }
        
        // Собираем все id сотрудников из файла
        Set<Long> employeeIdsInFile = rewardDtos.stream()
                .map(RewardUploadDto::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // Проверяем какие сотрудники существуют в БД
        Set<Long> existingEmployeeIds = employeeRepository.findExistingEmployeeIds(employeeIdsInFile);
        
        // Группируем награды по сотрудникам
        Map<Long, List<RewardUploadDto>> rewardsByEmployee = rewardDtos.stream()
                .filter(dto -> dto.getEmployeeId() != null && existingEmployeeIds.contains(dto.getEmployeeId()))
                .collect(Collectors.groupingBy(RewardUploadDto::getEmployeeId));
        
        // Загружаем всех существующих сотрудников
        Map<Long, Employee> employeesMap = employeeRepository.findAllById(existingEmployeeIds)
                .stream()
                .collect(Collectors.toMap(Employee::getId, employee -> employee));
        
        // Сохраняем награды
        for (Map.Entry<Long, List<RewardUploadDto>> entry : rewardsByEmployee.entrySet()) {
            Long employeeId = entry.getKey();
            Employee employee = employeesMap.get(employeeId);
            
            if (employee != null) {
                List<Reward> rewards = entry.getValue().stream()
                        .map(dto -> createRewardFromDto(dto, employee))
                        .collect(Collectors.toList());
                
                rewardRepository.saveAll(rewards);
                successfulRecords += rewards.size();
            }
        }
        
        int failedRecords = totalRecords - successfulRecords;
        
        if (failedRecords > 0) {
            errors.add(failedRecords + " records were skipped because employees don't exist in the database");
        }
        
        log.info("Processed {} records: {} successful, {} failed",
                totalRecords, successfulRecords, failedRecords);
        
        return new UploadResponse(totalRecords, successfulRecords, failedRecords, errors);
    }
    
    private Reward createRewardFromDto(RewardUploadDto dto, Employee employee) {
        Reward reward = new Reward();
        reward.setId(dto.getRewardId());
        reward.setRewardName(dto.getRewardName());
        reward.setReceiptDate(dto.getReceiptDate());
        reward.setEmployee(employee);
        return reward;
    }
}