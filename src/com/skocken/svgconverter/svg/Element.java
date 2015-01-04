package com.skocken.svgconverter.svg;

public class Element {
    private String name;
    private boolean canvasSave;

    public Element(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean hasCanvasSave() {
        return canvasSave;
    }

    public void setHasCanvasSave() {
        this.canvasSave = true;
    }

}
