package co.hotwax.common;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.moqui.impl.context.ExecutionContextFactoryImpl;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.moqui.context.ExecutionContext;
import org.moqui.service.ServiceException;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import javax.cache.Cache;


public class SchemaValidation {
    protected  ExecutionContextFactoryImpl ecfi;
    protected  ExecutionContext ec;
    private  Cache<String, Schema>schemaCache;

    public SchemaValidation(ExecutionContextFactoryImpl ecfi,ExecutionContext ec) {
        this.ecfi = ecfi;
        this.ec = ec;
    }

    public void validateSchema(LinkedHashMap<String, Object> payload, String schemaLocation) {
        if (!(payload instanceof Map)) {
            throw new ServiceException("JSON Schema validation not supported for the passed object: " + payload.getClass().getCanonicalName());
        }

        JSONObject jsonObject = new JSONObject((Map<?, ?>) payload);

        Schema schema = getSchema(schemaLocation);

        try {
            schema.validate(jsonObject);
        } catch (ValidationException e) {
            throw new ServiceException("Schema validation failed: " + e.getAllMessages());
        }
    }

    private  Schema getSchema(String schemaLocation) {
        schemaCache = ecfi.cacheFacade.getCache("json.validation.schema");

        if (!schemaCache.containsKey(schemaLocation)) {
            try (InputStream inputStream = ec.getResource().getLocationStream(schemaLocation)) {
                if (inputStream == null) {
                    throw new ServiceException("Schema file not found: " + schemaLocation);
                }
                JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));
                Schema schema = SchemaLoader.load(schemaJson);
                schemaCache.put(schemaLocation, schema);
            } catch (Exception e) {
                throw new ServiceException("Error parsing schema at location: " + schemaLocation, e);
            }
        }
        return schemaCache.get(schemaLocation);
    }
}


