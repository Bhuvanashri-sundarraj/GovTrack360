package com.govtrack360.service;

import com.govtrack360.dto.attendance.AttendanceMonthlySummaryResponse;
import com.govtrack360.dto.attendance.AttendanceResponse;
import com.govtrack360.dto.attendance.TodayAttendanceSummaryResponse;
import com.govtrack360.dto.common.EmployeeSummary;
import com.govtrack360.dto.department.DepartmentSummary;
import com.govtrack360.entity.*;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.AttendanceRepository;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime LATE_CUTOFF = LocalTime.of(9, 30);
    private static final double HALF_DAY_HOUR_THRESHOLD = 4.0;

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditLogService auditLogService;

    // =========================================================================================
    // CHECK IN / CHECK OUT (employee, self only)
    // =========================================================================================

    @Transactional
    public AttendanceResponse checkIn(User actor) {
        Employee employee = requireOwnEmployee(actor);
        LocalDate today = LocalDate.now();

        if (attendanceRepository.existsByEmployeeAndDate(employee, today)) {
            throw new InvalidRequestException("You have already checked in today.");
        }

        if (isOnApprovedLeave(employee, today)) {
            throw new InvalidRequestException("You are on approved leave today and cannot check in.");
        }

        LocalTime now = LocalTime.now();
        AttendanceStatus status = now.isAfter(LATE_CUTOFF) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(today)
                .checkInTime(now)
                .status(status)
                .build();

        attendance = attendanceRepository.save(attendance);
        auditLogService.log(actor, "ATTENDANCE_CHECK_IN", "ATTENDANCE",
                employee.getEmployeeId() + " checked in at " + now);

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse checkOut(User actor) {
        Employee employee = requireOwnEmployee(actor);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeAndDate(employee, today)
                .orElseThrow(() -> new InvalidRequestException("You must check in before you can check out."));

        if (attendance.getCheckOutTime() != null) {
            throw new InvalidRequestException("You have already checked out today.");
        }

        LocalTime now = LocalTime.now();
        if (now.isBefore(attendance.getCheckInTime())) {
            throw new InvalidRequestException("Check-out time cannot be before check-in time.");
        }

        double hours = Duration.between(attendance.getCheckInTime(), now).toMinutes() / 60.0;
        attendance.setCheckOutTime(now);
        attendance.setTotalWorkingHours(Math.round(hours * 100.0) / 100.0);

        if (attendance.getStatus() != AttendanceStatus.LATE && hours < HALF_DAY_HOUR_THRESHOLD) {
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        }

        attendance = attendanceRepository.save(attendance);
        auditLogService.log(actor, "ATTENDANCE_CHECK_OUT", "ATTENDANCE",
                employee.getEmployeeId() + " checked out at " + now);

        return toResponse(attendance);
    }

    // =========================================================================================
    // EMPLOYEE SELF-SERVICE READ
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyHistory(User actor, Integer month, Integer year) {
        Employee employee = requireOwnEmployee(actor);
        YearMonth ym = resolveYearMonth(month, year);
        return attendanceRepository
                .findByEmployeeAndDateBetweenOrderByDateDesc(employee, ym.atDay(1), ym.atEndOfMonth())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceMonthlySummaryResponse getMySummary(User actor, Integer month, Integer year) {
        Employee employee = requireOwnEmployee(actor);
        YearMonth ym = resolveYearMonth(month, year);
        List<Attendance> records = attendanceRepository
                .findByEmployeeAndDateBetweenOrderByDateDesc(employee, ym.atDay(1), ym.atEndOfMonth());

        long present = records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long late = records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long half = records.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
        long leave = records.stream().filter(a -> a.getStatus() == AttendanceStatus.ON_LEAVE).count();

        LocalDate rangeEnd = ym.atEndOfMonth().isAfter(LocalDate.now()) ? LocalDate.now() : ym.atEndOfMonth();
        long elapsedWorkingDays = rangeEnd.isBefore(ym.atDay(1)) ? 0 : (ym.atDay(1).until(rangeEnd).getDays() + 1);
        long absent = Math.max(0, elapsedWorkingDays - records.size());

        return AttendanceMonthlySummaryResponse.builder()
                .month(ym.getMonthValue())
                .year(ym.getYear())
                .presentDays(present)
                .lateDays(late)
                .halfDays(half)
                .leaveDays(leave)
                .absentDays(absent)
                .totalRecordedDays(records.size())
                .build();
    }

    // =========================================================================================
    // ADMIN / OFFICER READ
    // =========================================================================================

    @Transactional(readOnly = true)
    public List<AttendanceResponse> search(Long employeeId, Long departmentId, LocalDate date, User actor) {
        Long effectiveDepartmentId = departmentId;

        if (actor.getRole() == Role.OFFICER) {
            Employee officerEmployee = employeeRepository.findByUser(actor).orElse(null);
            if (officerEmployee == null) {
                return List.of();
            }
            Long officerDeptId = officerEmployee.getDepartment().getId();
            if (departmentId != null && !departmentId.equals(officerDeptId)) {
                throw new AccessDeniedException("Officers may only view attendance within their own department.");
            }
            effectiveDepartmentId = officerDeptId;
        }

        return attendanceRepository.search(employeeId, effectiveDepartmentId, date)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TodayAttendanceSummaryResponse getTodaySummary() {
        LocalDate today = LocalDate.now();

        long totalEmployees = employeeRepository.countByStatus(Status.ACTIVE);
        long lateToday = attendanceRepository.countByDateAndStatus(today, AttendanceStatus.LATE);
        long presentToday = lateToday
                + attendanceRepository.countByDateAndStatus(today, AttendanceStatus.PRESENT)
                + attendanceRepository.countByDateAndStatus(today, AttendanceStatus.HALF_DAY);
        long onLeaveToday = leaveRequestRepository.countByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                LeaveStatus.APPROVED, today, today);
        long absentToday = Math.max(0, totalEmployees - presentToday - onLeaveToday);

        return TodayAttendanceSummaryResponse.builder()
                .totalEmployees(totalEmployees)
                .presentToday(presentToday)
                .absentToday(absentToday)
                .onLeaveToday(onLeaveToday)
                .lateToday(lateToday)
                .build();
    }

    // =========================================================================================
    // Internal helpers
    // =========================================================================================

    private Employee requireOwnEmployee(User actor) {
        return employeeRepository.findByUser(actor)
                .orElseThrow(() -> new InvalidRequestException("No employee record is linked to your account. Contact an administrator."));
    }

    private boolean isOnApprovedLeave(Employee employee, LocalDate date) {
        return leaveRequestRepository.existsByEmployeeAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employee, LeaveStatus.APPROVED, date, date);
    }

    private YearMonth resolveYearMonth(Integer month, Integer year) {
        YearMonth current = YearMonth.now();
        int m = (month != null) ? month : current.getMonthValue();
        int y = (year != null) ? year : current.getYear();
        if (m < 1 || m > 12) {
            throw new InvalidRequestException("Month must be between 1 and 12.");
        }
        return YearMonth.of(y, m);
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        Employee employee = attendance.getEmployee();
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .employee(EmployeeSummary.builder()
                        .id(employee.getId())
                        .employeeId(employee.getEmployeeId())
                        .fullName(employee.getFirstName() + " " + employee.getLastName())
                        .designation(employee.getDesignation())
                        .build())
                .department(DepartmentSummary.builder()
                        .id(employee.getDepartment().getId())
                        .departmentCode(employee.getDepartment().getDepartmentCode())
                        .departmentName(employee.getDepartment().getDepartmentName())
                        .status(employee.getDepartment().getStatus().name())
                        .build())
                .date(attendance.getDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .totalWorkingHours(attendance.getTotalWorkingHours())
                .status(attendance.getStatus().name())
                .build();
    }
}
