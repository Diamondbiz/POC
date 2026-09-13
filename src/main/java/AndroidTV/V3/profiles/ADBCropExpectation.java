package AndroidTV.V3.profiles;

/**
 * Immutable description of one crop-and-compare check on a screen.
 * bounds = [x1, y1, x2, y2]
 */
public class ADBCropExpectation {

    private final String name;
    private final int[] bounds;
    private final String expectedBaselinePath;

    public ADBCropExpectation(String name, int[] bounds, String expectedBaselinePath) {
        if (bounds == null || bounds.length != 4) {
            throw new IllegalArgumentException("bounds must be [x1, y1, x2, y2]");
        }
        this.name = name;
        this.bounds = bounds.clone();
        this.expectedBaselinePath = expectedBaselinePath;
    }

    public String getName() { return name; }
    public int[] getBounds() { return bounds.clone(); }
    public String getExpectedBaselinePath() { return expectedBaselinePath; }

    @Override
    public String toString() {
        return name + " [" + bounds[0] + "," + bounds[1] + " → " +
                bounds[2] + "," + bounds[3] + "] → " + expectedBaselinePath;
    }
}