*** Settings ***
Documentation   A keyword library that gives bug management functionality
Variables     ../resources/sut.py
Library       com.ca.cdd.plugins.NolioRESTClient
Library  String
Library  Collections

*** Variables ***

# API Return Values
${OK_CODE}=  200
${BAD_REQUEST_CODE}=  400
${dynamicValuesLimit}=  400

# Task State
${FINISHED}=  FINISHED
${FAILED}=  FAILED

${GRADLE_TESTING_PLUGIN_URL}=  ${SERVER_URL}/cdd-gradletesting-plugin
${GRADLE_TESTING_API}=  ${GRADLE_TESTING_PLUGIN_URL}/api
${GRADLE_TESTING_CONNECTIVITY_URL}=  ${GRADLE_TESTING_API}/connectivity-tests/connect
${GRADLE_TESTING_IMPORT_TESTS_URL}=  ${GRADLE_TESTING_API}/test-sources/test-suites
${GRADLE_TESTING_MANIFEST_URL}=  ${GRADLE_TESTING_PLUGIN_URL}/manifest.json

*** Keywords ***

Generate Json Map From Dictionary
    [Arguments]  ${dictionary}
    ${keys}=  Get Dictionary Keys  ${dictionary}
    ${jsonMap}=  Set Variable  {
    :for  ${key}  in  @{keys}
    \  ${value}=  Get From Dictionary  ${dictionary}  ${key}
    \  ${jsonMap}=  Catenate  ${jsonMap}  "${key}": "${value}",
    ${jsonMap}=  Get Substring  ${jsonMap}  0  -1
    ${jsonMap}=  Catenate  ${jsonMap}  }
    [Return]  ${jsonMap}

# The convention for the pluginServiceContect is as follows:
# "<entityType>": { "key": "<entityId>", "value": "<entityName>" }
# For example: "application": { "key": 1234, "value": "MyApplication" }
Generate Search Context Properties By Name JSON
    [Arguments]  ${entityType}  ${entityName}
    Run Keyword And Return  Catenate
    ...  "pluginServiceContext": {
    ...      "${entityType}": { "key": 0, "value": "${entityName}" }
    ...  }

Generate Search Context Properties With Two Properties JSON
    [Arguments]  ${entity1Type}  ${entity1Name}  ${entity2Type}  ${entity2Name}
    Run Keyword And Return  Catenate
    ...  "pluginServiceContext": {
    ...      "${entity1Type}": { "key": 0, "value": "${entity1Name}" },
    ...      "${entity2Type}": { "key": 0, "value": "${entity2Name}" }
    ...  }

Get Items Values
    [Arguments]  @{items_list}
    ${output}=  Create List
    :for  ${item}  IN  @{items_list}
    \  Append To List  ${output}  ${item.value}
    [Return]  ${output}

Sort Dynamic Values By Value
    [Arguments]  @{items_list}

    ${map}=  Create Dictionary
    :for   ${item}  IN  @{items_list}
    \  Set To Dictionary  ${map}  ${item.value}  ${item}

    Run Keyword And Return  Return Sorted List By Dictionaory  ${map}

Sort Items By Name
    [Arguments]  @{items_list}

    ${map}=  Create Dictionary
    :for   ${item}  IN  @{items_list}
    \  Set To Dictionary  ${map}  ${item.name}  ${item}

    Run Keyword And Return  Return Sorted List By Dictionaory  ${map}

Sort Items By Id
    [Arguments]  @{items_list}

    ${map}=  Create Dictionary
    :for   ${item}  IN  @{items_list}
    \  Set To Dictionary  ${map}  ${item.id}  ${item}

    Run Keyword And Return  Return Sorted List By Dictionaory  ${map}

Sort Commits By Message
    [Arguments]  @{items_list}

    ${map}=  Create Dictionary
    :for   ${item}  IN  @{items_list}
    \  Set To Dictionary  ${map}  ${item.message}  ${item}

    Run Keyword And Return  Return Sorted List By Dictionaory  ${map}

Sort Imported Test Suites By External Id
    [Arguments]  @{items_list}

    ${map}=  Create Dictionary
    :for   ${item}  IN  @{items_list}
    \  Set To Dictionary  ${map}  ${item.externalId}  ${item}

    Run Keyword And Return  Return Sorted List By Dictionaory  ${map}

Sort Imported Test Suites By Creation Date
    [Arguments]  @{items_list}

    ${map}=  Create Dictionary
    :for   ${item}  IN  @{items_list}
    \  Set To Dictionary  ${map}  ${item.creationDate}  ${item}

    Run Keyword And Return  Return Sorted List By Dictionaory  ${map}

Return Sorted List By Dictionaory
    [Arguments]  ${items_dictionary}
    # How we are going to soirt the identities?
    # A. Create a map from the sort key to the entity (at previous level)
    # B. Get out the map keys (names) sorted.
    # C. Create the sorted list by extracting the items one by one.
    # If you find a better solution ... be my guest.

    # Accrdong to ROBOT documentation: If keys are sortable, they are returned in sorted order.
    # Since 'name' is string whihc is sortable the items listed are sorted.
    # Now we create the output using this list.
    @{keys_list}=  Get Dictionary Keys  ${items_dictionary}

    ${output_list}=  Create List
    :for   ${key}  IN  @{keys_list}
    \  ${item}=  Get From Dictionary  ${items_dictionary}  ${key}
    \  Append To List  ${output_list}  ${item}
    [Return]  ${output_list}

Dynamic Values Should Contain Values
    [Arguments]  ${actual_list}  @{expected_values}

    ${actual_values_list}=  Create List
    :for   ${item}  IN  @{actual_list}
    \  Append To List  ${actual_values_list}  ${item.value}

    :for  ${expected_value}  IN  @{expected_values}
    \  List Should Contain Value  ${actual_values_list}  ${expected_value}

Verify Error Within String Response
    [Arguments]  ${result}  ${exception}  ${message}
    Should Match Regexp  ${result}  ${exception}
    Should Match Regexp  ${result}  ${message}

Verify Several Optional Errors Within String Response
    [Arguments]  ${result}  ${exception}  @{optionalMmessages}
    Should Match Regexp  ${result}  ${exception}
    Should Match One Of Strings  ${result}  @{optionalMmessages}

Verify Generic Plugin Error
    [Arguments]  ${result}  ${code}  ${message}
    Should Be Equal As Strings  ${result.errorCode}  ${code}
    Should Be Equal As Strings  ${result.message}  ${message}

Verify Generic Plugin Error Regex Message
    [Arguments]  ${result}  ${code}  ${message}
    Should Be Equal As Strings  ${result.errorCode}  ${code}
    Should Match Regexp  ${result.message}  ${message}

Verify Plugin Error
    [Arguments]  ${result}  ${message}
    Run Keyword And Return  Verify Generic Plugin Error  ${result}  UNEXPECTED  ${message}

Validate Task Execution Results
    [Arguments]  ${result}  ${expectedStatus}  ${expectedState}  @{expectedMessages}
    Should Be Equal As Strings  ${result.executionStatus}  ${expectedStatus}
    Should Be Equal As Strings  ${result.status}  ${expectedState}
    :FOR  ${expectedMessage}  IN  @{expectedMessages}
    \  Should Match Regexp  ${result.detailedInfo}  ${expectedMessage}

Validate Task Execution Results Does Not Contain Messages
    [Arguments]  ${result}  @{notExpectedMessages}
    :FOR  ${notExpectedMessage}  IN  @{notExpectedMessages}
    \  Should Not Contain  ${result.detailedInfo}  ${notExpectedMessage}

Validate Import Execution Results
    [Arguments]  ${result}  ${expectedStatus}  @{expectedMessages}
    Should Be Equal As Strings  ${result.executionStatus}  ${expectedStatus}
    :FOR  ${expectedMessage}  IN  @{expectedMessages}
    \  Should Match Regexp  ${result.detailedInfo}  ${expectedMessage}

Validate Project List Results
    [Arguments]  ${result}  ${expected}
    Should Be Equal As Integers  ${result}  ${expected}

Validate String Response Results
    [Arguments]  ${result}  ${expected}
    Should Be Equal As Strings  ${result}  ${expected}

Validate Partial Dynamic Values
    [Arguments]  ${actualInputValues}  @{expectedValues}
    ${actualValues}=  Get Items Values  @{actualInputValues}
    List Should Contain Sub List  ${actualValues}  ${expectedValues}

Validate Dynamic Values
    [Arguments]  ${actualValues}  @{expectedValues}
    ${actualValuesCount}=  Get Length  ${actualValues}
    ${expectedValuesCount}=  Get Length  ${expectedValues}
    Should Be Equal As Integers  ${actualValuesCount}  ${expectedValuesCount}
    ${sortedActualValues}=  Sort Dynamic Values By Value  @{actualValues}
    :for  ${idx}  IN RANGE  ${actualValuesCount}
    \  Should Be Equal As Strings  ${sortedActualValues[${idx}].value}  ${expectedValues[${idx}]}

Validate Imported Test Suites Name
    [Arguments]  ${actualTestSuites}  @{expectedTestSuites}
    ${actualTestSuitesCount}=  Get Length  ${actualTestSuites}
    ${expectedTestSuitesCount}=  Get Length  ${expectedTestSuites}
    Should Be Equal As Integers  ${actualTestSuitesCount}  ${expectedTestSuitesCount}
    ${sortedActualTestSuites}=  Sort Items By Name  @{actualTestSuites}
    :for  ${idx}  IN RANGE  ${actualTestSuitesCount}
    \  Should Be Equal As Strings  ${sortedActualTestSuites[${idx}].name}  ${expectedTestSuites[${idx}]}

Validate Imported Test Suites External Id
    [Arguments]  ${actualTestSuites}  @{expectedTestSuites}
    ${actualTestSuitesCount}=  Get Length  ${actualTestSuites}
    ${expectedTestSuitesCount}=  Get Length  ${expectedTestSuites}
    Should Be Equal As Integers  ${actualTestSuitesCount}  ${expectedTestSuitesCount}
    ${sortedActualTestSuites}=  Sort Imported Test Suites By External Id  @{actualTestSuites}
    :for  ${idx}  IN RANGE  ${actualTestSuitesCount}
    \  Should Be Equal As Strings  ${sortedActualTestSuites[${idx}].externalId}  ${expectedTestSuites[${idx}]}

Validate Imported Test Suites Creation Date
    [Arguments]  ${actualTestSuites}  @{expectedTestSuites}
    ${actualTestSuitesCount}=  Get Length  ${actualTestSuites}
    ${expectedTestSuitesCount}=  Get Length  ${expectedTestSuites}
    Should Be Equal As Integers  ${actualTestSuitesCount}  ${expectedTestSuitesCount}
    ${sortedActualTestSuites}=  Sort Imported Test Suites By Creation Date  @{actualTestSuites}
    :for  ${idx}  IN RANGE  ${actualTestSuitesCount}
    \  Should Be Equal As Integers  ${sortedActualTestSuites[${idx}].creationDate}  ${expectedTestSuites[${idx}]}

Get Test Suite Result
    [Arguments]  ${testSuiteResult}
    ${systemOutputParameters}=  Convert To Dictionary  ${testSuiteResult.outSystemParameters}
    ${testSuiteJsonNode}=  Get From Dictionary  ${systemOutputParameters}  TEST_SUITE_RESULT
    ${testSuiteJsonString}=  Convert To String  ${testSuiteJsonNode}
    Run Keyword And Return  Parse Json From String  com.ca.rp.plugins.dto.model.impl.TestSuiteResultImpl  ${testSuiteJsonString}

Verify Test Case Result
    [Arguments]  ${testCase}  ${expectedName}  ${expectedExternalId}  ${expectedStatus}  ${expectedMessage}
    Should Be Equal As Strings   ${testCase.name}  ${expectedName}
    Should Be Equal As Strings   ${testCase.externalId}  ${expectedExternalId}
    Should Be Equal As Strings   ${testCase.executionStatus}  ${expectedStatus}
    Run Keyword If  '${expectedMessage}' == '${None}'
    ...  Should Be Equal As Strings   ${testCase.resultMessage}  ${expectedMessage}
    ...  ELSE
    ...  Should Match Regexp  ${testCase.resultMessage}  ${expectedMessage}
    Should Be True  ${testCase.executionStartDate} >= ${beforeExecution}
    Should Be True  ${testCase.executionEndDate} >= ${testCase.executionStartDate}
    Should Be True  ${afterExecution} >= ${testCase.executionEndDate}

Verify Test Suite Results
    [Arguments]  ${taskResult}  @{expectedTestCases}
    ${status}  ${testSuiteResult}=  Run Keyword And Ignore Error  Get Test Suite Result  ${taskResult}
    # If the execution failed due to timeout we will not have the test suite result so we exit gracefully.
    Return From Keyword If  '${status}' == 'FAIL'

    Should Be True  ${testSuiteResult.executionStartDate} >= ${beforeExecution}
    Should Be True  ${testSuiteResult.executionEndDate} >= ${testSuiteResult.executionStartDate}
    Should Be True  ${afterExecution} >= ${testSuiteResult.executionEndDate}

    ${sortedTestCases}=  Sort Items By Name  @{testSuiteResult.testCaseResults}
    ${testCasesCount}=  Get Length  ${sortedTestCases}
    ${testCasesCountFour}=  Evaluate  ${testCasesCount} * 4
    ${expectedTestCasesCount}=  Get Length  ${expectedTestCases}

    Should Be equal As Integers  ${testCasesCountFour}  ${expectedTestCasesCount}

    # Creating a list out of the values for better processing
    ${expectedTestCasesList}=  Create List
    :for  ${testCase}  in  @{expectedTestCases}
    \  Append To List  ${expectedTestCasesList}  ${testCase}

    ${expectedIdx}=  Set variable  0
    :for  ${testCase}  IN  @{sortedTestCases}
    \  ${expectedName}=  Get From List  ${expectedTestCasesList}  ${expectedIdx}
    \  ${expectedIdx}=  Evaluate  ${expectedIdx} + 1
    \  ${expectedExternalId}=  Get From List  ${expectedTestCasesList}  ${expectedIdx}
    \  ${expectedIdx}=  Evaluate  ${expectedIdx} + 1
    \  ${expectedStatus}=  Get From List  ${expectedTestCasesList}  ${expectedIdx}
    \  ${expectedIdx}=  Evaluate  ${expectedIdx} + 1
    \  ${expectedMessage}=  Get From List  ${expectedTestCasesList}  ${expectedIdx}
    \  ${expectedIdx}=  Evaluate  ${expectedIdx} + 1
    \  Verify Test Case Result  ${testCase}  ${expectedName}  ${expectedExternalId}  ${expectedStatus}  ${expectedMessage}

Mark Before Execution Time
    ${now}=  Get time  epoch
    ${nowMilli}=  Evaluate  ${now} * 1000
    Set Test Variable  ${beforeExecution}  ${nowMilli}

Mark After Execution Time
    ${now}=  Get time  epoch
    #Becasue we have resolution differnce between seconds and milliseconds we take the upper limit which is one second ahead.
    ${nowMilli}=  Evaluate  (${now} + 1) * 1000
    Set Test Variable  ${afterExecution}  ${nowMilli}

Generate Continue Execution JSON
    [Arguments]  ${endpoint}  ${executionContext}  ${taskProperties}  ${executorId}
    ${executionContextJson}=  Generate Json Map From Dictionary  ${executionContext}
    Run Keyword And Return  Catenate
    ...  {
    ...      ${endpoint},
    ...      "executorId": "${executorId}",
    ...      "executionContext": ${executionContextJson},
    ...      "taskProperties": ${taskProperties}
    ...  }

Check Executed Task Status
    [Arguments]  ${executionUrl}  ${endpoint}  ${taskProperties}  ${executionContext}  ${executorId}  ${expectedStatus}
    ${execution}=  Generate Continue Execution JSON  ${endpoint}  ${executionContext}  ${taskProperties}  ${executorId}
    ${result}=  Execute Task  ${executionUrl}  ${execution}  execute
    Set Test Variable  ${currentResult}  ${result}
    Should Be Equal As Strings  ${expectedStatus}  ${result.status}

Check Executed Task Execution Parameter
    [Arguments]  ${executionUrl}  ${endpoint}  ${taskProperties}  ${executionContext}  ${executorId}  ${executionParameterKey}  ${expectedParameterValue}
    ${execution}=  Generate Continue Execution JSON  ${endpoint}  ${executionContext}  ${taskProperties}  ${executorId}
    ${result}=  Execute Task  ${executionUrl}  ${execution}  execute
    Set Test Variable  ${currentResult}  ${result}

    ${afterValue}=  Get From Dictionary  ${result.executionContext}  ${executionParameterKey}
    Should Be Equal As Strings  ${expectedParameterValue}  ${afterValue}

Log Dto To Console
    [Arguments]  ${dto}
    ${dto_json}=  Turn Object To Json  ${dto}
    Log To Console  DTO: ${dto_json}

Should Match One Of Strings
    [Arguments]  ${actualString}  @{expectedStrings}
    :for  ${expectedString}  IN  @{expectedStrings}
    \  ${passed}=  Run Keyword And Return Status  Should Match Regexp  ${actualString}  ${expectedString}
    \  Run Keyword If 	${passed} 	Return From Keyword
    Fail  ${actualString} was not expected.

Validate Build Number From Result
    [Arguments]  ${response}  ${expected}
    ${buildNumber}=  Get From Dictionary  ${response.outSystemParameters}  DEPLOYED_BUILD_NUMBER
    Should Be Equal As Strings  ${buildNumber}  ${expected}

Verify Successfull Connectivity
    [Arguments]  ${result}
    Should Be True  ${result.success}

Verify Failure Connectivity
    [Arguments]  ${result}  ${message}
    Should Not Be True  ${result.success}
    Should Be Equal As Strings  ${message}  ${result.errorMessage}

Find Item in Parameters List By Name
    [Arguments]  ${parameterName}  ${itemToFind}  @{parameters}
    :FOR  ${currentParameter}  IN  @{parameters}
    \  Return From Keyword If  '${currentParameter.${parameterName}}' == '${itemToFind}'  ${currentParameter}
    [Return]  ${EMPTY}