package com.civicsphere.service;

import com.civicsphere.config.AppConfig;
import com.civicsphere.dao.ComplaintDAO;
import com.civicsphere.dao.RecurrenceDAO;
import com.civicsphere.model.Complaint;
import com.civicsphere.model.RecurrenceRecord;
import com.civicsphere.util.HaversineDistance;
import com.civicsphere.util.TextSimilarity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core Engine for Recurring Civic Issue Detection and Accountability.
 * Compares incoming complaints against historical resolved/closed complaints
 * to detect chronic infrastructure failures.
 */
public class RecurrenceService {

    private static final Logger LOGGER = Logger.getLogger(RecurrenceService.class.getName());

    private final ComplaintDAO complaintDAO;
    private final RecurrenceDAO recurrenceDAO;

    public RecurrenceService() {
        this.complaintDAO = new ComplaintDAO();
        this.recurrenceDAO = new RecurrenceDAO();
    }

    public RecurrenceService(ComplaintDAO complaintDAO, RecurrenceDAO recurrenceDAO) {
        this.complaintDAO = complaintDAO;
        this.recurrenceDAO = recurrenceDAO;
    }

    /**
     * Evaluates a newly submitted complaint against past resolved complaints.
     * Identifies potential recurrences based on category, location, description, and status.
     *
     * @param newComplaint The new complaint
     * @return List of newly detected RecurrenceRecords (may be empty if no match exceeds threshold)
     */
    public List<RecurrenceRecord> evaluateRecurrence(Complaint newComplaint) {
        List<RecurrenceRecord> detected = new ArrayList<>();
        if (newComplaint == null || newComplaint.getComplaintId() <= 0) {
            return detected;
        }

        // Fetch candidate historical complaints that are RESOLVED or CLOSED
        List<Complaint> candidates = complaintDAO.findResolvedAndClosedComplaints(newComplaint.getCategoryId());

        double threshold = AppConfig.getRecurrenceScoreThreshold(); // Default 70.0
        double bestScore = 0.0;
        Complaint bestMatch = null;
        String bestReason = null;

        for (Complaint past : candidates) {
            // Cannot be recurring to itself
            if (past.getComplaintId() == newComplaint.getComplaintId()) {
                continue;
            }

            // Already linked?
            if (recurrenceDAO.exists(past.getComplaintId(), newComplaint.getComplaintId())) {
                continue;
            }

            RecurrenceScoreResult result = calculateRecurrenceScore(newComplaint, past);

            if (result.totalScore >= threshold) {
                if (result.totalScore > bestScore) {
                    bestScore = result.totalScore;
                    bestMatch = past;
                    bestReason = result.reasonSummary;
                }

                // Create a recurrence record for this match
                RecurrenceRecord rec = new RecurrenceRecord();
                rec.setOriginalComplaintId(past.getComplaintId());
                rec.setRecurringComplaintId(newComplaint.getComplaintId());
                rec.setMatchingScore(BigDecimal.valueOf(result.totalScore).setScale(2, RoundingMode.HALF_UP));
                rec.setMatchingReason(result.reasonSummary);
                rec.setVerificationStatus("PENDING");

                boolean created = recurrenceDAO.createRecurrenceRecord(rec);
                if (created) {
                    detected.add(rec);
                    LOGGER.info("Possible Recurring Issue detected: Complaint #" + newComplaint.getComplaintId() +
                                " matches Resolved Complaint #" + past.getComplaintId() +
                                " with score " + result.totalScore + "%");
                }
            }
        }

        // Link the best matching complaint as parent_complaint_id
        if (bestMatch != null && newComplaint.getParentComplaintId() == null) {
            complaintDAO.updateParentComplaintId(newComplaint.getComplaintId(), bestMatch.getComplaintId());
            newComplaint.setParentComplaintId(bestMatch.getComplaintId());
        }

        return detected;
    }

    /**
     * Calculates the recurrence matching score (0 to 100) and rationale.
     */
    public RecurrenceScoreResult calculateRecurrenceScore(Complaint newC, Complaint pastC) {
        double categoryScore = 0.0;
        double locationScore = 0.0;
        double descriptionScore = 0.0;
        double resolvedScore = 0.0;
        List<String> reasons = new ArrayList<>();

        // 1. Category Match (30 points)
        if (newC.getCategoryId() == pastC.getCategoryId()) {
            categoryScore = 30.0;
            reasons.add("Category Match (+30 pts)");
        }

        // 2. Location Match (40 points)
        boolean hasCoordinates = newC.getLatitude() != null && newC.getLongitude() != null &&
                                pastC.getLatitude() != null && pastC.getLongitude() != null;

        if (hasCoordinates) {
            double distanceMeters = HaversineDistance.calculateDistanceMeters(
                    newC.getLatitude().doubleValue(), newC.getLongitude().doubleValue(),
                    pastC.getLatitude().doubleValue(), pastC.getLongitude().doubleValue()
            );

            double maxDistance = AppConfig.getDistanceThresholdMeters(); // e.g. 100m
            if (distanceMeters <= maxDistance) {
                locationScore = 40.0;
                reasons.add(String.format("GPS Proximity: %.1fm <= %.0fm (+40 pts)", distanceMeters, maxDistance));
            } else if (distanceMeters <= maxDistance * 2.5) {
                locationScore = 25.0;
                reasons.add(String.format("GPS Proximity: %.1fm nearby (+25 pts)", distanceMeters));
            } else if (distanceMeters <= maxDistance * 5.0) {
                locationScore = 15.0;
                reasons.add(String.format("GPS Proximity: %.1fm in vicinity (+15 pts)", distanceMeters));
            }
        }

        // If coordinates not matched or missing, fallback to area & city normalization
        if (locationScore == 0.0) {
            boolean cityMatches = TextSimilarity.isLocationMatch(newC.getCity(), pastC.getCity());
            boolean areaMatches = TextSimilarity.isLocationMatch(newC.getArea(), pastC.getArea());

            if (cityMatches && areaMatches) {
                locationScore = 30.0;
                reasons.add(String.format("Area & City Match: '%s, %s' (+30 pts)", newC.getArea(), newC.getCity()));

                // Bonus for landmark overlap (up to 10 pts)
                if (newC.getLandmark() != null && pastC.getLandmark() != null) {
                    double landmarkSim = TextSimilarity.jaccardSimilarity(newC.getLandmark(), pastC.getLandmark());
                    if (landmarkSim > 0.2 || TextSimilarity.isLocationMatch(newC.getLandmark(), pastC.getLandmark())) {
                        locationScore += 10.0;
                        reasons.add("Landmark Match: '" + newC.getLandmark() + "' (+10 pts)");
                    }
                }
            } else if (areaMatches) {
                locationScore = 20.0;
                reasons.add("Area Match: '" + newC.getArea() + "' (+20 pts)");
            }
        }

        // 3. Description Similarity (20 points)
        double textSim = TextSimilarity.jaccardSimilarity(newC.getDescription() + " " + newC.getTitle(),
                                                          pastC.getDescription() + " " + pastC.getTitle());
        if (textSim >= 0.40) {
            descriptionScore = 20.0;
            reasons.add(String.format("Description Keyword Match: %.0f%% overlap (+20 pts)", textSim * 100));
        } else if (textSim >= 0.20) {
            descriptionScore = 15.0;
            reasons.add(String.format("Description Similarity: %.0f%% overlap (+15 pts)", textSim * 100));
        } else if (textSim > 0.05) {
            descriptionScore = Math.min(20.0, Math.round(textSim * 50.0));
            reasons.add(String.format("Partial Description Match: %.0f%% overlap (+%.0f pts)", textSim * 100, descriptionScore));
        }

        // 4. Previous Complaint Resolved (10 points)
        if ("RESOLVED".equalsIgnoreCase(pastC.getStatus()) || "CLOSED".equalsIgnoreCase(pastC.getStatus())) {
            resolvedScore = 10.0;
            reasons.add("Previous Complaint Status: " + pastC.getStatus() + " (+10 pts)");
        }

        double total = categoryScore + locationScore + descriptionScore + resolvedScore;
        total = Math.min(100.0, Math.max(0.0, total));

        RecurrenceScoreResult res = new RecurrenceScoreResult();
        res.totalScore = total;
        res.categoryScore = categoryScore;
        res.locationScore = locationScore;
        res.descriptionScore = descriptionScore;
        res.resolvedScore = resolvedScore;
        res.reasonSummary = String.join("; ", reasons);

        return res;
    }

    /**
     * Allows a citizen to report an issue recurring after a previous resolution.
     */
    public RecurrenceRecord reportCitizenRecurrence(int originalComplaintId, Complaint newComplaint) {
        Complaint original = complaintDAO.findById(originalComplaintId);
        if (original == null) {
            throw new IllegalArgumentException("Original complaint #" + originalComplaintId + " not found.");
        }

        // Ensure category and location are aligned if missing in new report
        if (newComplaint.getCategoryId() <= 0) {
            newComplaint.setCategoryId(original.getCategoryId());
        }
        if (newComplaint.getCity() == null || newComplaint.getCity().isEmpty()) {
            newComplaint.setCity(original.getCity());
        }
        if (newComplaint.getArea() == null || newComplaint.getArea().isEmpty()) {
            newComplaint.setArea(original.getArea());
        }
        if (newComplaint.getState() == null || newComplaint.getState().isEmpty()) {
            newComplaint.setState(original.getState());
        }
        newComplaint.setParentComplaintId(originalComplaintId);

        // Insert new complaint
        int newId = complaintDAO.createComplaint(newComplaint);
        if (newId <= 0) {
            throw new RuntimeException("Failed to submit recurrence complaint.");
        }

        // Calculate score
        RecurrenceScoreResult scoreResult = calculateRecurrenceScore(newComplaint, original);
        // Explicit reporting gets an immediate high confidence minimum score
        double score = Math.max(85.0, scoreResult.totalScore);

        String reasons = "Citizen Explicit Recurrence Report; " + scoreResult.reasonSummary;

        RecurrenceRecord rec = new RecurrenceRecord();
        rec.setOriginalComplaintId(originalComplaintId);
        rec.setRecurringComplaintId(newId);
        rec.setMatchingScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
        rec.setMatchingReason(reasons);
        rec.setVerificationStatus("PENDING");

        recurrenceDAO.createRecurrenceRecord(rec);
        return rec;
    }

    public List<RecurrenceRecord> getAllRecurrenceRecords(String verificationStatus) {
        return recurrenceDAO.findAll(verificationStatus);
    }

    public RecurrenceRecord getRecurrenceById(int recurrenceId) {
        return recurrenceDAO.findById(recurrenceId);
    }

    public boolean verifyRecurrence(int recurrenceId, String newStatus, int verifiedBy) {
        if (!"CONFIRMED".equalsIgnoreCase(newStatus) && !"REJECTED".equalsIgnoreCase(newStatus)) {
            throw new IllegalArgumentException("Verification status must be either CONFIRMED or REJECTED.");
        }
        return recurrenceDAO.verifyRecurrence(recurrenceId, newStatus.toUpperCase(), verifiedBy);
    }

    public Map<String, Integer> getRecurrenceStatistics() {
        return recurrenceDAO.getRecurrenceStats();
    }

    public static class RecurrenceScoreResult {
        public double totalScore;
        public double categoryScore;
        public double locationScore;
        public double descriptionScore;
        public double resolvedScore;
        public String reasonSummary;
    }
}

