package com.govtrack360.repository;

import com.govtrack360.entity.Attendance;
import com.govtrack360.entity.AttendanceStatus;
import com.govtrack360.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeAndDate(Employee employee, LocalDate date);

    boolean existsByEmployeeAndDate(Employee employee, LocalDate date);

    List<Attendance> findByEmployeeAndDateBetweenOrderByDateDesc(Employee employee, LocalDate from, LocalDate to);

    long countByDateAndStatus(LocalDate date, AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE " +
           "(:employeeId IS NULL OR a.employee.id = :employeeId) " +
           "AND (:departmentId IS NULL OR a.employee.department.id = :departmentId) " +
           "AND (:date IS NULL OR a.date = :date) " +
           "ORDER BY a.date DESC")
    List<Attendance> search(@Param("employeeId") Long employeeId,
                             @Param("departmentId") Long departmentId,
                             @Param("date") LocalDate date);


    // Phase 3 addition (performance scoring and reports)
    @Query("SELECT a FROM Attendance a WHERE a.employee.id IN :employeeIds AND a.date BETWEEN :from AND :to")
    List<Attendance> findForEmployeesBetween(@Param("employeeIds") Collection<Long> employeeIds,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);
}
