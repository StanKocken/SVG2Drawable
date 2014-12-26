package com.skocken.svgconverter.svg;

public class LinearGradient extends Shader {

    private float mX0;
    private float mY0;
    private float mX1;
    private float mY1;
    private int[] mColors;
    private float[] mPositions;

    private String mTileMode;

    public LinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, String tileMode) {
        super();
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
        mColors = colors;
        mPositions = positions;
        mTileMode = tileMode;
    }

    @Override
    public void printTo(InstructionRecorder drawInstructions) {
        drawInstructions.add("{");
        StringBuffer sb = new StringBuffer("int[] colors = ");
        if (mColors == null) {
            sb.append("null;");
        } else {
            sb.append(" { ");
            int startIterationLength = sb.length();
            for (int color : mColors) {
                if (sb.length() > startIterationLength) {
                    sb.append(", ");
                }
                sb.append(color);
            }
            sb.append(" }; ");
        }
        drawInstructions.add(sb.toString());
        sb = new StringBuffer("float[] positions = ");
        if (mPositions == null) {
            sb.append("null;");
        } else {
            sb.append(" { ");
            int startIterationLength = sb.length();
            for (float position : mPositions) {
                if (sb.length() > startIterationLength) {
                    sb.append(", ");
                }
                sb.append(position);
                sb.append("f");
            }
            sb.append(" }; ");
        }
        drawInstructions.add(sb.toString());
        drawInstructions.add("shader = new LinearGradient(%ff, %ff, %ff, %ff, colors, positions, %s);", mX0, mY0, mX1, mY1, mTileMode);
        drawInstructions.add("}");
        printMatrixTo(drawInstructions);
    }

}
