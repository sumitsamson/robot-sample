
*** Keywords ***

Validate Manifest by name version uniqueId and vendor
    [Documentation]  Validate Manifest by name version uniqueId and vendor
    [Arguments]  ${manifest}  ${name}  ${version}  ${uniqueId}  ${vendor}

    ${pluginName}=  Extract String from Json By Path Json  ${manifest}  $.name
    Should Be Equal  ${pluginName}  ${name}

    ${pluginVersion}=  Extract String from Json By Path Json  ${manifest}  $.version
    Should Be Equal  ${pluginVersion}  ${version}

    ${pluginUniqueId}=  Extract String from Json By Path Json  ${manifest}  $.uniqueId
    Should Be Equal  ${pluginUniqueId}  ${uniqueId}

    ${pluginVendor}=  Extract String from Json By Path Json  ${manifest}  $.vendor
    Should Be Equal  ${pluginVendor}  ${vendor}
