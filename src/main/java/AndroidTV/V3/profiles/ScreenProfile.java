package AndroidTV.V3.profiles;

import AndroidTV.V3.core.XmlParser;

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
public class ScreenProfile {

    private final String name;
    private final ElementType markerType;
    private final String markerValue;
    private final List<ElementExpectation> elements;
    private final List<CropExpectation> crops;

    private ScreenProfile(Builder b) {
        this.name = b.name;
        this.markerType = b.markerType;
        this.markerValue = b.markerValue;
        this.elements = Collections.unmodifiableList(new ArrayList<>(b.elements));
        this.crops = Collections.unmodifiableList(new ArrayList<>(b.crops));
    }

    public String getName() { return name; }
    public ElementType getMarkerType() { return markerType; }
    public String getMarkerValue() { return markerValue; }
    public List<ElementExpectation> getElements() { return elements; }
    public List<CropExpectation> getCrops() { return crops; }

    /**
     * Returns true if the screen's marker is present in the given XML.
     */
    public boolean isMarkerPresentIn(String xml, XmlParser parser) {
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
        private ElementType markerType = ElementType.RESOURCE_ID;
        private String markerValue = "";
        private final List<ElementExpectation> elements = new ArrayList<>();
        private final List<CropExpectation> crops = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder marker(ElementType type, String value) {
            this.markerType = type;
            this.markerValue = value;
            return this;
        }

        public Builder element(String elementName, ElementType type, String value) {
            elements.add(new ElementExpectation(elementName, type, value));
            return this;
        }

        public Builder crop(String cropName, int[] bounds, String baselinePath) {
            crops.add(new CropExpectation(cropName, bounds, baselinePath));
            return this;
        }

        public ScreenProfile build() {
            if (markerValue == null || markerValue.isEmpty()) {
                throw new IllegalStateException(
                        "Profile '" + name + "' must declare a marker (marker(type, value)).");
            }
            return new ScreenProfile(this);
        }
    }
}