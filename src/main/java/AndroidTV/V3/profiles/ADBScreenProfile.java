package AndroidTV.V3.profiles;

import AndroidTV.V3.core.ADBXmlParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Declarative description of one screen.
 * - Identity: which resource-id or text marks this screen as loaded.
 * - Elements: what must be present.
 * - Crops: what must be compared to baselines (optional).
 *
 * Profiles are data. They contain no behavior beyond "is my marker present?".
 */
public class ADBScreenProfile {

    private final String name;
    private final ADBElementType markerType;
    private final String markerValue;
    private final List<ADBElementExpectation> elements;
    private final List<ADBCropExpectation> crops;

    private ADBScreenProfile(Builder b) {
        this.name = b.name;
        this.markerType = b.markerType;
        this.markerValue = b.markerValue;
        this.elements = Collections.unmodifiableList(new ArrayList<>(b.elements));
        this.crops = Collections.unmodifiableList(new ArrayList<>(b.crops));
    }

    public String getName() { return name; }
    public ADBElementType getMarkerType() { return markerType; }
    public String getMarkerValue() { return markerValue; }
    public List<ADBElementExpectation> getElements() { return elements; }
    public List<ADBCropExpectation> getCrops() { return crops; }

    /**
     * Returns true if the screen's marker is present in the given XML.
     */
    public boolean isMarkerPresentIn(String xml, ADBXmlParser parser) {
        switch (markerType) {
            case RESOURCE_ID:
                return parser.containsResourceId(xml, markerValue);
            case TEXT:
                return xml.contains("text=\"" + markerValue + "\"");
            case TEXT_PRESENT_ANYWHERE:
                return xml.contains(markerValue);
            default:
                return false;
        }
    }

    // ==================== BUILDER ====================

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private ADBElementType markerType = ADBElementType.RESOURCE_ID;
        private String markerValue = "";
        private final List<ADBElementExpectation> elements = new ArrayList<>();
        private final List<ADBCropExpectation> crops = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder marker(ADBElementType type, String value) {
            this.markerType = type;
            this.markerValue = value;
            return this;
        }

        public Builder element(String elementName, ADBElementType type, String value) {
            elements.add(new ADBElementExpectation(elementName, type, value));
            return this;
        }

        public Builder crop(String cropName, int[] bounds, String baselinePath) {
            crops.add(new ADBCropExpectation(cropName, bounds, baselinePath));
            return this;
        }

        public ADBScreenProfile build() {
            if (markerValue == null || markerValue.isEmpty()) {
                throw new IllegalStateException(
                        "Profile '" + name + "' must declare a marker (marker(type, value)).");
            }
            return new ADBScreenProfile(this);
        }
    }
}