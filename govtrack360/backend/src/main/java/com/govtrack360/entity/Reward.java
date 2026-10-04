package com.govtrack360.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A recognition / reward granted to an employee by an Admin or Officer.
 */
@Entity
@Table(name = "rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_by_user_id", nullable = false)
    private User awardedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String citation;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private LocalDate awardedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.awardedDate == null) {
            this.awardedDate = LocalDate.now();
        }
    }
}
