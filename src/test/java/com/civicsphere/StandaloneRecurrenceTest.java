package com.civicsphere;

import com.civicsphere.model.Complaint;
import com.civicsphere.service.RecurrenceService;
import com.civicsphere.util.HaversineDistance;
import com.civicsphere.util.TextSimilarity;

import java.math.BigDecimal;

/**
 * Standalone test runner for verification of the Recurrence Engine.
 */
public class StandaloneRecurrenceTest {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println("  CIVICSPHERE: RECURRENCE ENGINE & ALGORITHM VERIFICATION SUITE");
        System.out.println("=====================================================================\n");

        int passed = 0;
        int failed = 0;

        RecurrenceService recurrenceService = new RecurrenceService(null, null);

        // -------------------------------------------------------------
        // TEST 1: Haversine Formula Distance Verification
        // -------------------------------------------------------------
        System.out.print("[TEST 1] Haversine Distance (17.4938, 78.3995) to (17.4948, 78.3995)... ");
        double dist = HaversineDistance.calculateDistanceMeters(17.4938, 78.3995, 17.4948, 78.3995);
        if (dist > 100 && dist < 120) {
            System.out.printf("PASSED (Distance = %.2f meters)%n", dist);
            passed++;
        } else {
            System.out.printf("FAILED (Expected ~111m, got %.2f)%n", dist);
            failed++;
        }

        // -------------------------------------------------------------
        // TEST 2: Text Normalization and Jaccard Keyword Overlap
        // -------------------------------------------------------------
        System.out.print("[TEST 2] Text Keyword Overlap Similarity... ");
        String text1 = "Severe pothole near metro pillar 1142 causing accidents and skidding";
        String text2 = "The pothole near metro pillar 1142 has reopened after rain";
        double sim = TextSimilarity.jaccardSimilarity(text1, text2);
        if (sim > 0.3) {
            System.out.printf("PASSED (Jaccard Similarity = %.2f%%)%n", sim * 100);
            passed++;
        } else {
            System.out.printf("FAILED (Expected > 30%%, got %.2f%%)%n", sim * 100);
            failed++;
        }

        // -------------------------------------------------------------
        // TEST 3: High Recurrence Scenario (Kukatpally Pothole)
        // -------------------------------------------------------------
        System.out.println("\n[TEST 3] Evaluating High Recurrence Case:");
        System.out.println("         Complaint A: Road Damage, Kukatpally, Pillar 1142, RESOLVED");
        System.out.println("         Complaint B: Road Damage, Kukatpally, Pillar 1142, SUBMITTED");

        Complaint cA = new Complaint();
        cA.setComplaintId(101);
        cA.setCategoryId(1);
        cA.setTitle("Severe Pothole on Main Road near Metro Station");
        cA.setDescription("A deep crater has formed near pillar 1142 causing dangerous accidents.");
        cA.setArea("Kukatpally");
        cA.setCity("Hyderabad");
        cA.setState("Telangana");
        cA.setLandmark("Near Metro Pillar 1142");
        cA.setLatitude(new BigDecimal("17.49380000"));
        cA.setLongitude(new BigDecimal("78.39950000"));
        cA.setStatus("RESOLVED");

        Complaint cB = new Complaint();
        cB.setComplaintId(145);
        cB.setCategoryId(1);
        cB.setTitle("Pothole reappeared at metro pillar 1142");
        cB.setDescription("The pothole repaired last week has cracked open again near metro pillar 1142.");
        cB.setArea("Kukatpally");
        cB.setCity("Hyderabad");
        cB.setState("Telangana");
        cB.setLandmark("Metro Pillar 1142");
        cB.setLatitude(new BigDecimal("17.49388000")); // ~10-15 meters away
        cB.setLongitude(new BigDecimal("78.39952000"));
        cB.setStatus("SUBMITTED");

        RecurrenceService.RecurrenceScoreResult scoreResult = recurrenceService.calculateRecurrenceScore(cB, cA);

        System.out.printf("         -> Category Match Score:    %.1f / 30%n", scoreResult.categoryScore);
        System.out.printf("         -> Location Match Score:    %.1f / 40%n", scoreResult.locationScore);
        System.out.printf("         -> Description Match Score: %.1f / 20%n", scoreResult.descriptionScore);
        System.out.printf("         -> Resolved Status Bonus:   %.1f / 10%n", scoreResult.resolvedScore);
        System.out.printf("         -> TOTAL MATCHING SCORE:    %.1f / 100%n", scoreResult.totalScore);
        System.out.println("         -> Matching Rationale:      " + scoreResult.reasonSummary);

        if (scoreResult.totalScore >= 70.0) {
            System.out.println("         RESULT: PASSED (Possible Recurrence Detected >= 70%)");
            passed++;
        } else {
            System.out.println("         RESULT: FAILED (Expected score >= 70%, got " + scoreResult.totalScore + "%)");
            failed++;
        }

        // -------------------------------------------------------------
        // TEST 4: False Positive Prevention (Different Category)
        // -------------------------------------------------------------
        System.out.println("\n[TEST 4] Evaluating False Positive Prevention (Different Category at Same Spot):");
        Complaint cC = new Complaint();
        cC.setComplaintId(146);
        cC.setCategoryId(5); // Streetlight failure (different from Road Damage)
        cC.setTitle("Streetlight pole fused");
        cC.setDescription("The electric lamp on pillar 1142 is not working at night.");
        cC.setArea("Kukatpally");
        cC.setCity("Hyderabad");
        cC.setLatitude(new BigDecimal("17.49380000"));
        cC.setLongitude(new BigDecimal("78.39950000"));
        cC.setStatus("SUBMITTED");

        RecurrenceService.RecurrenceScoreResult falseCategory = recurrenceService.calculateRecurrenceScore(cC, cA);
        System.out.printf("         -> Score: %.1f / 100%n", falseCategory.totalScore);
        if (falseCategory.totalScore < 70.0) {
            System.out.println("         RESULT: PASSED (Correctly rejected: score < 70%)");
            passed++;
        } else {
            System.out.println("         RESULT: FAILED (False positive triggered)");
            failed++;
        }

        // -------------------------------------------------------------
        // TEST 5: False Positive Prevention (Different Location)
        // -------------------------------------------------------------
        System.out.println("\n[TEST 5] Evaluating False Positive Prevention (Same Category in Different City):");
        Complaint cD = new Complaint();
        cD.setComplaintId(147);
        cD.setCategoryId(1); // Road Damage
        cD.setTitle("Pothole on ring road");
        cD.setDescription("Pothole on service road near junction.");
        cD.setArea("Whitefield");
        cD.setCity("Bengaluru");
        cD.setStatus("SUBMITTED");

        RecurrenceService.RecurrenceScoreResult falseLocation = recurrenceService.calculateRecurrenceScore(cD, cA);
        System.out.printf("         -> Score: %.1f / 100%n", falseLocation.totalScore);
        if (falseLocation.totalScore < 70.0) {
            System.out.println("         RESULT: PASSED (Correctly rejected: score < 70%)");
            passed++;
        } else {
            System.out.println("         RESULT: FAILED (False positive triggered)");
            failed++;
        }

        // -------------------------------------------------------------
        // Summary
        // -------------------------------------------------------------
        System.out.println("\n=====================================================================");
        System.out.printf("  TEST EXECUTION SUMMARY: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("=====================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}

