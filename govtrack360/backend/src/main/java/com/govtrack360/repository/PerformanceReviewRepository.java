package com.govtrack360.repository;

import com.govtrack360.entity.Employee;
import com.govtrack360.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {

    boolean existsByEmployeeAndPeriodStartAndPeriodEnd(Employee employee, LocalDate periodStart, LocalDate periodEnd);

    @Query("SELECT r FROM PerformanceReview r WHERE r.employee.id = :employeeId " +
           "ORDER BY r.periodEnd DESC, r.createdAt DESC")
    List<PerformanceReview> findByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT r FROM PerformanceReview r ORDER BY r.periodEnd DESC, r.createdAt DESC")
    List<PerformanceReview> findAllOrdered();

    @Query("SELECT r FROM PerformanceReview r WHERE r.employee.department.id = :departmentId " +
           "ORDER BY r.periodEnd DESC, r.createdAt DESC")
    List<PerformanceReview> findByDepartmentId(@Param("departmentId") Long departmentId);
}
