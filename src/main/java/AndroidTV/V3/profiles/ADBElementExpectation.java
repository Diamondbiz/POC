package AndroidTV.V3.profiles;

/**
 * Immutable description of one expected UI element on a screen.
 */
public class ADBElementExpectation {

    private final String name;
    private final ADBElementType type;
    private final String value;

    public ADBElementExpectation(String name, ADBElementType type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() { return name; }
    public ADBElementType getType() { return type; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return name + " [" + type + " = \"" + value + "\"]";
    }
}