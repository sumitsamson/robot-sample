import ConfigParser
from robot.libraries.BuiltIn import BuiltIn

def get_variables():
    # Read the sut from sut.txt
    config = ConfigParser.RawConfigParser()
    config.optionxform=str
    resourceDir=BuiltIn().replace_variables('${RESOURCES}')
    config.read(resourceDir+'/sut.txt')
    sut_list = config.items('SUT')
    result = dict(sut_list)

    # Get existing variables - to know if we got something from the command line
    existingVariables = BuiltIn().get_variables()
    #print 'existing variables:';
    #print existingVariables;

    #Override values in the SUT file with ones we got from the command line
    for varName in result:
        formattedVarName = '${%s}' % varName
        if formattedVarName in existingVariables:
            result[varName]=existingVariables[formattedVarName]

    # result['SERVER_URL']='%(HOST)s:%(PORT)s/' % result
    result['SERVER_URL']='%(SCHEME)s://%(HOST)s:%(PORT)s' % result

    print 'resulting variables:'
    print result

    return result
