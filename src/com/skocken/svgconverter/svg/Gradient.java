package com.skocken.svgconverter.svg;

// import android.graphics.Matrix;

import java.util.ArrayList;

public class Gradient {
    String id;
    String xlink;
    boolean isLinear;
    float x1, y1, x2, y2;
    float x, y, radius;
    ArrayList<Float> positions = new ArrayList<Float>();
    ArrayList<Integer> colors = new ArrayList<Integer>();

    InstructionRecorder matrixInstructions = new InstructionRecorder();

    public Gradient createChild(Gradient g) {
        Gradient child = new Gradient();
        child.id = g.id;
        child.xlink = id;
        child.isLinear = g.isLinear;
        child.x1 = g.x1;
        child.x2 = g.x2;
        child.y1 = g.y1;
        child.y2 = g.y2;
        child.x = g.x;
        child.y = g.y;
        child.radius = g.radius;
        child.positions = positions;
        child.colors = colors;
        child.matrixInstructions = matrixInstructions;
        if (g.matrixInstructions != null) {
            if (matrixInstructions == null) {
                child.matrixInstructions = g.matrixInstructions;
            } else {
                // Matrix m = new Matrix(matrixInstructions);
                // m.preConcat(g.matrixInstructions);
                // child.matrixInstructions = m;
                child.matrixInstructions = g.matrixInstructions;
            }
        }
        return child;
    }
}