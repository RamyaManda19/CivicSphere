package com.civicsphere.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Complaint entity for citizen reports and recurrence tracking.
 */
public class Complaint {

    private int complaintId;
    private int userId;
    private int categoryId;
    private Integer departmentId;
    private Integer parentComplaintId;

    private String title;
    private String description;
    private String area;
    private String city;
    private String state;
    private String landmark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imagePath;
    private String status; // SUBMITTED, UNDER REVIEW, ASSIGNED, IN PROGRESS, RESOLVED, CLOSED
    private Timestamp submittedAt;
    private Timestamp resolvedAt;
    private String resolutionRemarks;
    private String resolutionImagePath;

    // View-helper joined fields
    private String userName;
    private String userEmail;
    private String userPhone;
    private String categoryName;
    private String departmentName;
    private String parentComplaintTitle;
    private boolean isRecurring;
    private Double recurrenceScore;

    public Complaint() {}

    public int getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(int complaintId) {
        this.complaintId = complaintId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getParentComplaintId() {
        return parentComplaintId;
    }

    public void setParentComplaintId(Integer parentComplaintId) {
        this.parentComplaintId = parentComplaintId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionRemarks() {
        return resolutionRemarks;
    }

    public void setResolutionRemarks(String resolutionRemarks) {
        this.resolutionRemarks = resolutionRemarks;
    }

    public String getResolutionImagePath() {
        return resolutionImagePath;
    }

    public void setResolutionImagePath(String resolutionImagePath) {
        this.resolutionImagePath = resolutionImagePath;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getParentComplaintTitle() {
        return parentComplaintTitle;
    }

    public void setParentComplaintTitle(String parentComplaintTitle) {
        this.parentComplaintTitle = parentComplaintTitle;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public Double getRecurrenceScore() {
        return recurrenceScore;
    }

    public void setRecurrenceScore(Double recurrenceScore) {
        this.recurrenceScore = recurrenceScore;
    }
}

