package ru.integris.dss2.awardsdownloadservice.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.integris.dss2.awardsdownloadservice.dao.entity.Employee;

import java.util.Set;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    @Query("SELECT e.id FROM Employee e WHERE e.id IN :employeeIds")
    Set<Long> findExistingEmployeeIds(@Param("employeeIds") Set<Long> employeeIds);
}