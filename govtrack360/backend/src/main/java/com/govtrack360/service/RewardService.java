package com.govtrack360.service;

import com.govtrack360.dto.reward.*;
import com.govtrack360.entity.*;
import com.govtrack360.exception.DuplicateResourceException;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.exception.ResourceNotFoundException;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.RewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Rewards and recognition.
 * <ul>
 *   <li>ADMIN grants to any active employee; OFFICER only to active employees of their own department.</li>
 *   <li>Nobody can reward themself.</li>
 *   <li>Only one EMPLOYEE_OF_THE_MONTH may be granted per department per calendar month.</li>
 *   <li>Every employee can see their own rewards and the organisation-wide recognition wall.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    private static final int MAX_WALL_SIZE = 50;

    private final RewardRepository rewardRepository;
    private final EmployeeRepository employeeRepository;
    private final AccessScopeService scopeService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final SummaryMapper mapper;

    // =========================================================================================
    // GRANT / REVOKE
    // =========================================================================================

    @Transactional
    public RewardResponse grant(RewardRequest request, User actor) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + request.getEmployeeId()));

        Employee own = scopeService.findOwnEmployee(actor).orElse(null);
        if (own != null && own.getId().equals(employee.getId())) {
            throw new AccessDeniedException("You cannot grant a reward to yourself.");
        }
        scopeService.assertCanAccessEmployee(actor, employee);

        if (employee.getStatus() != Status.ACTIVE) {
            throw new InvalidRequestException("Rewards can only be granted to active employees.");
        }

        RewardType type = parseType(request.getType());

        LocalDate awardedDate = (request.getAwardedDate() != null) ? request.getAwardedDate() : LocalDate.now();
        if (awardedDate.isAfter(LocalDate.now())) {
            throw new InvalidRequestException("The award date cannot be in the future.");
        }

        if (type == RewardType.EMPLOYEE_OF_THE_MONTH) {
            YearMonth ym = YearMonth.from(awardedDate);
            long existing = rewardRepository.countByTypeAndDepartmentBetween(
                    type, employee.getDepartment().getId(), ym.atDay(1), ym.atEndOfMonth());
            if (existing > 0) {
                throw new DuplicateResourceException("Employee of the Month has already been awarded in "
                        + employee.getDepartment().getDepartmentName() + " for " + ym + ".");
            }
        }

        Reward reward = Reward.builder()
                .employee(employee)
                .awardedBy(actor)
                .type(type)
                .title(request.getTitle().trim())
                .citation(request.getCitation() == null || request.getCitation().isBlank()
                        ? null : request.getCitation().trim())
                .points(request.getPoints())
                .awardedDate(awardedDate)
                .build();
        reward = rewardRepository.save(reward);

        if (employee.getUser() != null) {
            notificationService.notify(employee.getUser(), "You received a reward: " + reward.getTitle(),
                    type.getLabel() + " (" + reward.getPoints() + " points) was awarded to you.",
                    NotificationType.SYSTEM);
        }
        auditLogService.log(actor, "REWARD_GRANTED", "REWARD",
                "Granted " + type.name() + " (" + reward.getPoints() + " pts) to " + employee.getEmployeeId());

        return toResponse(reward);
    }

    @Transactional
    public void revoke(Long id, User actor) {
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found with id: " + id));

        if (actor.getRole() == Role.OFFICER) {
            scopeService.assertCanAccessEmployee(actor, reward.getEmployee());
            if (!reward.getAwardedBy().getId().equals(actor.getId())) {
                throw new AccessDeniedException("Officers can only revoke rewards that they granted.");
            }
        }

        String code = reward.getEmployee().getEmployeeId();
        rewardRepository.delete(reward);
        auditLogService.log(actor, "REWARD_REVOKED", "REWARD",
                "Revoked " + reward.getType().name() + " (" + reward.getPoints() + " pts) from " + code);
    }

    // =========================================================================================
    // READ
    // =========================================================================================

    /**
     * ADMIN/OFFICER listing. Officers are always limited to their own department.
     */
    @Transactional(readOnly = true)
    public List<RewardResponse> search(User actor, Long employeeId, Long departmentId, String typeValue,
                                       LocalDate from, LocalDate to) {
        Long scopedDepartmentId = scopeService.resolveDepartmentScope(actor, departmentId);
        RewardType type = (typeValue == null || typeValue.isBlank()) ? null : parseType(typeValue);

        List<Reward> rewards = fetch(scopedDepartmentId, from, to);
        return rewards.stream()
                .filter(r -> employeeId == null || r.getEmployee().getId().equals(employeeId))
                .filter(r -> type == null || r.getType() == type)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyRewardsResponse getMine(User actor) {
        Employee employee = scopeService.requireOwnEmployee(actor);
        List<Reward> rewards = rewardRepository.findByEmployeeId(employee.getId());
        long totalPoints = rewards.stream().mapToLong(Reward::getPoints).sum();
        return MyRewardsResponse.builder()
                .totalPoints(totalPoints)
                .totalRewards(rewards.size())
                .rewards(rewards.stream().map(this::toResponse).toList())
                .build();
    }

    /**
     * Organisation-wide recognition wall, visible to every signed-in user.
     */
    @Transactional(readOnly = true)
    public List<RewardResponse> getWall(Integer limit) {
        int size = (limit == null) ? 10 : limit;
        if (size < 1 || size > MAX_WALL_SIZE) {
            throw new InvalidRequestException("Limit must be between 1 and " + MAX_WALL_SIZE + ".");
        }
        return rewardRepository.findRecent(PageRequest.of(0, size)).stream().map(this::toResponse).toList();
    }

    /**
     * Total points per employee (optionally within a date range), highest first.
     */
    @Transactional(readOnly = true)
    public List<RewardLeaderboardEntry> getLeaderboard(User actor, Long departmentId, LocalDate from, LocalDate to) {
        Long scopedDepartmentId = scopeService.resolveDepartmentScope(actor, departmentId);
        List<Reward> rewards = fetch(scopedDepartmentId, from, to);

        Map<Long, long[]> totals = new LinkedHashMap<>(); // employeeId -> [points, count]
        Map<Long, Employee> employees = new HashMap<>();
        for (Reward reward : rewards) {
            Employee employee = reward.getEmployee();
            employees.putIfAbsent(employee.getId(), employee);
            long[] slot = totals.computeIfAbsent(employee.getId(), k -> new long[2]);
            slot[0] += reward.getPoints();
            slot[1] += 1;
        }

        List<Map.Entry<Long, long[]>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort((a, b) -> {
            int byPoints = Long.compare(b.getValue()[0], a.getValue()[0]);
            if (byPoints != 0) return byPoints;
            return mapper.fullName(employees.get(a.getKey())).compareTo(mapper.fullName(employees.get(b.getKey())));
        });

        List<RewardLeaderboardEntry> result = new ArrayList<>();
        int position = 0;
        int rank = 0;
        long previousPoints = -1;
        for (Map.Entry<Long, long[]> entry : sorted) {
            position++;
            if (entry.getValue()[0] != previousPoints) {
                rank = position;
                previousPoints = entry.getValue()[0];
            }
            Employee employee = employees.get(entry.getKey());
            result.add(RewardLeaderboardEntry.builder()
                    .rank(rank)
                    .employee(mapper.employee(employee))
                    .department(mapper.department(employee.getDepartment()))
                    .totalPoints(entry.getValue()[0])
                    .rewardCount(entry.getValue()[1])
                    .build());
        }
        return result;
    }

    public List<RewardTypeResponse> getTypes() {
        return Arrays.stream(RewardType.values())
                .map(t -> RewardTypeResponse.builder()
                        .value(t.name())
                        .label(t.getLabel())
                        .defaultPoints(t.getDefaultPoints())
                        .build())
                .toList();
    }

    // =========================================================================================
    // INTERNAL
    // =========================================================================================

    private List<Reward> fetch(Long departmentId, LocalDate from, LocalDate to) {
        boolean ranged = from != null || to != null;
        if (!ranged) {
            return (departmentId == null)
                    ? rewardRepository.findAllOrdered()
                    : rewardRepository.findByDepartmentId(departmentId);
        }
        // A one-sided range is treated as open-ended on the other side.
        LocalDate start = (from != null) ? from : LocalDate.of(1970, 1, 1);
        LocalDate end = (to != null) ? to : LocalDate.of(2999, 12, 31);
        if (start.isAfter(end)) {
            throw new InvalidRequestException("The 'from' date cannot be after the 'to' date.");
        }
        return (departmentId == null)
                ? rewardRepository.findByAwardedDateBetween(start, end)
                : rewardRepository.findByDepartmentIdAndAwardedDateBetween(departmentId, start, end);
    }

    private RewardType parseType(String value) {
        try {
            return RewardType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid reward type: " + value);
        }
    }

    RewardResponse toResponse(Reward reward) {
        Employee employee = reward.getEmployee();
        return RewardResponse.builder()
                .id(reward.getId())
                .employee(mapper.employee(employee))
                .department(mapper.department(employee.getDepartment()))
                .awardedBy(mapper.user(reward.getAwardedBy()))
                .type(reward.getType().name())
                .typeLabel(reward.getType().getLabel())
                .title(reward.getTitle())
                .citation(reward.getCitation())
                .points(reward.getPoints())
                .awardedDate(reward.getAwardedDate())
                .createdAt(reward.getCreatedAt())
                .build();
    }
}
