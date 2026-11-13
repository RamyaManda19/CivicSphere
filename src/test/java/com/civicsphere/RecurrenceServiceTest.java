package com.civicsphere;

import com.civicsphere.model.Complaint;
import com.civicsphere.service.RecurrenceService;
import com.civicsphere.util.HaversineDistance;
import com.civicsphere.util.TextSimilarity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying the Recurrence Detection Scoring Engine.
 */
public class RecurrenceServiceTest {

    private RecurrenceService recurrenceService;

    @BeforeEach
    public void setup() {
        // Instantiate service without DAOs for pure logic testing
        recurrenceService = new RecurrenceService(null, null);
    }

    @Test
    @DisplayName("Should detect high recurrence score (>=70) for identical problem at same GPS location")
    public void testHighRecurrenceMatch() {
        // Original resolved complaint
        Complaint orig = new Complaint();
        orig.setComplaintId(101);
        orig.setCategoryId(1); // Road Damage
        orig.setTitle("Deep pothole near metro pillar 1142");
        orig.setDescription("A massive dangerous pothole has formed in the middle lane causing accidents.");
        orig.setArea("Kukatpally");
        orig.setCity("Hyderabad");
        orig.setState("Telangana");
        orig.setLandmark("Metro Pillar 1142");
        orig.setLatitude(new BigDecimal("17.493800"));
        orig.setLongitude(new BigDecimal("78.399500"));
        orig.setStatus("RESOLVED");

        // New complaint submitted 10 days later at nearly the exact same spot (within 30 meters)
        Complaint newC = new Complaint();
        newC.setComplaintId(145);
        newC.setCategoryId(1); // Same category
        newC.setTitle("Pothole reappeared near pillar 1142");
        newC.setDescription("The pothole repaired last week has cracked open again near metro pillar 1142.");
        newC.setArea("Kukatpally");
        newC.setCity("Hyderabad");
        newC.setState("Telangana");
        newC.setLandmark("Metro Pillar 1142");
        newC.setLatitude(new BigDecimal("17.493900")); // ~15-20 meters away
        newC.setLongitude(new BigDecimal("78.399550"));
        newC.setStatus("SUBMITTED");

        RecurrenceService.RecurrenceScoreResult result = recurrenceService.calculateRecurrenceScore(newC, orig);

        System.out.println("=== High Match Recurrence Test ===");
        System.out.println("Total Score: " + result.totalScore);
        System.out.println("Category: " + result.categoryScore + "/30");
        System.out.println("Location: " + result.locationScore + "/40");
        System.out.println("Description: " + result.descriptionScore + "/20");
        System.out.println("Resolved Bonus: " + result.resolvedScore + "/10");
        System.out.println("Reason: " + result.reasonSummary);

        assertEquals(30.0, result.categoryScore, "Category match should be 30 pts");
        assertEquals(40.0, result.locationScore, "Location proximity <= 100m should be 40 pts");
        assertTrue(result.descriptionScore >= 10.0, "Description keyword overlap should score at least 10 pts");
        assertEquals(10.0, result.resolvedScore, "Previous resolved status should give 10 pts");
        assertTrue(result.totalScore >= 70.0, "Total score should exceed recurrence threshold (>=70%)");
    }

    @Test
    @DisplayName("Should NOT flag recurrence if category is different even at exact same location")
    public void testDifferentCategoryNoRecurrence() {
        Complaint orig = new Complaint();
        orig.setComplaintId(1);
        orig.setCategoryId(1); // Road Damage
        orig.setCity("Hyderabad");
        orig.setArea("Kukatpally");
        orig.setStatus("RESOLVED");
        orig.setTitle("Pothole on main road");
        orig.setDescription("Crater in road surface.");

        Complaint newC = new Complaint();
        newC.setComplaintId(2);
        newC.setCategoryId(5); // Streetlight failure
        newC.setCity("Hyderabad");
        newC.setArea("Kukatpally");
        newC.setStatus("SUBMITTED");
        newC.setTitle("Streetlight pole not functioning");
        newC.setDescription("The electric lamp on pole is fused and dark at night.");

        RecurrenceService.RecurrenceScoreResult result = recurrenceService.calculateRecurrenceScore(newC, orig);

        System.out.println("=== Different Category Test Total Score: " + result.totalScore + " ===");
        assertEquals(0.0, result.categoryScore, "Category score should be 0 for different categories");
        assertTrue(result.totalScore < 70.0, "Different category should not trigger recurrence (score < 70)");
    }

    @Test
    @DisplayName("Should correctly calculate Haversine geographical distance in meters")
    public void testHaversineDistance() {
        // Two points ~111 meters apart in Hyderabad
        double lat1 = 17.493800;
        double lon1 = 78.399500;
        double lat2 = 17.494800; // ~0.001 deg lat is ~111 meters
        double lon2 = 78.399500;

        double dist = HaversineDistance.calculateDistanceMeters(lat1, lon1, lat2, lon2);
        System.out.println("Calculated distance: " + dist + " meters");
        assertTrue(dist > 100.0 && dist < 120.0, "Distance should be approx 111 meters");
    }

    @Test
    @DisplayName("Should extract keyword tokens and compute Jaccard similarity")
    public void testTextSimilarity() {
        String s1 = "Deep hazardous pothole in the road causing accidents";
        String s2 = "Accidents caused by road pothole and crater";

        double sim = TextSimilarity.jaccardSimilarity(s1, s2);
        System.out.println("Jaccard similarity: " + sim);
        assertTrue(sim > 0.25, "Keyword similarity should detect overlapping civic terms");
    }
}

