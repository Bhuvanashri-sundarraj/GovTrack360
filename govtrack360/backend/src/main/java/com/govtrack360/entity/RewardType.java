package com.govtrack360.entity;

/**
 * Categories of recognition. Each type carries a display label and a suggested point value
 * (the API still accepts any value between 1 and 100 so officers have flexibility).
 */
public enum RewardType {
    EMPLOYEE_OF_THE_MONTH("Employee of the Month", 50),
    BEST_PERFORMER("Best Performer", 40),
    EXCELLENCE_AWARD("Excellence Award", 30),
    INNOVATION("Innovation Award", 30),
    TEAM_PLAYER("Team Player", 20),
    PUNCTUALITY("Punctuality Award", 15),
    APPRECIATION("Appreciation", 10);

    private final String label;
    private final int defaultPoints;

    RewardType(String label, int defaultPoints) {
        this.label = label;
        this.defaultPoints = defaultPoints;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultPoints() {
        return defaultPoints;
    }
}
