package com.civicsphere.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entity representing an identified recurrence link between an original complaint
 * and a newly submitted complaint.
 */
public class RecurrenceRecord {

    private int recurrenceId;
    private int originalComplaintId;
    private int recurringComplaintId;
    private BigDecimal matchingScore;
    private String matchingReason;
    private String verificationStatus; // "PENDING", "CONFIRMED", "REJECTED"
    private Integer verifiedBy;
    private String verifiedByName;
    private Timestamp verifiedAt;
    private Timestamp createdAt;

    // View model details for dashboard / recurrence review
    private Complaint originalComplaint;
    private Complaint recurringComplaint;

    public RecurrenceRecord() {}

    public int getRecurrenceId() {
        return recurrenceId;
    }

    public void setRecurrenceId(int recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    public int getOriginalComplaintId() {
        return originalComplaintId;
    }

    public void setOriginalComplaintId(int originalComplaintId) {
        this.originalComplaintId = originalComplaintId;
    }

    public int getRecurringComplaintId() {
        return recurringComplaintId;
    }

    public void setRecurringComplaintId(int recurringComplaintId) {
        this.recurringComplaintId = recurringComplaintId;
    }

    public BigDecimal getMatchingScore() {
        return matchingScore;
    }

    public void setMatchingScore(BigDecimal matchingScore) {
        this.matchingScore = matchingScore;
    }

    public String getMatchingReason() {
        return matchingReason;
    }

    public void setMatchingReason(String matchingReason) {
        this.matchingReason = matchingReason;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Integer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Integer verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public void setVerifiedByName(String verifiedByName) {
        this.verifiedByName = verifiedByName;
    }

    public Timestamp getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Timestamp verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Complaint getOriginalComplaint() {
        return originalComplaint;
    }

    public void setOriginalComplaint(Complaint originalComplaint) {
        this.originalComplaint = originalComplaint;
    }

    public Complaint getRecurringComplaint() {
        return recurringComplaint;
    }

    public void setRecurringComplaint(Complaint recurringComplaint) {
        this.recurringComplaint = recurringComplaint;
    }
}

