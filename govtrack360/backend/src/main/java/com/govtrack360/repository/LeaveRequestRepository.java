package com.govtrack360.repository;

import com.govtrack360.entity.Employee;
import com.govtrack360.entity.LeaveRequest;
import com.govtrack360.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeOrderByAppliedAtDesc(Employee employee);

    long countByStatus(LeaveStatus status);

    long countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LeaveStatus status, LocalDate startDateInclusive, LocalDate endDateInclusive);

    boolean existsByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Employee employee, LeaveStatus status, LocalDate endDate, LocalDate startDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
           "(:employeeId IS NULL OR lr.employee.id = :employeeId) " +
           "AND (:departmentId IS NULL OR lr.employee.department.id = :departmentId) " +
           "AND (:status IS NULL OR lr.status = :status) " +
           "ORDER BY lr.appliedAt DESC")
    List<LeaveRequest> search(@Param("employeeId") Long employeeId,
                               @Param("departmentId") Long departmentId,
                               @Param("status") LeaveStatus status);


    // Phase 3 addition (performance scoring and reports): leave of a given status overlapping [from, to]
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id IN :employeeIds AND lr.status = :status " +
           "AND lr.startDate <= :to AND lr.endDate >= :from")
    List<LeaveRequest> findOverlapping(@Param("employeeIds") Collection<Long> employeeIds,
                                        @Param("status") LeaveStatus status,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);
}
