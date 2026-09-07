package com.tieyoh.api.feed.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

// A configurable calendar entry for the Feed's UPCOMING_HOLIDAY event --
// deliberately data, not hard-coded in the app, per the spec's explicit
// "should eventually be configurable rather than hard-coded" note.
// ruleType is either FIXED_DATE (month + day, e.g. Christmas = 12/25) or
// NTH_WEEKDAY (month + weekday + which occurrence, e.g. Thanksgiving =
// November, Thursday, 4th one) -- several well-known US holidays,
// Thanksgiving among them (the spec's own example), don't fall on a
// fixed date every year, so a plain month/day column alone isn't enough.
@Entity
@Table(name = "holiday_definition")
public class HolidayDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(nullable = false)
    private Integer month;

    private Integer day;

    // ISO-8601 weekday number: Monday = 1 ... Sunday = 7 (matches
    // java.time.DayOfWeek.getValue()).
    private Integer weekday;

    @Column(name = "nth_week")
    private Integer nthWeek;

    @Column(name = "display_message", nullable = false)
    private String displayMessage;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public HolidayDefinition() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Resolves this holiday's actual calendar date in a given year. For
    // FIXED_DATE this is just month/day; for NTH_WEEKDAY it walks forward
    // from the 1st of the month to the first matching weekday, then adds
    // whole weeks to reach the nth occurrence (e.g. 4th Thursday).
    public LocalDate resolveDateForYear(int year) {
        if ("NTH_WEEKDAY".equals(ruleType)) {
            LocalDate firstOfMonth = LocalDate.of(year, month, 1);
            DayOfWeek target = DayOfWeek.of(weekday);
            int daysUntilTarget = (target.getValue() - firstOfMonth.getDayOfWeek().getValue() + 7) % 7;
            LocalDate firstOccurrence = firstOfMonth.plusDays(daysUntilTarget);
            return firstOccurrence.plusWeeks(nthWeek - 1L);
        }
        return LocalDate.of(year, month, day);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getWeekday() {
        return weekday;
    }

    public void setWeekday(Integer weekday) {
        this.weekday = weekday;
    }

    public Integer getNthWeek() {
        return nthWeek;
    }

    public void setNthWeek(Integer nthWeek) {
        this.nthWeek = nthWeek;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
