package com.govtrack360.repository;

import com.govtrack360.entity.Employee;
import com.govtrack360.entity.Task;
import com.govtrack360.entity.TaskPriority;
import com.govtrack360.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByTaskCode(String taskCode);

    boolean existsByTaskCode(String taskCode);

    long countByStatus(TaskStatus status);

    long countByAssignedTo(Employee employee);

    long countByAssignedToAndStatus(Employee employee, TaskStatus status);

    long countByStatusAndDueDateBefore(TaskStatus status, LocalDate date);

    List<Task> findTop5ByOrderByCreatedAtDesc();

    List<Task> findByAssignedToAndDueDateBetweenAndStatusNotIn(
            Employee employee, LocalDate from, LocalDate to, List<TaskStatus> excludedStatuses);

    long countByDepartmentIdAndStatus(Long departmentId, TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < :today AND t.status NOT IN :excludedStatuses")
    long countOverdue(@Param("today") LocalDate today, @Param("excludedStatuses") List<TaskStatus> excludedStatuses);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.department.id = :departmentId " +
           "AND t.dueDate < :today AND t.status NOT IN :excludedStatuses")
    long countOverdueByDepartment(@Param("departmentId") Long departmentId, @Param("today") LocalDate today,
                                   @Param("excludedStatuses") List<TaskStatus> excludedStatuses);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :employeeId " +
           "AND t.dueDate < :today AND t.status NOT IN :excludedStatuses")
    long countOverdueByAssignee(@Param("employeeId") Long employeeId, @Param("today") LocalDate today,
                                 @Param("excludedStatuses") List<TaskStatus> excludedStatuses);

    /**
     * Generic search used by ADMIN (full visibility), OFFICER (department-scoped) and
     * EMPLOYEE (assignee-scoped). Null filter params are ignored.
     */
    @Query("SELECT t FROM Task t WHERE " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(t.taskCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
           "AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId) " +
           "AND (:restrictDepartmentId IS NULL OR t.department.id = :restrictDepartmentId) " +
           "AND (:restrictAssignedToId IS NULL OR t.assignedTo.id = :restrictAssignedToId) " +
           "ORDER BY t.createdAt DESC")
    List<Task> search(@Param("search") String search,
                       @Param("status") TaskStatus status,
                       @Param("priority") TaskPriority priority,
                       @Param("departmentId") Long departmentId,
                       @Param("assignedToId") Long assignedToId,
                       @Param("restrictDepartmentId") Long restrictDepartmentId,
                       @Param("restrictAssignedToId") Long restrictAssignedToId);


    // ---------------------------------------------------------------------------------------
    // Phase 3 additions (performance scoring and reports)
    // ---------------------------------------------------------------------------------------

    /**
     * Tasks assigned to any of the given employees whose due date falls in [from, to], excluding one status
     * (used to leave CANCELLED tasks out of performance scoring).
     */
    @Query("SELECT t FROM Task t JOIN FETCH t.assignedTo a JOIN FETCH a.department " +
           "WHERE a.id IN :employeeIds AND t.dueDate BETWEEN :from AND :to AND t.status <> :excludedStatus")
    List<Task> findForPerformance(@Param("employeeIds") Collection<Long> employeeIds,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("excludedStatus") TaskStatus excludedStatus);

    @Query("SELECT t FROM Task t JOIN FETCH t.department WHERE t.dueDate BETWEEN :from AND :to " +
           "ORDER BY t.dueDate ASC, t.taskCode ASC")
    List<Task> findByDueDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT t FROM Task t JOIN FETCH t.department WHERE t.department.id = :departmentId " +
           "AND t.dueDate BETWEEN :from AND :to ORDER BY t.dueDate ASC, t.taskCode ASC")
    List<Task> findByDepartmentAndDueDateRange(@Param("departmentId") Long departmentId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);
}
