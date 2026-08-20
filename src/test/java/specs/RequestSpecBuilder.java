package specs;

import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import utils.ConfigReader;
import utils.LoggerUtility;

/**
 * Builds the request specification shared by every call.
 *
 * <p>Centralising base URI, base path, headers, timeouts and logging means a
 * change to any of them applies to the whole suite at once, and individual step
 * definitions never repeat setup.
 *
 * <p>Note the deliberate name clash with {@code io.restassured.builder.RequestSpecBuilder}:
 * this class is the framework's own façade over it, so REST Assured's builder is
 * referenced by its fully qualified name below.
 */
public final class RequestSpecBuilder {

    private static final LoggerUtility LOG = LoggerUtility.forClass(RequestSpecBuilder.class);

    private RequestSpecBuilder() {
        // Utility class.
    }

    /**
     * Builds the default request specification: JSON in and out, the configured
     * base URI and path, timeouts, and the API key when one is available.
     *
     * @return a reusable specification
     */
    public static RequestSpecification build() {
        io.restassured.builder.RequestSpecBuilder builder =
                new io.restassured.builder.RequestSpecBuilder()
                        .setBaseUri(ConfigReader.baseUri())
                        .setBasePath(ConfigReader.basePath())
                        .setContentType(ContentType.JSON)
                        .setAccept(ContentType.JSON)
                        .setConfig(timeoutConfig());

        applyAuthentication(builder);
        applyLogging(builder);

        return builder.build();
    }

    /**
     * Builds a specification carrying a JSON body.
     *
     * @param payload object serialised by Jackson onto the wire
     * @return a specification ready to send
     */
    public static RequestSpecification withBody(Object payload) {
        return build().body(payload);
    }

    /**
     * Builds a specification without authentication, for negative tests that
     * assert the API rejects unauthenticated calls.
     *
     * @return a specification with no API key
     */
    public static RequestSpecification withoutAuthentication() {
        io.restassured.builder.RequestSpecBuilder builder =
                new io.restassured.builder.RequestSpecBuilder()
                        .setBaseUri(ConfigReader.baseUri())
                        .setBasePath(ConfigReader.basePath())
                        .setContentType(ContentType.JSON)
                        .setAccept(ContentType.JSON)
                        .setConfig(timeoutConfig());

        applyLogging(builder);
        return builder.build();
    }

    /**
     * Builds a specification carrying a caller-supplied API key.
     *
     * <p>Used by the authentication-contract scenarios to prove the API
     * distinguishes an unrecognised key from an absent one.
     *
     * @param apiKey the key to send
     * @return a specification using that key
     */
    public static RequestSpecification withApiKey(String apiKey) {
        io.restassured.builder.RequestSpecBuilder builder =
                new io.restassured.builder.RequestSpecBuilder()
                        .setBaseUri(ConfigReader.baseUri())
                        .setBasePath(ConfigReader.basePath())
                        .setContentType(ContentType.JSON)
                        .setAccept(ContentType.JSON)
                        .addHeader(ConfigReader.apiKeyHeader(), apiKey)
                        .setConfig(timeoutConfig());

        applyLogging(builder);
        return builder.build();
    }

    /**
     * Adds the API key header when a key is configured.
     *
     * <p>reqres.in rejects every endpoint with 401 {@code missing_api_key} unless
     * this header is present, so a missing key is worth a loud warning rather
     * than a silent 401 later.
     */
    private static void applyAuthentication(io.restassured.builder.RequestSpecBuilder builder) {
        if (ConfigReader.hasApiKey()) {
            builder.addHeader(ConfigReader.apiKeyHeader(), ConfigReader.apiKey());
            return;
        }

        LOG.warn("No API key configured. reqres.in returns 401 missing_api_key for every "
                + "endpoint without one. Set apiKey in config.properties, pass -DapiKey=<key>, "
                + "or export API_KEY=<key>. Create a key at app.reqres.in/api-keys.");
    }

    /**
     * Attaches REST Assured's own request and response logging filters when
     * configuration asks for them.
     *
     * <p>These write to the console. The structured, file-backed logging that
     * {@code LoggerUtility} performs is separate and always on.
     */
    private static void applyLogging(io.restassured.builder.RequestSpecBuilder builder) {
        if ("always".equals(ConfigReader.logRequests())) {
            builder.addFilter(new RequestLoggingFilter(LogDetail.ALL));
        }
        if ("always".equals(ConfigReader.logResponses())) {
            builder.addFilter(new ResponseLoggingFilter(LogDetail.ALL));
        }
    }

    /**
     * @return connect and socket timeouts drawn from configuration
     */
    private static RestAssuredConfig timeoutConfig() {
        return RestAssuredConfig.config().httpClient(
                HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", ConfigReader.connectTimeout())
                        .setParam("http.socket.timeout", ConfigReader.readTimeout()));
    }
}
