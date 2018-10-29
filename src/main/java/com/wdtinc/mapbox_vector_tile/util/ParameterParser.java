package com.wdtinc.mapbox_vector_tile.util;

import javax.servlet.http.HttpServletRequest;

public abstract class ParameterParser<T extends Parameter> {

    public abstract T parse(HttpServletRequest req);

    public static String getString(HttpServletRequest req, String name, String defaultValue) {
        String value = req.getParameter(name);
        if (value == null) {
            if (defaultValue == null)
                throw new IllegalArgumentException("need parameter " + name);
            else
                return defaultValue;
        }
        return value;
    }

    public static int getInt(HttpServletRequest req, String name, String defaultValue) {
        String value = getString(req, name, defaultValue);
        int intval;
        try {
            intval = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("unknown " + name + ":" + value);
        }
        return intval;
    }

    public static String[] getStringArray(HttpServletRequest req, String name, String[] defaultValue) {
        String[] value = req.getParameterValues(name);
        if (value == null){
            if (defaultValue == null)
                throw new IllegalArgumentException("need parameter " + name);
            else
                return defaultValue;
        }
        return value;
    }

    public static int[] getIntArray(HttpServletRequest req, String name, String[] defaultValue) {
        String[] values = getStringArray(req, name, null);
        int[] intvals = new int[values.length];
        int i = 0;
        try {
            for(; i < values.length; i++){
                intvals[i] = Integer.parseInt(values[i]);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("unknown " + name + ":" + values[i]);
        }
        return intvals;
    }
}
