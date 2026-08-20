@api @auth
Feature: Users API Authentication Contract

  As an API consumer
  I want the users endpoints to reject unauthenticated and unrecognised keys distinctly
  So that a client can tell a missing credential from a wrong one

  # These scenarios need no API key: they assert what the API does when a key is
  # absent or invalid. They are the only scenarios that run green against
  # reqres.in without a personal key, which also makes them a useful smoke test
  # that the framework itself is wired up correctly.

  Background:
    Given User API is reachable

  @negative
  Scenario: Request without an API key is rejected as missing
    When I send GET request for user id 2 without an API key
    Then response status code should be 401
    And response error should be "missing_api_key"

  @negative
  Scenario: Request with an unrecognised API key is rejected as invalid
    When I send GET request for user id 2 with an invalid API key
    Then response status code should be 403
    And response error should be "invalid_api_key"
