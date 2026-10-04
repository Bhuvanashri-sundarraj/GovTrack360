package com.govtrack360.dto.report;

public enum ReportType {
    TASK_SUMMARY("Task Summary",
            "Every task due in the period with its status, assignee and progress."),
    EMPLOYEE_PERFORMANCE("Employee Performance",
            "Ranked performance scores with task and attendance breakdowns for each employee."),
    ATTENDANCE("Attendance",
            "Working days, attendance counts and attendance rate for each employee."),
    LEAVE("Leave",
            "Leave requests starting in the period with their status and duration."),
    DEPARTMENT_SUMMARY("Department Summary",
            "Side-by-side comparison of departments: tasks, attendance, performance and rewards."),
    REWARDS("Rewards & Recognition",
            "Rewards granted in the period, with points and who awarded them.");

    private final String label;
    private final String description;

    ReportType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
