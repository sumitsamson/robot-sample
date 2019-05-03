package com.ca.cdd.plugins;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import java.io.IOException;
import java.util.List;

import static com.amazonaws.util.json.Jackson.getObjectMapper;

/**
 * Created by golro08 on 7/1/2016.
 *
 */
@RobotKeywords
public class NolioJsonUtils {

    public static final Configuration configuration = Configuration.defaultConfiguration();

    @RobotKeyword("Extract Long from Json By Path Json")
    public static Long extractLongFromJsonByPathJson(String json, String jsonPath) {
        return JsonPath.using(configuration).parse(json).read(jsonPath, Long.class);
    }

    @RobotKeyword("Extract Long from Json List By Path Json")
    public static Long extractLongFromJsonListByPathJson(String json, String jsonPath) {
        return JsonPath.using(configuration).parse(json).read(jsonPath, Long[].class)[0];
    }

    @RobotKeyword("Extract String from Json By Path Json")
    public static String extractStringFromJsonByPathJson(String json, String pathJson) {
        return JsonPath.using(configuration).parse(json).read(pathJson, String.class);
    }

    @RobotKeyword("Extract String from Json List By Path Json")
    public static String extractStringFromJsonListByPathJson(String json, String pathJson) {
        return JsonPath.using(configuration).parse(json).read(pathJson, String[].class)[0];
    }


    @RobotKeyword("Extract Boolean from Json By Path Json")
    public static Boolean extractBooleanFromJsonByPathJson(String json, String pathJson) {
        return JsonPath.using(configuration).parse(json).read(pathJson, Boolean.class);
    }

    @RobotKeyword("Extract Boolean from Json List By Path Json")
    public static Boolean extractBooleanFromJsonListByPathJson(String json, String pathJson) {
        return JsonPath.using(configuration).parse(json).read(pathJson, Boolean[].class)[0];
    }

    @RobotKeyword("Extract String List from Json List By Path Json")
    public static List extractStringListFromJsonListByPathJson(String json, String pathJson) {
        //noinspection unchecked
        return JsonPath.using(configuration).parse(json).read(pathJson, List.class);
    }

    @RobotKeyword("Validate String From Json By Path Json")
    public static void validateStringFromJsonByPathJson(String json, String jsonPath, String value) {
        final String extractedValue = extractStringFromJsonByPathJson(json, jsonPath);
        Assert.assertEquals(value, extractedValue);
    }

    @RobotKeyword("Validate Long From Json By Path Json")
    public static void validateLongFromJsonByPathJson(String json, String jsonPath, Long number) {
        final Long extractedNum = extractLongFromJsonByPathJson(json, jsonPath);
        Assert.assertEquals(number, extractedNum);
    }

    @RobotKeyword("Validate Boolean From Json By Path Json")
    public static void validateBooleanFromJsonByPathJson(String json, String jsonPath, Boolean value) {
        final Boolean extractedValue = extractBooleanFromJsonByPathJson(json, jsonPath);
        Assert.assertEquals(value, extractedValue);
    }

    @RobotKeyword("Add Field By Json Path")
    public static String addFieldByJsonPath(String json, String jsonPath, Object value) {
        return JsonPath.using(configuration).parse(json).add(jsonPath, value).jsonString();
    }

    @RobotKeyword("Add Object By Json Path")
    public static String addObjectByJsonPath(String json, String jsonPath, String key, Object value) {
        return JsonPath.using(configuration).parse(json).put(jsonPath, key, value).jsonString();
    }

    @RobotKeyword("Turn Object To Json")
    public String turnObjectToJson(Object dto) {
        return toJson(dto.getClass(),dto);
    }

    public static String toJson(final Class<?> c, final Object o) {
        final ObjectMapper mapper = getObjectMapper();
        try {
            return mapper.writer().withType(c).writeValueAsString(o);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @RobotKeyword("Parse Json From String")
    public static <T> T parseJsonFromString(String className, String jsonString) throws Exception {
        Class<T> aClass = (Class<T>) Class.forName(className);
        ObjectMapper mapper = PluginJsonObjectMapper.getJsonObjectMapper();
        try {
            return mapper.readValue(jsonString, aClass);
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
