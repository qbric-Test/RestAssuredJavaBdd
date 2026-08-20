package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Singleton configuration reader.
 *
 * <p>Values are read from {@code src/test/resources/config/config.properties}.
 * A matching JVM system property always wins, and an environment variable wins
 * over the file too, so secrets never need to be committed:
 *
 * <pre>
 *   mvn test -DapiKey=abc123
 *   API_KEY=abc123 mvn test
 * </pre>
 */
public final class ConfigReader {

    private static final String CONFIG_FILE = "config/config.properties";

    private static final Properties PROPERTIES = load();

    private ConfigReader() {
        // Utility class.
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream input =
                     ConfigReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Configuration file not found on the classpath: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read configuration file: " + CONFIG_FILE, e);
        }
        return properties;
    }

    /**
     * Returns a required property.
     *
     * <p>Resolution order: system property, environment variable, properties file.
     *
     * @param key property name
     * @return the trimmed value
     * @throws IllegalStateException when the key is absent or blank everywhere
     */
    public static String get(String key) {
        String value = resolve(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing configuration value for key: " + key);
        }
        return value.trim();
    }

    /**
     * Returns a property, or the supplied default when it is absent.
     *
     * @param key          property name
     * @param defaultValue value to use when the key is absent
     * @return the resolved value
     */
    public static String get(String key, String defaultValue) {
        String value = resolve(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /**
     * Returns a property parsed as an int.
     *
     * @param key          property name
     * @param defaultValue value to use when the key is absent
     * @return the resolved value
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Configuration value for '" + key + "' is not a number: " + value, e);
        }
    }

    /**
     * Returns a property parsed as a boolean.
     *
     * @param key          property name
     * @param defaultValue value to use when the key is absent
     * @return the resolved value
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    /**
     * Resolves a key from a system property, then an environment variable, then
     * the properties file.
     *
     * <p>Environment lookups also try the SCREAMING_SNAKE_CASE form, so
     * {@code apiKey} is satisfied by {@code API_KEY} — the shape CI systems use.
     */
    private static String resolve(String key) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String environment = System.getenv(key);
        if (environment != null && !environment.isBlank()) {
            return environment;
        }

        String screamingSnake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        String environmentSnake = System.getenv(screamingSnake);
        if (environmentSnake != null && !environmentSnake.isBlank()) {
            return environmentSnake;
        }

        return PROPERTIES.getProperty(key);
    }

    // ------------------------------------------------------------------
    // Typed accessors
    // ------------------------------------------------------------------

    /**
     * @return the base URI, without a trailing slash
     */
    public static String baseUri() {
        String uri = get("baseUri");
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    /**
     * @return the base path prefixed to every endpoint, for example {@code /api}
     */
    public static String basePath() {
        return get("basePath", "");
    }

    public static String environment() {
        return get("environment", "qa");
    }

    /**
     * @return the header name carrying the API key
     */
    public static String apiKeyHeader() {
        return get("apiKeyHeader", "x-api-key");
    }

    /**
     * @return the API key, or an empty string when none is configured
     */
    public static String apiKey() {
        return get("apiKey", "");
    }

    /**
     * @return {@code true} when an API key is available
     */
    public static boolean hasApiKey() {
        return !apiKey().isBlank();
    }

    public static int connectTimeout() {
        return getInt("connectTimeout", 30_000);
    }

    public static int readTimeout() {
        return getInt("readTimeout", 30_000);
    }

    public static long maxResponseTimeMillis() {
        return getInt("maxResponseTimeMillis", 10_000);
    }

    public static String expectedContentType() {
        return get("expectedContentType", "JSON");
    }

    public static String logRequests() {
        return get("logRequests", "always").toLowerCase(Locale.ROOT);
    }

    public static String logResponses() {
        return get("logResponses", "always").toLowerCase(Locale.ROOT);
    }

    public static String logDir() {
        return get("logDir", "test-results");
    }

    public static String reportsDir() {
        return get("reportsDir", "reports");
    }
}
