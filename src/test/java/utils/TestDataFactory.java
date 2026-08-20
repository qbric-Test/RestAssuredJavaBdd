package utils;

import com.fasterxml.jackson.databind.JsonNode;
import models.UserRequest;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Supplies request payloads to the step definitions.
 *
 * <p>Static data lives in {@code src/test/resources/testdata/users.json} and is
 * read through a named key, so changing a payload never means touching Java
 * code. Generated data is produced here for cases that must be unique per run.
 *
 * <p>The file is parsed once and cached: the payloads are immutable inputs, and
 * re-reading them per scenario would add I/O to every parallel worker.
 */
public final class TestDataFactory {

    private static final String TEST_DATA_FILE = "testdata/users.json";

    private static final JsonNode TEST_DATA = JsonUtils.readResource(TEST_DATA_FILE);

    private TestDataFactory() {
        // Utility class.
    }

    /**
     * Reads a named payload from the test-data file and maps it to a request.
     *
     * @param key top-level key in users.json, for example "createUser"
     * @return the mapped request payload
     * @throws IllegalArgumentException when the key is missing
     */
    public static UserRequest userFrom(String key) {
        JsonNode node = TEST_DATA.get(key);
        if (node == null) {
            throw new IllegalArgumentException(
                    "No '" + key + "' entry in " + TEST_DATA_FILE
                            + ". Available keys: " + availableKeys());
        }
        return JsonUtils.convert(node, UserRequest.class);
    }

    /**
     * @return the payload used to create a user
     */
    public static UserRequest createUserPayload() {
        return userFrom("createUser");
    }

    /**
     * @return the payload used to update a user
     */
    public static UserRequest updateUserPayload() {
        return userFrom("updateUser");
    }

    /**
     * Builds a payload with a name unique to this call.
     *
     * <p>Useful when a test must not collide with data left behind by an earlier
     * run, which matters as soon as scenarios execute in parallel.
     *
     * @return a request with a generated name
     */
    public static UserRequest randomUserPayload() {
        int suffix = ThreadLocalRandom.current().nextInt(100_000, 999_999);
        return UserRequest.builder()
                .name("qa-user-" + suffix)
                .job("automation-engineer")
                .build();
    }

    /**
     * Reads an arbitrary value from the test-data file.
     *
     * @param path slash-separated path, for example "knownUser/email"
     * @return the value as text
     */
    public static String value(String path) {
        JsonNode node = TEST_DATA;
        for (String segment : path.split("/")) {
            node = node.get(segment);
            if (node == null) {
                throw new IllegalArgumentException(
                        "No '" + path + "' entry in " + TEST_DATA_FILE);
            }
        }
        return node.asText();
    }

    /**
     * @return the id of the user the read scenarios rely on
     */
    public static int knownUserId() {
        return Integer.parseInt(value("knownUser/id"));
    }

    private static String availableKeys() {
        StringBuilder keys = new StringBuilder();
        TEST_DATA.fieldNames().forEachRemaining(name -> keys.append(name).append(' '));
        return keys.toString().trim();
    }
}
