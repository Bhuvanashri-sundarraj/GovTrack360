package com.govtrack360.repository;

import com.govtrack360.entity.Task;
import com.govtrack360.entity.TaskWorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface TaskWorkflowHistoryRepository extends JpaRepository<TaskWorkflowHistory, Long> {
    List<TaskWorkflowHistory> findByTaskOrderByTimestampAsc(Task task);


    // Phase 3 addition: [taskId, count] rows for the given action (e.g. TASK_REJECTED) across many tasks
    @Query("SELECT h.task.id, COUNT(h) FROM TaskWorkflowHistory h WHERE h.task.id IN :taskIds " +
           "AND h.action = :action GROUP BY h.task.id")
    List<Object[]> countByTaskIdsAndAction(@Param("taskIds") Collection<Long> taskIds,
                                            @Param("action") String action);
}
