package stepdefinitions;

import client.ApiClient;
import context.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import models.UserRequest;
import models.UserResponse;
import specs.RequestSpecBuilder;
import specs.ResponseSpecBuilder;
import utils.ConfigReader;
import utils.LoggerUtility;
import utils.TestDataFactory;

import java.util.List;
import java.util.Map;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for {@code features/users.feature}.
 *
 * <p>Steps stay thin: they call {@link client.ApiClient} through the context,
 * store the response, and assert on it. No request building or specification
 * assembly happens here — that lives in the client and spec layers, which is
 * what lets a new endpoint reuse all of it.
 */
public class UsersApiSteps {

    private static final String USERS_ENDPOINT = "/users";

    private static final LoggerUtility LOG = LoggerUtility.forClass(UsersApiSteps.class);

    private final TestContext context;

    /**
     * @param context per-scenario context, injected by PicoContainer
     */
    public UsersApiSteps(TestContext context) {
        this.context = context;
    }

    // ------------------------------------------------------------------
    // Given
    // ------------------------------------------------------------------

    @Given("User API is available")
    public void userApiIsAvailable() {
        assertNotNull(context.getApiClient(),
                "The API client was not initialised by the Before hook.");

        // Fail early and clearly when no key is configured. Without this every
        // scenario fails on a status assertion instead, which hides the cause.
        assertTrue(ConfigReader.hasApiKey(),
                "No API key is configured, and " + ConfigReader.baseUri()
                        + " rejects every endpoint with 401 missing_api_key without one.\n"
                        + "Create a key at app.reqres.in/api-keys, then either set apiKey in "
                        + "src/test/resources/config/config.properties, pass -DapiKey=<key>, "
                        + "or export API_KEY=<key>.");

        LOG.validation("User API is reachable at " + ConfigReader.baseUri()
                + ConfigReader.basePath());
    }

    /**
     * Reachability check that does <em>not</em> require a key.
     *
     * <p>Used by the authentication-contract scenarios, which are about what the
     * API does when a key is absent or wrong — so demanding one first would be
     * self-defeating.
     */
    @Given("User API is reachable")
    public void userApiIsReachable() {
        assertNotNull(context.getApiClient(),
                "The API client was not initialised by the Before hook.");

        LOG.validation("Target API is " + ConfigReader.baseUri() + ConfigReader.basePath());
    }

    // ------------------------------------------------------------------
    // When
    // ------------------------------------------------------------------

    @When("I send GET request for user id {int} without an API key")
    public void iSendGetRequestForUserIdWithoutApiKey(int userId) {
        ApiClient unauthenticated = new ApiClient(RequestSpecBuilder.withoutAuthentication());

        context.setResponse(unauthenticated.get(USERS_ENDPOINT + "/" + userId));
        context.set("userId", userId);
    }

    @When("I send GET request for user id {int} with an invalid API key")
    public void iSendGetRequestForUserIdWithInvalidApiKey(int userId) {
        ApiClient wrongKey = new ApiClient(
                RequestSpecBuilder.withApiKey("invalid-key-used-by-the-auth-contract-test"));

        context.setResponse(wrongKey.get(USERS_ENDPOINT + "/" + userId));
        context.set("userId", userId);
    }

    @When("I send GET request for user id {int}")
    public void iSendGetRequestForUserId(int userId) {
        Response response = context.getApiClient().get(USERS_ENDPOINT + "/" + userId);
        context.setResponse(response);
        context.set("userId", userId);
    }

    @When("I send GET request for users page {int}")
    public void iSendGetRequestForUsersPage(int page) {
        Response response = context.getApiClient()
                .get(USERS_ENDPOINT, Map.of("page", page));
        context.setResponse(response);
        context.set("page", page);
    }

    @When("I send POST request to create user")
    public void iSendPostRequestToCreateUser() {
        UserRequest payload = TestDataFactory.createUserPayload();
        context.setRequestPayload(payload);

        Response response = context.getApiClient().post(USERS_ENDPOINT, payload);
        context.setResponse(response);
    }

    @When("I send PUT request for user id {int}")
    public void iSendPutRequestForUserId(int userId) {
        UserRequest payload = TestDataFactory.updateUserPayload();
        context.setRequestPayload(payload);

        Response response = context.getApiClient().put(USERS_ENDPOINT + "/" + userId, payload);
        context.setResponse(response);
        context.set("userId", userId);
    }

    @When("I send PATCH request for user id {int}")
    public void iSendPatchRequestForUserId(int userId) {
        UserRequest payload = TestDataFactory.updateUserPayload();
        context.setRequestPayload(payload);

        Response response = context.getApiClient().patch(USERS_ENDPOINT + "/" + userId, payload);
        context.setResponse(response);
        context.set("userId", userId);
    }

    @When("I send DELETE request for user id {int}")
    public void iSendDeleteRequestForUserId(int userId) {
        Response response = context.getApiClient().delete(USERS_ENDPOINT + "/" + userId);
        context.setResponse(response);
        context.set("userId", userId);
    }

    // ------------------------------------------------------------------
    // Then
    // ------------------------------------------------------------------

    @Then("response status code should be {int}")
    public void responseStatusCodeShouldBe(int expectedStatusCode) {
        Response response = context.getResponse();

        // The shared response specification also enforces the content type and
        // the response-time budget, so a slow-but-correct API still fails here —
        // deliberately, because latency is part of the contract.
        response.then().spec(ResponseSpecBuilder.forStatus(expectedStatusCode));

        LOG.validation("Status code is " + expectedStatusCode
                + " (in " + response.getTime() + " ms)");
    }

    @Then("response should contain user id {int}")
    public void responseShouldContainUserId(int expectedUserId) {
        Response response = context.getResponse();

        response.then().body("data.id", equalTo(expectedUserId));

        // Deserialise as well as assert on the raw body: this proves the POJO
        // mapping works, which the JSON-path assertion alone would not.
        UserResponse user = response.jsonPath().getObject("data", UserResponse.class);
        assertNotNull(user, "The response carried no 'data' object.");
        assertEquals(expectedUserId, user.getIdAsInt(), "Unexpected user id.");
        assertThat("The user should have an email", user.getEmail(), notNullValue());

        LOG.validation("Response contains user id " + expectedUserId
                + " (" + user.getEmail() + ")");
    }

    @Then("response should match user schema")
    public void responseShouldMatchUserSchema() {
        context.getResponse().then()
                .body(matchesJsonSchemaInClasspath("schemas/user-schema.json"));

        LOG.validation("Response matches schemas/user-schema.json");
    }

    /**
     * Validates the response against any schema on the classpath.
     *
     * <p>The named variants above cover the users endpoints; this one lets a new
     * endpoint be schema-checked without adding a step definition.
     *
     * @param schemaPath path under {@code src/test/resources}, e.g. "schemas/user-schema.json"
     */
    @Then("response should match schema {string}")
    public void responseShouldMatchSchema(String schemaPath) {
        context.getResponse().then().body(matchesJsonSchemaInClasspath(schemaPath));
        LOG.validation("Response matches " + schemaPath);
    }

    @Then("response should match user list schema")
    public void responseShouldMatchUserListSchema() {
        context.getResponse().then()
                .body(matchesJsonSchemaInClasspath("schemas/user-list-schema.json"));

        LOG.validation("Response matches schemas/user-list-schema.json");
    }

    @Then("users list should not be empty")
    public void usersListShouldNotBeEmpty() {
        Response response = context.getResponse();

        response.then().body("data.size()", greaterThan(0));

        List<UserResponse> users = response.jsonPath().getList("data", UserResponse.class);
        assertNotNull(users, "The response carried no 'data' array.");
        assertThat("The users list should not be empty", users, not(org.hamcrest.Matchers.empty()));

        users.forEach(user -> assertNotNull(user.getEmail(),
                "Every returned user should have an email but one was missing: " + user));

        LOG.validation(users.size() + " user(s) returned on page "
                + context.get("page", Integer.class));
    }

    @Then("created user should be returned")
    public void createdUserShouldBeReturned() {
        Response response = context.getResponse();
        UserRequest sent = context.getRequestPayload();

        UserResponse created = response.as(UserResponse.class);

        assertNotNull(created.getId(), "A created user should carry an id.");
        assertNotNull(created.getCreatedAt(), "A created user should carry a createdAt timestamp.");
        assertEquals(sent.getName(), created.getName(), "The returned name should echo the request.");
        assertEquals(sent.getJob(), created.getJob(), "The returned job should echo the request.");

        context.set("createdUserId", created.getId());
        LOG.validation("Created user id " + created.getId() + " at " + created.getCreatedAt());
    }

    @Then("updated user should be returned")
    public void updatedUserShouldBeReturned() {
        Response response = context.getResponse();
        UserRequest sent = context.getRequestPayload();

        UserResponse updated = response.as(UserResponse.class);

        assertNotNull(updated.getUpdatedAt(), "An updated user should carry an updatedAt timestamp.");
        assertEquals(sent.getJob(), updated.getJob(), "The returned job should echo the request.");

        LOG.validation("Updated user at " + updated.getUpdatedAt());
    }

    @Then("response body should be empty")
    public void responseBodyShouldBeEmpty() {
        String body = LoggerUtility.safeBody(context.getResponse());
        assertTrue(body.isBlank(),
                "Expected an empty body but received: " + body);

        LOG.validation("Response body is empty, as required for 204 No Content");
    }

    /**
     * Asserts on the machine-readable {@code error} code in an error body.
     *
     * <p>Asserting the code rather than the human-readable message means the
     * test survives copy edits to the message text, which are common and
     * meaningless.
     *
     * @param expectedError the expected error code, e.g. "missing_api_key"
     */
    @Then("response error should be {string}")
    public void responseErrorShouldBe(String expectedError) {
        Response response = context.getResponse();

        response.then().body("error", equalTo(expectedError));

        LOG.validation("API reported '" + expectedError + "' as expected");
    }

    @Then("response time should be under {int} ms")
    public void responseTimeShouldBeUnder(int maxMillis) {
        long actual = context.getResponse().getTime();
        assertTrue(actual < maxMillis,
                "Response took " + actual + " ms, which exceeds the " + maxMillis + " ms budget.");

        LOG.validation("Response time " + actual + " ms is within " + maxMillis + " ms");
    }
}
