package com.skocken.svgconverter.svg;

import static com.skocken.svgconverter.svg.SVGParsingMethods.getStringAttr;

import org.xml.sax.Attributes;

public class Properties {

    StyleSet styles = null;

    Attributes atts;

    Properties(Attributes atts) {
        this.atts = atts;
        String styleAttr = getStringAttr("style", atts);
        if (styleAttr != null) {
            styles = new StyleSet(styleAttr);
        }
    }

    public String getAttr(String name) {
        String v = null;
        if (styles != null) {
            v = styles.getStyle(name);
        }
        if (v == null) {
            v = getStringAttr(name, atts);
        }
        return v;
    }

    public String getString(String name) {
        return getAttr(name);
    }

    public Integer getHex(String name) {
        String v = getAttr(name);
        if (v == null || !v.startsWith("#")) {
            return null;
        } else {
            try {
                return Integer.parseInt(v.substring(1), 16);
            } catch (NumberFormatException nfe) {
                // todo - parse word-based color here
                return null;
            }
        }
    }

    public Float getFloat(String name, float defaultValue) {
        Float v = getFloat(name);
        if (v == null) {
            return defaultValue;
        } else {
            return v;
        }
    }

    public Float getFloat(String name) {
        String v = getAttr(name);
        if (v == null) {
            return null;
        } else {
            try {
                return Float.parseFloat(v);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }
}