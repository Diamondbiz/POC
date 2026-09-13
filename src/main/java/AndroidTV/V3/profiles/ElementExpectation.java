package AndroidTV.V3.profiles;

/**
 * Immutable description of one expected UI element on a screen.
 */
public class ElementExpectation {

    private final String name;
    private final ElementType type;
    private final String value;

    public ElementExpectation(String name, ElementType type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() { return name; }
    public ElementType getType() { return type; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return name + " [" + type + " = \"" + value + "\"]";
    }
}