package utils;

import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralised logging for API requests and responses.
 *
 * <p>REST Assured can log to the console itself, but that output goes straight to
 * {@code System.out} and bypasses SLF4J — so it never reaches the log file and
 * cannot be filtered by level. Routing through this class keeps request and
 * response detail in {@code test-results/api-execution.log} alongside everything
 * else.
 *
 * <p>Header values for the API key are masked, so a shared log or a CI console
 * never leaks a credential.
 */
public final class LoggerUtility {

    private static final int MAX_BODY_CHARS = 4_000;

    private final Logger logger;

    private LoggerUtility(Class<?> type) {
        this.logger = LoggerFactory.getLogger(type);
    }

    /**
     * Creates a logger scoped to the supplied class.
     *
     * @param type class the messages belong to
     * @return a new logger instance
     */
    public static LoggerUtility forClass(Class<?> type) {
        return new LoggerUtility(type);
    }

    // ------------------------------------------------------------------
    // Request and response logging
    // ------------------------------------------------------------------

    /**
     * Logs the outgoing request: method, URI, headers and body.
     *
     * @param specification the request specification about to be sent
     * @param method        HTTP method
     * @param endpoint      the endpoint being called
     */
    public void request(RequestSpecification specification, String method, String endpoint) {
        logger.info("REQUEST   | {} {}", method, endpoint);

        try {
            QueryableRequestSpecification queryable = SpecificationQuerier.query(specification);

            logger.debug("REQUEST   | URI     : {}", queryable.getURI());
            logger.debug("REQUEST   | Headers : {}", maskSensitive(String.valueOf(queryable.getHeaders())));

            String body = queryable.getBody();
            if (body != null && !body.isBlank()) {
                logger.info("REQUEST   | Body    : {}", truncate(body));
            }
        } catch (RuntimeException e) {
            // Querying a specification can fail for exotic configurations; never
            // let logging break the test.
            logger.debug("REQUEST   | Could not introspect the specification: {}", e.getMessage());
        }
    }

    /**
     * Logs the response: status, time, content type and body.
     *
     * @param response the received response
     */
    public void response(Response response) {
        if (response == null) {
            logger.warn("RESPONSE  | No response was captured");
            return;
        }

        logger.info("RESPONSE  | Status  : {} {}",
                response.getStatusCode(), response.getStatusLine());
        logger.info("RESPONSE  | Time    : {} ms", response.getTime());
        logger.debug("RESPONSE  | Type    : {}", response.getContentType());
        logger.debug("RESPONSE  | Headers : {}", maskSensitive(String.valueOf(response.getHeaders())));

        String body = safeBody(response);
        if (!body.isBlank()) {
            logger.info("RESPONSE  | Body    : {}", truncate(body));
        }
    }

    // ------------------------------------------------------------------
    // Execution events
    // ------------------------------------------------------------------

    /**
     * @param scenarioName scenario name
     */
    public void scenarioStart(String scenarioName) {
        logger.info("SCENARIO  | START  | {}", scenarioName);
    }

    /**
     * @param scenarioName scenario name
     * @param status       final status
     */
    public void scenarioEnd(String scenarioName, String status) {
        logger.info("SCENARIO  | END    | {} | status={}", scenarioName, status);
    }

    /**
     * @param message what was verified
     */
    public void validation(String message) {
        logger.info("VERIFY    | {}", message);
    }

    /**
     * @param message what was cleaned up
     */
    public void cleanup(String message) {
        logger.info("CLEANUP   | {}", message);
    }

    // ------------------------------------------------------------------
    // Generic levels
    // ------------------------------------------------------------------

    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void error(String message, Object... args) {
        logger.error("FAILURE   | " + message, args);
    }

    /**
     * @param message context for the failure
     * @param error   the throwable
     */
    public void failure(String message, Throwable error) {
        logger.error("FAILURE   | {}", message, error);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Reads a response body without throwing on a body-less response such as a
     * 204 No Content.
     *
     * @param response the response
     * @return the body, or an empty string
     */
    public static String safeBody(Response response) {
        try {
            String body = response.getBody().asString();
            return body == null ? "" : body;
        } catch (RuntimeException e) {
            return "";
        }
    }

    /**
     * Masks credential-bearing header values.
     *
     * @param text raw header text
     * @return the same text with secrets replaced
     */
    private static String maskSensitive(String text) {
        return text.replaceAll("(?i)(x-api-key|authorization|api-key)=([^,\\]\\s]+)", "$1=****");
    }

    private static String truncate(String body) {
        String collapsed = body.replaceAll("\\s+", " ").trim();
        return collapsed.length() > MAX_BODY_CHARS
                ? collapsed.substring(0, MAX_BODY_CHARS) + "... [truncated]"
                : collapsed;
    }
}
