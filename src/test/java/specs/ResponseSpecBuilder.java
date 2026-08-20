package specs;

import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;
import utils.ConfigReader;

import static org.hamcrest.Matchers.lessThan;

/**
 * Builds the response specifications shared by the assertions.
 *
 * <p>A response specification bundles the checks that should hold for a whole
 * class of responses — status, content type, response time — so a step
 * definition asserts intent rather than repeating boilerplate.
 *
 * <p>Note the deliberate name clash with {@code io.restassured.builder.ResponseSpecBuilder}:
 * this class is the framework's façade over it, so REST Assured's builder is
 * referenced by its fully qualified name below.
 */
public final class ResponseSpecBuilder {

    private ResponseSpecBuilder() {
        // Utility class.
    }

    /**
     * A JSON response with the expected status, returned inside the configured
     * time budget.
     *
     * @param expectedStatusCode the status the call should produce
     * @return the specification
     */
    public static ResponseSpecification expect(int expectedStatusCode) {
        return new io.restassured.builder.ResponseSpecBuilder()
                .expectStatusCode(expectedStatusCode)
                .expectContentType(contentType())
                .expectResponseTime(lessThan(ConfigReader.maxResponseTimeMillis()))
                .build();
    }

    /**
     * A response with the expected status and no content-type requirement.
     *
     * <p>Needed for 204 No Content: the body is empty and most APIs omit the
     * Content-Type header entirely, so asserting JSON there would fail a
     * perfectly correct response.
     *
     * @param expectedStatusCode the status the call should produce
     * @return the specification
     */
    public static ResponseSpecification expectNoContent(int expectedStatusCode) {
        return new io.restassured.builder.ResponseSpecBuilder()
                .expectStatusCode(expectedStatusCode)
                .expectResponseTime(lessThan(ConfigReader.maxResponseTimeMillis()))
                .build();
    }

    /**
     * Chooses the right specification for a status code.
     *
     * <p>Any 2xx that carries no body — 204, and 304 — gets the content-type-free
     * variant automatically, so callers do not have to remember the distinction.
     *
     * @param expectedStatusCode the status the call should produce
     * @return the specification
     */
    public static ResponseSpecification forStatus(int expectedStatusCode) {
        return isBodyless(expectedStatusCode)
                ? expectNoContent(expectedStatusCode)
                : expect(expectedStatusCode);
    }

    /**
     * @param statusCode an HTTP status
     * @return {@code true} when the status forbids a response body
     */
    public static boolean isBodyless(int statusCode) {
        return statusCode == 204 || statusCode == 304 || statusCode == 205;
    }

    /**
     * A 200 OK JSON response.
     *
     * @return the specification
     */
    public static ResponseSpecification ok() {
        return expect(200);
    }

    /**
     * A 201 Created JSON response.
     *
     * @return the specification
     */
    public static ResponseSpecification created() {
        return expect(201);
    }

    /**
     * A 204 No Content response.
     *
     * @return the specification
     */
    public static ResponseSpecification noContent() {
        return expectNoContent(204);
    }

    /**
     * A specification that only enforces the response-time budget, for cases
     * where the status is asserted separately.
     *
     * @return the specification
     */
    public static ResponseSpecification withinTimeBudget() {
        return new io.restassured.builder.ResponseSpecBuilder()
                .expectResponseTime(lessThan(ConfigReader.maxResponseTimeMillis()))
                .build();
    }

    private static ContentType contentType() {
        return ContentType.valueOf(ConfigReader.expectedContentType().toUpperCase());
    }
}
