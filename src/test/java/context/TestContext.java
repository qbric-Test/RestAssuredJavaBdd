package context;

import client.ApiClient;
import io.cucumber.java.Scenario;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import models.UserRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-scenario state shared between hooks, step definitions and utilities.
 *
 * <p>PicoContainer creates one instance per scenario and injects that same
 * instance wherever it is declared as a constructor parameter. Nothing is held
 * statically, which is what makes the parallel execution configured in
 * {@code junit-platform.properties} safe.
 */
public class TestContext {

    private final Map<String, Object> scenarioData = new HashMap<>();

    private RequestSpecification requestSpecification;

    private ApiClient apiClient;

    private Response response;

    private UserRequest requestPayload;

    private Scenario scenario;

    // ------------------------------------------------------------------
    // Request specification and client
    // ------------------------------------------------------------------

    /**
     * @return the specification every call in this scenario uses
     * @throws IllegalStateException when the Before hook has not run
     */
    public RequestSpecification getRequestSpecification() {
        if (requestSpecification == null) {
            throw new IllegalStateException(
                    "No request specification in the test context: the Before hook has not run.");
        }
        return requestSpecification;
    }

    /**
     * Stores the specification and builds the client bound to it.
     *
     * @param requestSpecification the shared specification
     */
    public void setRequestSpecification(RequestSpecification requestSpecification) {
        this.requestSpecification = requestSpecification;
        this.apiClient = new ApiClient(requestSpecification);
    }

    /**
     * @return the API client for this scenario
     * @throws IllegalStateException when the Before hook has not run
     */
    public ApiClient getApiClient() {
        if (apiClient == null) {
            throw new IllegalStateException(
                    "No API client in the test context: the Before hook has not run.");
        }
        return apiClient;
    }

    // ------------------------------------------------------------------
    // Response
    // ------------------------------------------------------------------

    /**
     * @return the most recent response
     * @throws IllegalStateException when no request has been sent yet
     */
    public Response getResponse() {
        if (response == null) {
            throw new IllegalStateException(
                    "No response in the test context: a When step must send a request first.");
        }
        return response;
    }

    /**
     * @param response the response just received
     */
    public void setResponse(Response response) {
        this.response = response;
    }

    /**
     * @return {@code true} when a response has been captured
     */
    public boolean hasResponse() {
        return response != null;
    }

    // ------------------------------------------------------------------
    // Request payload
    // ------------------------------------------------------------------

    /**
     * @return the payload sent by the last write request
     */
    public UserRequest getRequestPayload() {
        return requestPayload;
    }

    /**
     * @param requestPayload the payload about to be sent
     */
    public void setRequestPayload(UserRequest requestPayload) {
        this.requestPayload = requestPayload;
    }

    // ------------------------------------------------------------------
    // Scenario
    // ------------------------------------------------------------------

    /**
     * @return the running scenario
     */
    public Scenario getScenario() {
        return scenario;
    }

    /**
     * @param scenario the running scenario, supplied by the Before hook
     */
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * @return the running scenario's name, or a placeholder before it is set
     */
    public String getScenarioName() {
        return scenario == null ? "scenario" : scenario.getName();
    }

    // ------------------------------------------------------------------
    // Free-form data sharing between steps
    // ------------------------------------------------------------------

    /**
     * Stores a value for a later step in the same scenario.
     *
     * @param key   lookup key
     * @param value value to keep
     */
    public void set(String key, Object value) {
        scenarioData.put(key, value);
    }

    /**
     * Reads a value stored earlier in the same scenario.
     *
     * @param key  lookup key
     * @param type expected type
     * @param <T>  expected type
     * @return the stored value, or {@code null} when absent
     */
    public <T> T get(String key, Class<T> type) {
        return type.cast(scenarioData.get(key));
    }

    /**
     * Reads a value a previous step is required to have set.
     *
     * @param key  lookup key
     * @param type expected type
     * @param <T>  expected type
     * @return the stored value
     * @throws IllegalStateException when the key is absent
     */
    public <T> T require(String key, Class<T> type) {
        Object value = scenarioData.get(key);
        if (value == null) {
            throw new IllegalStateException("Scenario context is missing the required key: " + key);
        }
        return type.cast(value);
    }

    /**
     * Clears everything held for this scenario.
     */
    public void clear() {
        scenarioData.clear();
        response = null;
        requestPayload = null;
        requestSpecification = null;
        apiClient = null;
    }
}
