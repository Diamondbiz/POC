package AndroidTV.V3.profiles;

/**
 * How an element expectation is matched against the UI XML.
 */
public enum ElementType {
    /** Matches resource-id="<value>" */
    RESOURCE_ID,

    /** Matches text="<value>" exactly */
    TEXT,

    /** Matches if <value> appears anywhere in the XML (substring) */
    TEXT_PRESENT_ANYWHERE
}