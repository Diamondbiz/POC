package AndroidTV.V3.validators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Result of running the generic screen validator against one ScreenProfile.
 * Pure data holder. No behavior beyond "did everything pass?" and summaries.
 */
public class ScreenAssertionResult {

    // ==================== CROP RESULT ====================

    public static class CropResult {
        private final boolean passed;
        private final double similarity;

        public CropResult(boolean passed, double similarity) {
            this.passed = passed;
            this.similarity = similarity;
        }

        public boolean isPassed() { return passed; }
        public double getSimilarity() { return similarity; }
    }

    // ==================== FIELDS ====================

    private final String screenName;
    private boolean markerPresent;
    private final Map<String, Boolean> elements = new LinkedHashMap<>();
    private final Map<String, CropResult> cropResults = new LinkedHashMap<>();
    private String failureReason;

    // ==================== CONSTRUCTOR ====================

    public ScreenAssertionResult(String screenName) {
        this.screenName = screenName;
    }

    // ==================== SETTERS (used by the validator as it fills in data) ====================

    public void setMarkerPresent(boolean present) { this.markerPresent = present; }
    public void putElement(String name, boolean found) { elements.put(name, found); }
    public void putCrop(String name, boolean passed, double similarity) {
        cropResults.put(name, new CropResult(passed, similarity));
    }
    public void setFailureReason(String reason) { this.failureReason = reason; }

    // ==================== GETTERS ====================

    public String getScreenName() { return screenName; }
    public boolean isMarkerPresent() { return markerPresent; }
    public Map<String, Boolean> getElements() { return Collections.unmodifiableMap(elements); }
    public Map<String, CropResult> getCropResults() { return Collections.unmodifiableMap(cropResults); }
    public String getFailureReason() { return failureReason; }

    // ==================== SUMMARIES ====================

    public boolean isOverallPassed() {
        if (!markerPresent) return false;
        for (Boolean found : elements.values()) {
            if (!found) return false;
        }
        for (CropResult c : cropResults.values()) {
            if (!c.isPassed()) return false;
        }
        return true;
    }

    public int getElementsFoundCount() {
        int n = 0;
        for (Boolean f : elements.values()) if (f) n++;
        return n;
    }

    public int getElementsTotalCount() {
        return elements.size();
    }

    public int getCropsPassedCount() {
        int n = 0;
        for (CropResult c : cropResults.values()) if (c.isPassed()) n++;
        return n;
    }

    public int getCropsTotalCount() {
        return cropResults.size();
    }

    @Override
    public String toString() {
        return screenName + " — marker=" + markerPresent +
                ", elements=" + getElementsFoundCount() + "/" + getElementsTotalCount() +
                ", crops=" + getCropsPassedCount() + "/" + getCropsTotalCount() +
                ", overall=" + (isOverallPassed() ? "PASS" : "FAIL");
    }
}