package AndroidTV.V3.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central artifact reporting utility.
 * - Ensures folder existence.
 * - Prints clickable file:// and open "..." links.
 * - Writes structured JSON logs via Jackson.
 *
 * Reusable by any test/validator. No dependency on specific screens.
 */
public class ArtifactReporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ==================== FOLDER LINKS ====================

    /**
     * Prints a clickable link for a folder.
     */
    public static void printFolderLink(String label, String folderPath) {
        File dir = new File(folderPath);
        if (!dir.exists()) dir.mkdirs();

        String encoded = folderPath.replace(" ", "%20");
        TestLogger.log("   📁 " + label + ":");
        TestLogger.log("      🔗 file://" + encoded);
        TestLogger.log("      💻 open \"" + folderPath + "\"");
    }

    /**
     * Prints a clickable link for a file, plus its parent folder.
     */
    public static void printFileLink(String label, String filePath) {
        String encoded = filePath.replace(" ", "%20");
        TestLogger.log("   📄 " + label + ":");
        TestLogger.log("      🔗 file://" + encoded);
        TestLogger.log("      💻 open \"" + filePath + "\"");

        File parent = new File(filePath).getParentFile();
        if (parent != null) {
            String parentPath = parent.getAbsolutePath();
            String parentEncoded = parentPath.replace(" ", "%20");
            TestLogger.log("      📁 Folder: file://" + parentEncoded);
            TestLogger.log("      💻 open \"" + parentPath + "\"");
        }
    }

    // ==================== JSON BUILDER ====================

    /**
     * Returns a fresh JSON object node. Callers add fields via .put(...).
     */
    public static ObjectNode newJsonObject() {
        return MAPPER.createObjectNode();
    }

    /**
     * Returns a fresh JSON array node.
     */
    public static ArrayNode newJsonArray() {
        return MAPPER.createArrayNode();
    }

    /**
     * Serializes a JSON node to a pretty-printed file.
     *
     * @param json the JSON node to write
     * @param outputPath absolute path of the JSON file
     */
    public static void writeJson(ObjectNode json, String outputPath) {
        try {
            File out = new File(outputPath);
            if (out.getParentFile() != null) out.getParentFile().mkdirs();

            String pretty = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            java.nio.file.Files.write(java.nio.file.Paths.get(outputPath), pretty.getBytes());

            printFileLink("JSON log", outputPath);
        } catch (Exception e) {
            TestLogger.logWarning("⚠️ Could not write JSON log: " + e.getMessage());
        }
    }

    // ==================== CONVENIENCE ====================

    /**
     * Converts a Map<String,String> into an ObjectNode.
     */
    public static ObjectNode mapToJson(Map<String, String> map) {
        ObjectNode node = MAPPER.createObjectNode();
        for (Map.Entry<String, String> e : map.entrySet()) {
            node.put(e.getKey(), e.getValue());
        }
        return node;
    }

    /**
     * Converts a Map<String,Double> into an ObjectNode.
     */
    public static ObjectNode doubleMapToJson(Map<String, Double> map) {
        ObjectNode node = MAPPER.createObjectNode();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            node.put(e.getKey(), e.getValue());
        }
        return node;
    }

    /**
     * Converts a Map<String,Boolean> into an ObjectNode.
     */
    public static ObjectNode boolMapToJson(Map<String, Boolean> map) {
        ObjectNode node = MAPPER.createObjectNode();
        for (Map.Entry<String, Boolean> e : map.entrySet()) {
            node.put(e.getKey(), e.getValue());
        }
        return node;
    }

    /**
     * Empty map helper to avoid null checks in callers.
     */
    public static <K, V> Map<K, V> emptyMap() {
        return new LinkedHashMap<>();
    }
}