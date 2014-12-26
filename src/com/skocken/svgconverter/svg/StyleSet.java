package com.skocken.svgconverter.svg;

import java.util.HashMap;

public class StyleSet {

    HashMap<String, String> styleMap = new HashMap<String, String>();

    StyleSet(String string) {
        String[] styles = string.split(";");
        for (String s : styles) {
            String[] style = s.split(":");
            if (style.length == 2) {
                styleMap.put(style[0], style[1]);
            }
        }
    }

    public String getStyle(String name) {
        return styleMap.get(name);
    }
}