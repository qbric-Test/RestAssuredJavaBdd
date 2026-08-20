package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Jackson-backed serialisation and deserialisation helpers.
 *
 * <p>One shared, pre-configured {@link ObjectMapper} is used everywhere. Building
 * a mapper per call is a common and expensive mistake — construction is heavy,
 * and separate instances drift out of configuration with each other.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = buildMapper();

    private JsonUtils() {
        // Utility class.
    }

    private static ObjectMapper buildMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // An unexpected field from the API must not fail the mapping; the
                // suite should assert on what it cares about, not on the absence
                // of everything else.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * @return the shared, configured mapper
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    // ------------------------------------------------------------------
    // Serialisation
    // ------------------------------------------------------------------

    /**
     * Serialises an object to a JSON string.
     *
     * @param value the object
     * @return its JSON representation
     */
    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise " + value, e);
        }
    }

    // ------------------------------------------------------------------
    // Deserialisation
    // ------------------------------------------------------------------

    /**
     * Deserialises a JSON string into a type.
     *
     * @param json the JSON text
     * @param type target type
     * @param <T>  target type
     * @return the mapped object
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialise into " + type.getSimpleName(), e);
        }
    }

    /**
     * Deserialises a JSON array into a list.
     *
     * @param json        the JSON text
     * @param elementType element type
     * @param <T>         element type
     * @return the mapped list
     */
    public static <T> List<T> fromJsonArray(String json, Class<T> elementType) {
        try {
            return MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unable to deserialise into a List<" + elementType.getSimpleName() + ">", e);
        }
    }

    /**
     * Parses JSON into a navigable tree.
     *
     * @param json the JSON text
     * @return the root node
     */
    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to parse JSON", e);
        }
    }

    /**
     * Converts an already-parsed node into a type.
     *
     * @param node the node
     * @param type target type
     * @param <T>  target type
     * @return the mapped object
     */
    public static <T> T convert(JsonNode node, Class<T> type) {
        return MAPPER.convertValue(node, type);
    }

    // ------------------------------------------------------------------
    // Classpath resources
    // ------------------------------------------------------------------

    /**
     * Reads a JSON resource from the classpath and maps it to a type.
     *
     * @param resourcePath path relative to {@code src/test/resources}
     * @param type         target type
     * @param <T>          target type
     * @return the mapped object
     */
    public static <T> T readResource(String resourcePath, Class<T> type) {
        try (InputStream input = openResource(resourcePath)) {
            return MAPPER.readValue(input, type);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read JSON resource: " + resourcePath, e);
        }
    }

    /**
     * Reads a JSON resource from the classpath into a tree.
     *
     * @param resourcePath path relative to {@code src/test/resources}
     * @return the root node
     */
    public static JsonNode readResource(String resourcePath) {
        try (InputStream input = openResource(resourcePath)) {
            return MAPPER.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read JSON resource: " + resourcePath, e);
        }
    }

    private static InputStream openResource(String resourcePath) {
        InputStream input = JsonUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IllegalStateException("Resource not found on the classpath: " + resourcePath);
        }
        return input;
    }

    /**
     * Pretty-prints a JSON string, returning it unchanged when it is not valid
     * JSON — useful for logging an error body that may be plain text or HTML.
     *
     * @param json the JSON text
     * @return the formatted text
     */
    public static String prettyPrint(String json) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
