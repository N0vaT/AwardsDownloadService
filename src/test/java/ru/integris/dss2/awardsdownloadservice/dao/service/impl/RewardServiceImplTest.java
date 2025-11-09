package ru.integris.dss2.awardsdownloadservice.dao.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.integris.dss2.awardsdownloadservice.dao.entity.Employee;
import ru.integris.dss2.awardsdownloadservice.dao.repository.EmployeeRepository;
import ru.integris.dss2.awardsdownloadservice.dao.repository.RewardRepository;
import ru.integris.dss2.awardsdownloadservice.dto.RewardUploadDto;
import ru.integris.dss2.awardsdownloadservice.dto.UploadResponse;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RewardRepository rewardRepository;

    @InjectMocks
    private RewardServiceImpl rewardService;

    private List<RewardUploadDto> validRewardDtos;
    private Set<Long> existingEmployeeIds;

    @BeforeEach
    void setUp() {
        validRewardDtos = Arrays.asList(
                new RewardUploadDto(1L, "Иванов Иван Иванович", 101L, "Лучший сотрудник", LocalDate.of(2024, 1, 15)),
                new RewardUploadDto(1L, "Иванов Иван Иванович", 102L, "Премия года", LocalDate.of(2024, 2, 20)),
                new RewardUploadDto(2L, "Петров Петр Петрович", 103L, "Инновация", LocalDate.of(2024, 3, 10))
        );

        existingEmployeeIds = new HashSet<>(Arrays.asList(1L, 2L));
    }

    @Test
    void processRewards_WithValidData_ShouldSaveRewards() {
        // Given
        when(employeeRepository.findExistingEmployeeIds(anySet()))
                .thenReturn(existingEmployeeIds);

        Employee employee1 = Employee.builder()
                .id(1L)
                .fullName("Иванов Иван Иванович")
                .build();
        Employee employee2 = Employee.builder()
                .id(2L)
                .fullName("Петров Петр Петрович")
                .build();

        when(employeeRepository.findAllById(anySet()))
                .thenReturn(Arrays.asList(employee1, employee2));

        when(rewardRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UploadResponse response = rewardService.processRewards(validRewardDtos);

        // Then
        assertNotNull(response);
        assertEquals(3, response.getTotalRecords());
        assertEquals(3, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertTrue(response.getErrors().isEmpty());

        verify(rewardRepository, times(2)).saveAll(any());
    }

    @Test
    void processRewards_WithNonExistingEmployees_ShouldSkipRecords() {
        // Given
        List<RewardUploadDto> mixedDtos = Arrays.asList(
                new RewardUploadDto(1L, "Иванов Иван Иванович", 101L, "Лучший сотрудник", LocalDate.of(2024, 1, 15)),
                new RewardUploadDto(99L, "Несуществующий Сотрудник", 102L, "Премия", LocalDate.of(2024, 2, 20)) // Не существует
        );

        when(employeeRepository.findExistingEmployeeIds(anySet()))
                .thenReturn(Set.of(1L));

        Employee employee1 = Employee.builder()
                .id(1L)
                .fullName("Иванов Иван Иванович")
                .build();
        when(employeeRepository.findAllById(anySet()))
                .thenReturn(List.of(employee1));

        when(rewardRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UploadResponse response = rewardService.processRewards(mixedDtos);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getTotalRecords());
        assertEquals(1, response.getSuccessfulRecords());
        assertEquals(1, response.getFailedRecords());
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).contains("1 records were skipped"));
    }

    @Test
    void processRewards_WithEmptyList_ShouldReturnError() {
        // When
        UploadResponse response = rewardService.processRewards(List.of());

        // Then
        assertNotNull(response);
        assertEquals(0, response.getTotalRecords());
        assertEquals(0, response.getSuccessfulRecords());
        assertEquals(0, response.getFailedRecords());
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).contains("No valid records found"));

        verify(rewardRepository, never()).saveAll(any());
    }
}