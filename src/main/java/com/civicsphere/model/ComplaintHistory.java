package com.civicsphere.model;

import java.sql.Timestamp;

/**
 * Audit history tracking every status change and remark for a complaint.
 */
public class ComplaintHistory {

    private int historyId;
    private int complaintId;
    private String previousStatus;
    private String newStatus;
    private String remarks;
    private int updatedBy;
    private String updatedByName;
    private String updatedByRole;
    private Timestamp updatedAt;

    public ComplaintHistory() {}

    public ComplaintHistory(int historyId, int complaintId, String previousStatus, String newStatus, String remarks, int updatedBy, Timestamp updatedAt) {
        this.historyId = historyId;
        this.complaintId = complaintId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(int complaintId) {
        this.complaintId = complaintId;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public String getUpdatedByRole() {
        return updatedByRole;
    }

    public void setUpdatedByRole(String updatedByRole) {
        this.updatedByRole = updatedByRole;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

