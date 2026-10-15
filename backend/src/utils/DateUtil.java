package utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DateUtil — date calculation utilities for leave management.
 * Demonstrates Semester III concepts: loops, conditionals, and functions.
 */
public class DateUtil {

    /**
     * Calculate working days between two dates, excluding weekends and holidays.
     * Uses a while loop (Sem III topic) to iterate through dates.
     *
     * @param startDate   start date (YYYY-MM-DD)
     * @param endDate     end date (YYYY-MM-DD)
     * @param halfDayStart whether the start date is a half day
     * @param halfDayEnd   whether the end date is a half day
     * @param holidays    set of holiday dates (YYYY-MM-DD)
     * @return number of working days
     */
    public static double calculateWorkingDays(String startDate, String endDate,
                                               boolean halfDayStart, boolean halfDayEnd,
                                               Set<String> holidays) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        if (start.isAfter(end)) return 0;

        double count = 0;
        LocalDate current = start;

        // Loop through each date — demonstrates while loop
        while (!current.isAfter(end)) {
            DayOfWeek day = current.getDayOfWeek();
            String dateStr = current.toString(); // YYYY-MM-DD format

            // Skip weekends (Saturday and Sunday)
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                // Skip holidays
                if (holidays == null || !holidays.contains(dateStr)) {
                    count++;
                }
            }

            current = current.plusDays(1);
        }

        // Adjust for half days
        if (halfDayStart && count > 0) count -= 0.5;
        if (halfDayEnd && count > 0) count -= 0.5;

        return Math.max(0, count);
    }

    /**
     * Get the current academic year string (e.g., "2026-2027").
     * Academic year starts in June.
     */
    public static String currentAcademicYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (month >= 6) {
            return year + "-" + (year + 1);
        }
        return (year - 1) + "-" + year;
    }

    /**
     * Get today's date as YYYY-MM-DD.
     */
    public static String today() {
        return LocalDate.now().toString();
    }

    /**
     * Get current timestamp as a string.
     */
    public static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Validate that a date string is in YYYY-MM-DD format and is a valid date.
     */
    public static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
