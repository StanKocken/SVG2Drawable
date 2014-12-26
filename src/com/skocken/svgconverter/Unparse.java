package com.skocken.svgconverter;

import java.io.FileInputStream;

import com.skocken.svgconverter.svg.SVGParser;

public class Unparse {

    public static void main(String[] args) {

        if (args.length == 3) {
            try {
                SVGParser.getSVGFromInputStream(args[1], args[2], new FileInputStream(args[0]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Some paramaters are missing. Should be 'path', 'package name' and 'class name'.");
        }
    }
}
