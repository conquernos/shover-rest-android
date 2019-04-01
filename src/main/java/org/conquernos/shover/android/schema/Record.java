package org.conquernos.shover.android.schema;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.conquernos.shover.android.ShoverMessage;
import org.conquernos.shover.android.exceptions.schema.ShoverSchemaException;

import java.lang.reflect.Field;

public class Record {

    public static JsonObject makeRecordJsonObject(ShoverMessage message) throws ShoverSchemaException {
        JsonObject recordValue = new JsonObject();
        Class<?> clazz = message.getClass();
        for (Field field : clazz.getFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(message);
                if (value instanceof String) recordValue.addProperty(field.getName(), (String) value);
                else if (value instanceof Number) recordValue.addProperty(field.getName(), (Number) value);
                else if (value instanceof Boolean) recordValue.addProperty(field.getName(), (Boolean) value);
                else if (value instanceof Character) recordValue.addProperty(field.getName(), (Character) value);
                else throw new ShoverSchemaException("the data type of value is wrong");
            } catch (IllegalAccessException e) {
                throw new ShoverSchemaException(e);
            }
        }

        JsonObject record = new JsonObject();
        record.add("value", recordValue);

        return record;
    }

    public static JsonObject makeRecordJsonObject(Object... values) throws ShoverSchemaException {
        JsonArray recordValue = new JsonArray();
        for (Object value : values) {
            if (value instanceof String) recordValue.add((String) value);
            else if (value instanceof Number) recordValue.add((Number) value);
            else if (value instanceof Boolean) recordValue.add((Boolean) value);
            else if (value instanceof Character) recordValue.add((Character) value);
            else throw new ShoverSchemaException("the data type of value is wrong");
        }

        JsonObject record = new JsonObject();
        record.add("value", recordValue);

        return record;
    }

}
