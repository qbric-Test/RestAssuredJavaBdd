package client;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import specs.RequestSpecBuilder;
import utils.LoggerUtility;

import java.util.Map;

/**
 * Thin, reusable wrapper over REST Assured.
 *
 * <p>Every call goes through the shared request specification from
 * {@link RequestSpecBuilder}, and every call is logged the same way, so step
 * definitions express intent — "GET this endpoint" — and nothing else.
 *
 * <p>Responses are returned rather than asserted on: validation belongs to the
 * step definitions, which is what keeps this class reusable across suites.
 */
public final class ApiClient {

    private static final LoggerUtility LOG = LoggerUtility.forClass(ApiClient.class);

    private final RequestSpecification specification;

    /**
     * Creates a client using the framework's default specification.
     */
    public ApiClient() {
        this(RequestSpecBuilder.build());
    }

    /**
     * Creates a client using a caller-supplied specification.
     *
     * @param specification the specification every call will use
     */
    public ApiClient(RequestSpecification specification) {
        this.specification = specification;
    }

    /**
     * @return the specification backing this client
     */
    public RequestSpecification specification() {
        return specification;
    }

    // ------------------------------------------------------------------
    // HTTP verbs
    // ------------------------------------------------------------------

    /**
     * Sends a GET request.
     *
     * @param endpoint path appended to the base URI and base path
     * @return the response
     */
    public Response get(String endpoint) {
        return send("GET", endpoint, request().get(endpoint));
    }

    /**
     * Sends a GET request with query parameters.
     *
     * @param endpoint        path appended to the base URI and base path
     * @param queryParameters query string parameters
     * @return the response
     */
    public Response get(String endpoint, Map<String, ?> queryParameters) {
        RequestSpecification request = request().queryParams(queryParameters);
        LOG.debug("REQUEST   | Query   : {}", queryParameters);
        return send("GET", endpoint, request.get(endpoint));
    }

    /**
     * Sends a POST request with a JSON body.
     *
     * @param endpoint path appended to the base URI and base path
     * @param payload  object serialised by Jackson
     * @return the response
     */
    public Response post(String endpoint, Object payload) {
        return send("POST", endpoint, request().body(payload).post(endpoint));
    }

    /**
     * Sends a POST request without a body.
     *
     * @param endpoint path appended to the base URI and base path
     * @return the response
     */
    public Response post(String endpoint) {
        return send("POST", endpoint, request().post(endpoint));
    }

    /**
     * Sends a PUT request with a JSON body.
     *
     * @param endpoint path appended to the base URI and base path
     * @param payload  object serialised by Jackson
     * @return the response
     */
    public Response put(String endpoint, Object payload) {
        return send("PUT", endpoint, request().body(payload).put(endpoint));
    }

    /**
     * Sends a PATCH request with a JSON body.
     *
     * @param endpoint path appended to the base URI and base path
     * @param payload  object serialised by Jackson
     * @return the response
     */
    public Response patch(String endpoint, Object payload) {
        return send("PATCH", endpoint, request().body(payload).patch(endpoint));
    }

    /**
     * Sends a DELETE request.
     *
     * @param endpoint path appended to the base URI and base path
     * @return the response
     */
    public Response delete(String endpoint) {
        return send("DELETE", endpoint, request().delete(endpoint));
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * @return a fresh request built from the shared specification
     */
    private RequestSpecification request() {
        return RestAssured.given().spec(specification);
    }

    /**
     * Logs a completed call and returns its response.
     *
     * <p>The request is logged before the response so a hung or failed call still
     * leaves a record of what was attempted.
     */
    private Response send(String method, String endpoint, Response response) {
        LOG.request(specification, method, endpoint);
        LOG.response(response);
        return response;
    }
}
