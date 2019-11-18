package com.example.demo_camera2api;

import java.util.HashMap;
import java.util.Map;

public class Parameter {
    private static Map<String, String> SOURCE = new HashMap<String, String>();

    public static Map<String, String> getSOURCE() {
        SOURCE.put("-1", "IMAGE");
        SOURCE.put("0", "LENS_FACING_FRONT");
        SOURCE.put("1", "LENS_FACING_BACK");
        SOURCE.put("2", "LENS_FACING_EXTERNAL");
        return SOURCE;
    }
}
