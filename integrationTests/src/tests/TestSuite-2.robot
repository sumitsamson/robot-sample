*** Settings ***
Documentation     A testing suite for testing the ARTIFACTORY plugin
Force Tags    premonly
Variables     ../resources/sut.py
Resource      ../common/common.robot
Resource      ../common/manifest.robot
Library       com.ca.cdd.plugins.NolioRESTClient
Library       com.ca.cdd.plugins.NolioJsonUtils
Library  String
Library  Collections

Metadata      Author: Andrey Kovalev
Test Timeout  5 minutes

*** Variables ***

${PRIVATE_GIT_REPO_DOMAIN}=  https://github.gwd.broadcom.net
${NON_EXISTING_GIT_REPO_DOMAIN}=  https://github-isl-99.ca.com

${PRIVATE_GIT_USER}=  bldcddbuild.co
${PRIVATE_GIT_PASSWORD}=  Summer12$

${PUBLIC_GIT_REPO_DOMAIN}=  https://github.com
${PUBLIC_GIT_USERNAME}=  cde-test-user
${PUBLIC_GIT_PASSWORD}=  N0li0123
${PRIVATE_SRC_REPO}=  ${PRIVATE_GIT_REPO_DOMAIN}/ESD/gradle-testing-plugin.git

${MASTER_BRANCH}=  master

# This is the default items count for dynamic values call in case max results is not specified in the query
${DEFAULT_ITEMS_COUNT}=  10

#Error Messages
${BAD_CREDENTIALS}=  Failed to connect to git repository due to = ${PRIVATE_SRC_REPO}: not authorized
${MISSING_ENDPOINT_PARAM}=  Endpoint is missing mandatory parameters


*** Keywords ***
Generate Execution Identifier
    ${now}=  Get time  epoch
    Set Test Variable  ${testExecutionId}  00000000-0000-0000-0000-000000000000_${now}

Generate Connection Properties JSON
    [Arguments]  ${registry}  ${user}  ${password}
    Run Keyword And Return  Catenate
    ...  "endPointProperties": {
    ...      "gitUrl": "${registry}",
    ...      "gitUsername": "${user}",
    ...      "gitUserPassword": "${password}",
    ...      "sourceControl": "Git"
    ...  }

Github Connection Test
    [Arguments]  ${registry}  ${user}  ${password}
    ${ep_properties}=  Generate Connection Properties JSON  ${registry}  ${user}  ${password}
    Set Test Variable  ${properties}  { ${ep_properties} }
    Run Keyword And Return  Execute Post Operation Class  ${GRADLE_TESTING_CONNECTIVITY_URL}
    ...                     ${properties}  com.ca.rp.plugins.dto.model.ConnectivityResult

Generate Gradle Test Source Properties JSON
    [Arguments]   ${branch}
    Run Keyword And Return  Catenate
    ...    "testSourceProperties": {
    ...        "branch":  "${branch}"
    ...    }
Generate Execution Context For Import JSON
    Run Keyword And Return  Catenate
    ...  "executionContext": {
    ...  }

Generate Gradle Import Tests Git JSON
    [Arguments]  ${url}  ${user}  ${password}  ${branch}
    ${testSource}=  Generate Gradle Test Source Properties JSON  ${branch}
    ${endpoint}=  Generate Connection Properties JSON  ${url}  ${user}  ${password}
    ${execution}=  Generate Execution Context For Import JSON
    Run Keyword And Return  Catenate
    ...  { ${endpoint}, ${testSource}, ${execution}, "executionId" : "${testExecutionId}" }

*** Test Cases ***

### ###################### ###
### Plugin/manifest tests: ###
### ###################### ###

01 - Get Manifest From Gradle Testing Plugin
    ${manifest}=  Get Manifest  ${GRADLE_TESTING_MANIFEST_URL}
    Validate Manifest by name version uniqueId and vendor  ${manifest}  Gradle Testing  1.2  CA Technologies Gradle Testing  CA Technologies
    Log  ${manifest}

02 - Get Manifest For Plugin And Validate The Manifest Fields Via Validator
    ${manifest}=  Get Manifest  ${GRADLE_TESTING_MANIFEST_URL}
    ${response}=  Validate Manifest Fields  ${manifest}
    Log  ${response}

03 - Validate supported gradle versions
    @{SUPPORTED_GRADLE_VERSIONS}=  Create List  3.5  3.0  2.14  4.4  4.0.2

    ${manifest}=  Get Manifest  ${GRADLE_TESTING_MANIFEST_URL}
    ${gradleVersions}=  Extract String List from Json List By Path Json  ${manifest}  $.endpointTemplate.parameters[0].possibleValues
    # Convert JsonArray to list
    ${gradleVersions}=  Create List  @{gradleVersions}
    # Sort, to ignore order during list comparisson
    Sort List  ${SUPPORTED_GRADLE_VERSIONS}
    Sort List  ${gradleVersions}
#    ${gradleVersionsType}=  Evaluate  type($gradleVersions).__name__
    Lists Should Be Equal  ${gradleVersions}  ${SUPPORTED_GRADLE_VERSIONS}

### ###################### ###
###    Endpoint tests:     ###
### ###################### ###


04 - GIT Connectivity Test - Private Repository
    ${response}=  Github Connection Test  ${PRIVATE_SRC_REPO}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}
    Verify Successfull Connectivity  ${response}

05 - Negative Connectivity Test Unknown User
    Set Test Variable  ${NON_EXISTING_USER}  nonExistingUser
    ${response}=  Github Connection Test  ${PRIVATE_SRC_REPO}  ${nonExistingUser}  ${PRIVATE_GIT_PASSWORD}
    Verify Failure Connectivity  ${response}  ${BAD_CREDENTIALS}

06 - Negative Connectivity Test Bad URL
    Set Test Variable  ${badUrlRepo}  ThisIsNotAURL
    ${response}=  Github Connection Test  ${badUrlRepo}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}
    Verify Failure Connectivity  ${response}  Property [gitUrl] got invalid value [${badUrlRepo}]. Reason: URL is malformed

07 - Negative Connectivity Test Inaccessible URL
    Set Test Variable  ${inaccessibleRepo}  ${NON_EXISTING_GIT_REPO_DOMAIN}/CddRobot/gradle-testing-plugin.git
    ${response}=  Github Connection Test  ${inaccessibleRepo}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}
    Verify Failure Connectivity  ${response}  Failed accessing URL: [${inaccessibleRepo}]. Reason: Host is unknown

### ###################### ###
###    Import Tests:       ###
### ###################### ###

# Doesn't work on embedded tomcat (gretty farm)
# Fails to scan the classpath, while using new File(URI uri) with the uri value below:
# jar:file:/C:/code/rp-plugins/gradleTesting/build/libs/cdd-gradletesting-plugin.war!/WEB-INF/classes/
# We get the IllegalArgumentException("URI is not hierarchical").
# See: java.io.File constructor
#08 - Import Test Suites from Private Git
#    [Setup]  Generate Execution Identifier
#    ${importJson}=  Generate Gradle Import Tests Git JSON
#    ...             ${PRIVATE_SRC_REPO}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${MASTER_BRANCH}
#    ${response}=  Execute Import Loop  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
#    Should Be Equal As Strings  Done  ${response.status}
#    Should Be Equal As Strings  Import finished successfully  ${response.detailedInfo}
#    Validate Imported Test Suites Name  ${response.testSuites}  LibraryTest  TestShouldFailed  TestStringsShouldPass
#    Validate Imported Test Suites External Id  ${response.testSuites}
#    ...  :::test::LibraryTest  :::test::TestShouldFailed  :::test::TestStringsShouldPass

09 - Negative - Import Test Suites from Git without URL
    [Setup]  Generate Execution Identifier
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${EMPTY}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${MASTER_BRANCH}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Property \\[gitUrl\\] is missing

10 - Negative - Import Test Suites from Git with Mal URL
    [Setup]  Generate Execution Identifier
    Set Test Variable  ${badUrlRepo}  ThisIsNotAURL
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${badUrlRepo}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${MASTER_BRANCH}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Property \\[gitUrl\\] got invalid value \\[${badUrlRepo}\\]. Reason: URL is malformed

11 - Negative - Import Test Suites from Git with URL with unknown host
    [Setup]  Generate Execution Identifier
    Set Test Variable  ${inaccessibleRepo}  ${NON_EXISTING_GIT_REPO_DOMAIN}/CddRobot/gradle-testing-plugin.git
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${inaccessibleRepo}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${MASTER_BRANCH}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Failed accessing URL: \\[${inaccessibleRepo}\\]. Reason: Host is unknown

12 - Negative - Import Test Suites from Git unknown repository
    [Setup]  Generate Execution Identifier
    Set Test Variable  ${unknownRepo}  ${PRIVATE_GIT_REPO_DOMAIN}/CddRobot/gradle-testing-unknown-repo.git
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${unknownRepo}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${MASTER_BRANCH}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Remote repository not found

13 - Negative - Import Test Suites from Git private repository with bad credentials
    [Setup]  Generate Execution Identifier
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${PRIVATE_SRC_REPO}  badUser  badPassword  ${MASTER_BRANCH}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Failed to connect to git repository due to = ${PRIVATE_SRC_REPO}: not authorized

14 - Negative - Import Test Suites from Git non existing branch
    [Setup]  Generate Execution Identifier
    Set Test Variable  ${unknownBranch}  unknownBranch
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${PRIVATE_SRC_REPO}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${unknownBranch}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Failed to find item \\[${unknownBranch}\\] of type \\[branch\\]

15 - Negative - Import Test Suites from Git without branch
    [Setup]  Generate Execution Identifier
    ${importJson}=  Generate Gradle Import Tests Git JSON
    ...             ${PRIVATE_SRC_REPO}  ${PRIVATE_GIT_USER}  ${PRIVATE_GIT_PASSWORD}  ${EMPTY}
    ${response}=  Execute Import Tests Operation  ${GRADLE_TESTING_IMPORT_TESTS_URL}  ${importJson}
    Validate Import Execution Results  ${response}  ${FAILED}  Property \\[branch\\] is missing
