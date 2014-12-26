package com.skocken.svgconverter.svg;

import java.util.ArrayList;

public class NumberParse {

    private ArrayList<Float> numbers;

    private int nextCmd;

    public NumberParse(ArrayList<Float> numbers, int nextCmd) {
        this.numbers = numbers;
        this.nextCmd = nextCmd;
    }

    public int getNextCmd() {
        return nextCmd;
    }

    public int size() {
        return numbers.size();
    }

    public float getNumber(int index) {
        return numbers.get(index);
    }

}
