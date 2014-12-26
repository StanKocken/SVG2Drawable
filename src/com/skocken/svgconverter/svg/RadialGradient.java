package com.skocken.svgconverter.svg;

public class RadialGradient extends Shader {

    private float mX;
    private float mY;
    private float mRadius;
    private int[] mColors;
    private float[] mPositions;

    private String mTileMode;

    public RadialGradient(float x, float y, float radius, int[] colors, float[] positions, String tileMode) {
        super();
        mX = x;
        mY = y;
        mRadius = radius;
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
        drawInstructions.add("shader = new RadialGradient(%ff, %ff, %ff, colors, positions, %s);", mX, mY, mRadius, mTileMode);
        drawInstructions.add("}");
        printMatrixTo(drawInstructions);
    }
}
