package com.ironlog.train.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny reflective JSON mapper for the model classes.
 * Supports String, int, long, double, float, boolean, List&lt;Model&gt; and nested models.
 */
public final class Json {

    private Json() {
    }

    public static JSONObject toJson(Object o) {
        JSONObject j = new JSONObject();
        if (o == null) {
            return j;
        }
        for (Field f : o.getClass().getFields()) {
            try {
                Object v = f.get(o);
                if (v == null) {
                    continue;
                }
                j.put(f.getName(), wrap(v));
            } catch (Exception ignored) {
            }
        }
        return j;
    }

    private static Object wrap(Object v) throws Exception {
        if (v instanceof List) {
            JSONArray arr = new JSONArray();
            for (Object item : (List<?>) v) {
                arr.put(item == null ? JSONObject.NULL : wrap(item));
            }
            return arr;
        }
        if (v instanceof String || v instanceof Boolean
                || v instanceof Integer || v instanceof Long
                || v instanceof Double || v instanceof Float) {
            return v;
        }
        return toJson(v);
    }

    public static JSONArray toJsonArray(List<?> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (Object o : list) {
            arr.put(toJson(o));
        }
        return arr;
    }

    public static <T> List<T> fromJsonArray(JSONArray arr, Class<T> cls) {
        List<T> out = new ArrayList<>();
        if (arr == null) {
            return out;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject j = arr.optJSONObject(i);
            if (j != null) {
                out.add(fromJson(j, cls));
            }
        }
        return out;
    }

    public static <T> T fromJson(JSONObject j, Class<T> cls) {
        T out;
        try {
            out = cls.newInstance();
        } catch (Exception e) {
            return null;
        }
        for (Field f : cls.getFields()) {
            if (!j.has(f.getName())) {
                continue;
            }
            try {
                Object v = read(j, f.getName(), f.getType(), f.getGenericType());
                if (v != null) {
                    f.set(out, v);
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static Object read(JSONObject j, String name, Class<?> type, Type generic) throws Exception {
        if (type == String.class) {
            return j.optString(name, null);
        }
        if (type == int.class || type == Integer.class) {
            return j.optInt(name);
        }
        if (type == long.class || type == Long.class) {
            return j.optLong(name);
        }
        if (type == double.class || type == Double.class) {
            return j.optDouble(name);
        }
        if (type == float.class || type == Float.class) {
            return (float) j.optDouble(name);
        }
        if (type == boolean.class || type == Boolean.class) {
            return j.optBoolean(name);
        }
        if (List.class.isAssignableFrom(type)) {
            Class<?> elem = Object.class;
            if (generic instanceof ParameterizedType) {
                Type at = ((ParameterizedType) generic).getActualTypeArguments()[0];
                if (at instanceof Class) {
                    elem = (Class<?>) at;
                }
            }
            JSONArray arr = j.optJSONArray(name);
            if (elem == Object.class) {
                return null;
            }
            return fromJsonArray(arr, elem);
        }
        JSONObject inner = j.optJSONObject(name);
        return inner == null ? null : fromJson(inner, type);
    }
}
