package hooks;

import context.TestContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import specs.RequestSpecBuilder;
import utils.ConfigReader;
import utils.JsonUtils;
import utils.LoggerUtility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Scenario lifecycle: configuration and specification setup before, response
 * logging and context cleanup after.
 */
public class Hooks {

    private static final LoggerUtility LOG = LoggerUtility.forClass(Hooks.class);

    private final TestContext context;

    /**
     * @param context per-scenario context, injected by PicoContainer
     */
    public Hooks(TestContext context) {
        this.context = context;
    }

    /**
     * Initialises configuration, the test context and the shared request
     * specification.
     *
     * @param scenario the scenario about to run
     */
    @Before(order = 0)
    public void setUp(Scenario scenario) {
        LOG.scenarioStart(scenario.getName());

        createOutputDirectories();

        // Applied once per scenario rather than globally: it keeps the
        // configuration explicit and survives a suite that later points
        // different scenarios at different hosts.
        RestAssured.baseURI = ConfigReader.baseUri();
        RestAssured.basePath = ConfigReader.basePath();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        LOG.info("Config    | env={} | baseUri={}{} | apiKey={}",
                ConfigReader.environment(),
                ConfigReader.baseUri(),
                ConfigReader.basePath(),
                ConfigReader.hasApiKey() ? "configured" : "MISSING");

        context.setScenario(scenario);
        context.setRequestSpecification(RequestSpecBuilder.build());
    }

    /**
     * Attaches the last response to the report and clears the context.
     *
     * <p>The response is attached on every outcome, not only on failure: for an
     * API suite the payload <em>is</em> the evidence, and having it on a passing
     * scenario is what makes the report useful when behaviour changes later.
     *
     * @param scenario the finished scenario
     */
    @After(order = 0)
    public void tearDown(Scenario scenario) {
        try {
            attachResponse(scenario);
        } catch (RuntimeException e) {
            LOG.warn("Could not attach the response to the report: {}", e.getMessage());
        } finally {
            context.clear();
            LOG.cleanup("Test context cleared");
            LOG.scenarioEnd(scenario.getName(), scenario.getStatus().name());
        }
    }

    /**
     * Logs the response and embeds it, plus a request summary, in the report.
     *
     * @param scenario the finished scenario
     */
    private void attachResponse(Scenario scenario) {
        if (!context.hasResponse()) {
            LOG.debug("No response captured for this scenario.");
            return;
        }

        Response response = context.getResponse();
        LOG.response(response);

        scenario.attach(
                "Status: " + response.getStatusCode() + " | Time: " + response.getTime() + " ms",
                "text/plain",
                "response-summary");

        String body = LoggerUtility.safeBody(response);
        if (!body.isBlank()) {
            // Pretty-printed so the report shows readable JSON rather than one
            // long line; falls back to the raw text when the body is not JSON.
            scenario.attach(JsonUtils.prettyPrint(body), "text/plain", "response-body");
        }

        if (context.getRequestPayload() != null) {
            scenario.attach(JsonUtils.toJson(context.getRequestPayload()),
                    "text/plain", "request-payload");
        }
    }

    /**
     * Creates the reports and log directories when they are missing.
     */
    private void createOutputDirectories() {
        try {
            Files.createDirectories(Paths.get(ConfigReader.reportsDir()));
            Files.createDirectories(Paths.get(ConfigReader.logDir()));
        } catch (IOException e) {
            LOG.warn("Could not create the output directories: {}", e.getMessage());
        }
    }
}
