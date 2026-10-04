package com.govtrack360.repository;

import com.govtrack360.entity.Employee;
import com.govtrack360.entity.Status;
import com.govtrack360.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);

    Optional<Employee> findByUser(User user);

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    long countByStatus(Status status);

    long countByDepartmentId(Long departmentId);

    List<Employee> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT e FROM Employee e WHERE " +
           "(:search IS NULL OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:status IS NULL OR e.status = :status)")
    List<Employee> search(@Param("search") String search,
                           @Param("departmentId") Long departmentId,
                           @Param("status") Status status);

    // ---------------------------------------------------------------------------------------
    // Phase 3 additions: null-free lookups (no optional parameters, so no PostgreSQL type-inference issues)
    // ---------------------------------------------------------------------------------------

    @Query("SELECT e FROM Employee e WHERE e.status = :status ORDER BY e.firstName, e.lastName")
    List<Employee> findByStatusOrdered(@Param("status") Status status);

    @Query("SELECT e FROM Employee e WHERE e.status = :status AND e.department.id = :departmentId " +
           "ORDER BY e.firstName, e.lastName")
    List<Employee> findByStatusAndDepartmentOrdered(@Param("status") Status status,
                                                     @Param("departmentId") Long departmentId);

    /** Active employees, optionally limited to one department (null = all departments). */
    default List<Employee> findActive(Long departmentId) {
        return departmentId == null
                ? findByStatusOrdered(Status.ACTIVE)
                : findByStatusAndDepartmentOrdered(Status.ACTIVE, departmentId);
    }
}
