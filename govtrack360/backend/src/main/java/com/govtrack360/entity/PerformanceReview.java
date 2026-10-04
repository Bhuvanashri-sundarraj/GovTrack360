package com.govtrack360.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A formal performance review for one employee over one period.
 * <p>
 * The final score blends the system-calculated score (from tasks and attendance, snapshotted when the
 * review is saved) with the reviewer's own 1-5 ratings. See PerformanceService for the exact formula.
 */
@Entity
@Table(name = "performance_reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_employee_period",
                columnNames = {"employee_id", "period_start", "period_end"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id", nullable = false)
    private User reviewer;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false)
    private int qualityRating;

    @Column(nullable = false)
    private int teamworkRating;

    @Column(nullable = false)
    private int initiativeRating;

    /** System-calculated score (0-100) at the time the review was saved. Null if there was no data. */
    private Double systemScore;

    /** Blended final score (0-100). */
    @Column(nullable = false)
    private Double finalScore;

    @Column(length = 1000)
    private String strengths;

    @Column(length = 1000)
    private String areasForImprovement;

    @Column(length = 1000)
    private String comments;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
