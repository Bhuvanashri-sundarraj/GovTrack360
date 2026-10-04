package com.govtrack360.service;

import com.govtrack360.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A validated, fully-resolved [from, to] date range. Used by every Phase 3 endpoint that takes optional
 * from/to parameters so defaults and validation are identical everywhere.
 */
public final class DateRange {

    public static final int DEFAULT_DAYS = 90;
    public static final int MAX_DAYS = 366;

    private final LocalDate from;
    private final LocalDate to;

    private DateRange(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public LocalDate from() {
        return from;
    }

    public LocalDate to() {
        return to;
    }

    /**
     * Defaults: to = today, from = to minus 89 days (a rolling 90-day window).
     */
    public static DateRange resolve(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = (to != null) ? to : LocalDate.now();
        LocalDate resolvedFrom = (from != null) ? from : resolvedTo.minusDays(DEFAULT_DAYS - 1L);
        return validated(resolvedFrom, resolvedTo);
    }

    public static DateRange of(LocalDate from, LocalDate to) {
        return validated(from, to);
    }

    private static DateRange validated(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new InvalidRequestException("The 'from' date cannot be after the 'to' date.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidRequestException("The date range cannot exceed " + MAX_DAYS + " days.");
        }
        return new DateRange(from, to);
    }
}
