package com.govtrack360.repository;

import com.govtrack360.entity.Department;
import com.govtrack360.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentCode(String departmentCode);
    boolean existsByDepartmentCode(String departmentCode);
    List<Department> findByDepartmentNameContainingIgnoreCase(String name);
    long countByStatus(Status status);
}
