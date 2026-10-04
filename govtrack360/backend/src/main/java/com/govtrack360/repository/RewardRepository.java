package com.govtrack360.repository;

import com.govtrack360.entity.Reward;
import com.govtrack360.entity.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    @Query("SELECT r FROM Reward r ORDER BY r.awardedDate DESC, r.createdAt DESC")
    List<Reward> findAllOrdered();

    @Query("SELECT r FROM Reward r ORDER BY r.awardedDate DESC, r.createdAt DESC")
    List<Reward> findRecent(Pageable pageable);

    @Query("SELECT r FROM Reward r WHERE r.employee.department.id = :departmentId " +
           "ORDER BY r.awardedDate DESC, r.createdAt DESC")
    List<Reward> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT r FROM Reward r WHERE r.employee.id = :employeeId " +
           "ORDER BY r.awardedDate DESC, r.createdAt DESC")
    List<Reward> findByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT r FROM Reward r WHERE r.awardedDate BETWEEN :from AND :to " +
           "ORDER BY r.awardedDate DESC, r.createdAt DESC")
    List<Reward> findByAwardedDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT r FROM Reward r WHERE r.employee.department.id = :departmentId " +
           "AND r.awardedDate BETWEEN :from AND :to ORDER BY r.awardedDate DESC, r.createdAt DESC")
    List<Reward> findByDepartmentIdAndAwardedDateBetween(@Param("departmentId") Long departmentId,
                                                         @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to);

    /** Null when the employee has no rewards yet. */
    @Query("SELECT SUM(r.points) FROM Reward r WHERE r.employee.id = :employeeId")
    Long sumPointsByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT COUNT(r) FROM Reward r WHERE r.type = :type AND r.employee.department.id = :departmentId " +
           "AND r.awardedDate BETWEEN :from AND :to")
    long countByTypeAndDepartmentBetween(@Param("type") RewardType type,
                                         @Param("departmentId") Long departmentId,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
