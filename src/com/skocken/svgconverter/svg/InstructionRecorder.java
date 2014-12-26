package com.skocken.svgconverter.svg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InstructionRecorder {

    private static final int MAX_LENGTH_LOG = 3800;

    private List<String> listInstructions = new ArrayList<String>();

    public void addBegin(String instructions, Object... args) {
        listInstructions.add(0, format(instructions, args));
    }

    public void add(String instructions, Object... args) {
        listInstructions.add(format(instructions, args));
    }

    public List<String> getListInstructions() {
        return listInstructions;
    }

    public boolean isEmpty() {
        return listInstructions.isEmpty();
    }

    public void print() {
        StringBuffer sb = new StringBuffer();
        for (String instruction : listInstructions) {
            if (sb.length() + instruction.length() > MAX_LENGTH_LOG) {
                System.out.println(sb.toString());
                sb.setLength(0);
            }
            sb.append(instruction);
            sb.append("\r\n");
        }
        System.out.println(sb.toString());
    }

    private String format(String instructions, Object[] args) {
        return String.format(Locale.ENGLISH, instructions, args);
    }

    public void add(InstructionRecorder instructions) {
        if (instructions == null || instructions.isEmpty()) {
            return;
        }
        listInstructions.addAll(instructions.listInstructions);
    }
}
