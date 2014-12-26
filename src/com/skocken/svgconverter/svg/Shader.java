package com.skocken.svgconverter.svg;

public abstract class Shader {

    private InstructionRecorder matrixInstructions;

    public abstract void printTo(InstructionRecorder drawInstructions);

    protected void printMatrixTo(InstructionRecorder drawInstructions) {
        if (matrixInstructions == null || matrixInstructions.isEmpty()) {
            return;
        }
        drawInstructions.add(matrixInstructions);
        drawInstructions.add("shader.setLocalMatrix(matrix);");
    }

    public void setLocalMatrix(InstructionRecorder matrixInstructions) {
        this.matrixInstructions = matrixInstructions;
    }
}
