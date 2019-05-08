package com.ca.cdd.plugins;


import com.ca.rp.plugins.dto.metadata.PossibleValue;
import com.ca.rp.plugins.dto.metadata.PossibleValueWithParameters;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Imported from CDD for mapping the Manifest Json
 *  Created by asssh01 on 5/4/2017.
 */
public class PluginJsonObjectMapper {

    private static final ObjectMapper jsonObjectMapper = new ObjectMapper();

    static {
        initObjectMapper();
    }

    public static ObjectMapper getJsonObjectMapper() { return jsonObjectMapper;}


    private static void initObjectMapper() {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(PossibleValue.class, new PossibleValueDeserializer(jsonObjectMapper));
        jsonObjectMapper.registerModule(module);

    }

    public static class PossibleValueDeserializer extends JsonDeserializer<PossibleValue> {

        private final ObjectMapper mapper;

        public PossibleValueDeserializer(ObjectMapper mapper) {
            this.mapper = mapper;
        }


        @Override
        public PossibleValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode jn = jp.getCodec().readValue(jp, JsonNode.class);
            if (jn.isTextual()) {
                PossibleValue result = new PossibleValue();
                result.setValue(jn.asText());
                return result;
            }
            return
                    mapper.treeToValue(jn,PossibleValueWithParameters.class);
        }
    }

}
