@api @users
Feature: Users API Validation

  As an API consumer
  I want the users endpoints to behave to contract
  So that clients can read, create, update and delete users reliably

  Background:
    Given User API is available

  @smoke
  Scenario: Get Single User
    When I send GET request for user id 2
    Then response status code should be 200
    And response should contain user id 2
    And response should match user schema

  @smoke
  Scenario: Get Users List
    When I send GET request for users page 2
    Then response status code should be 200
    And users list should not be empty

  Scenario: Create New User
    When I send POST request to create user
    Then response status code should be 201
    And created user should be returned

  Scenario: Update Existing User
    When I send PUT request for user id 2
    Then response status code should be 200

  Scenario: Delete User
    When I send DELETE request for user id 2
    Then response status code should be 204
