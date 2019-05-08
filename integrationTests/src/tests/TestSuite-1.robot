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
