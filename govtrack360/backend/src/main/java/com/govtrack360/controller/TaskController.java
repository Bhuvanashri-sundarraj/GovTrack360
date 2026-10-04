package com.govtrack360.controller;

import com.govtrack360.dto.task.*;
import com.govtrack360.entity.TaskPriority;
import com.govtrack360.entity.TaskStatus;
import com.govtrack360.exception.InvalidRequestException;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<TaskResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long assignedToId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskStatus taskStatus = parseEnum(status, TaskStatus.class, "status");
        TaskPriority taskPriority = parseEnum(priority, TaskPriority.class, "priority");

        return ResponseEntity.ok(taskService.search(search, taskStatus, taskPriority, departmentId, assignedToId,
                userDetails.getUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.getById(id, userDetails.getUser()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskCreateRequest request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, userDetails.getUser()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.update(id, request, userDetails.getUser()));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<TaskResponse> assign(@PathVariable Long id, @Valid @RequestBody TaskAssignRequest request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.assign(id, request, userDetails.getUser()));
    }

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TaskResponse> updateProgress(@PathVariable Long id, @Valid @RequestBody TaskProgressRequest request,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.updateProgress(id, request, userDetails.getUser()));
    }

    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TaskResponse> submit(@PathVariable Long id, @RequestBody(required = false) TaskSubmitRequest request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        String comment = request != null ? request.getComment() : null;
        return ResponseEntity.ok(taskService.submit(id, comment, userDetails.getUser()));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<TaskResponse> review(@PathVariable Long id, @Valid @RequestBody TaskReviewRequest request,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.review(id, request, userDetails.getUser()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ResponseEntity<TaskResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusChangeRequest request,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.changeStatus(id, request, userDetails.getUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        taskService.delete(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<TaskCommentResponse>> getComments(@PathVariable Long id,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.getComments(id, userDetails.getUser()));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<TaskCommentResponse> addComment(@PathVariable Long id, @Valid @RequestBody TaskCommentRequest request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addComment(id, request, userDetails.getUser()));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER','EMPLOYEE')")
    public ResponseEntity<List<TaskWorkflowHistoryResponse>> getHistory(@PathVariable Long id,
                                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(taskService.getHistory(id, userDetails.getUser()));
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid " + fieldName + " value: " + value);
        }
    }
}
