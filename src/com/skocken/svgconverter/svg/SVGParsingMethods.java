package com.skocken.svgconverter.svg;

import java.util.ArrayList;

import org.xml.sax.Attributes;

public class SVGParsingMethods {

    static NumberParse parseNumbers(String s) {
        // Util.debug("Parsing numbers from: '" + s + "'");
        int n = s.length();
        int p = 0;
        ArrayList<Float> numbers = new ArrayList<Float>();
        boolean skipChar = false;
        for (int i = 1; i < n; i++) {
            if (skipChar) {
                skipChar = false;
                continue;
            }
            char c = s.charAt(i);
            switch (c) {
            // This ends the parsing, as we are on the next element
                case 'M':
                case 'm':
                case 'Z':
                case 'z':
                case 'L':
                case 'l':
                case 'H':
                case 'h':
                case 'V':
                case 'v':
                case 'C':
                case 'c':
                case 'S':
                case 's':
                case 'Q':
                case 'q':
                case 'T':
                case 't':
                case 'a':
                case 'A':
                case ')': {
                    String str = s.substring(p, i);
                    if (str.trim().length() > 0) {
                        // Util.debug("  Last: " + str);
                        Float f = Float.parseFloat(str);
                        numbers.add(f);
                    }
                    p = i;
                    return new NumberParse(numbers, p);
                }
                case '\n':
                case '\t':
                case ' ':
                case ',':
                case '-': {
                    String str = s.substring(p, i);
                    // Just keep moving if multiple whitespace
                    if (str.trim().length() > 0) {
                        // Util.debug("  Next: " + str);
                        Float f = Float.parseFloat(str);
                        numbers.add(f);
                        if (c == '-') {
                            p = i;
                        } else {
                            p = i + 1;
                            skipChar = true;
                        }
                    } else {
                        p++;
                    }
                    break;
                }
            }
        }
        String last = s.substring(p);
        if (last.length() > 0) {
            // Util.debug("  Last: " + last);
            try {
                numbers.add(Float.parseFloat(last));
            } catch (NumberFormatException nfe) {
                // Just white-space, forget it
            }
            p = s.length();
        }
        return new NumberParse(numbers, p);
    }

    static InstructionRecorder parseTransform(InstructionRecorder drawInstructions, String s) {
        drawInstructions.add("matrix.reset();");
        return parseTransformInternal(drawInstructions, s);
    }

    static InstructionRecorder parseTransformInternal(InstructionRecorder drawInstructions, String s) {
        int lastP = s.lastIndexOf("(");
        if (lastP > -1) {
            String left = s.substring(0, lastP).trim();
            NumberParse np = parseNumbers(s.substring(lastP+1));

            if(left.endsWith("matrix") && np.size() == 6) {
                // matrix
                drawInstructions.add("matrix.setValues(new float[]{ factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, 0, 0, factorScale});",
                        // Row 1
                        np.getNumber(0), np.getNumber(2), np.getNumber(4),
                        // Row 2
                        np.getNumber(1), np.getNumber(3), np.getNumber(5));
            } else if(left.endsWith("translate") && np.size() > 0) {
                // translate
                float tx = np.getNumber(0);
                float ty = 0;
                if (np.size() > 1) {
                    ty = np.getNumber(1);
                }
                drawInstructions.add("matrix.postTranslate(factorScale * %ff, factorScale * %ff);", tx, ty);
            } else if(left.endsWith("scale") && np.size() > 0) {
                float sx = np.getNumber(0);
                float sy = 0;
                if (np.size() > 1) {
                    sy = np.getNumber(1);
                }
                drawInstructions.add("matrix.postScale(factorScale * %ff, factorScale * %ff);", sx, sy);
            } else if(left.endsWith("skewX") && np.size() > 0) {
                float angle = np.getNumber(0);
                drawInstructions.add("matrix.postSkew(factorScale * %ff, 0);", (float) Math.tan(angle));
            } else if(left.endsWith("skewY") && np.size() > 0) {
                float angle = np.getNumber(0);
                drawInstructions.add("matrix.postSkew(0, factorScale * %ff);", (float) Math.tan(angle));
            } else if(left.endsWith("rotate") && np.size() > 0) {
                float angle = np.getNumber(0);
                float cx = 0;
                float cy = 0;
                if (np.size() > 2) {
                    cx = np.getNumber(1);
                    cy = np.getNumber(2);
                }
                if(cx != 0 || cy != 0) {
                    drawInstructions.add("matrix.postTranslate(factorScale * %ff, factorScale * %ff);", cx, cy);
                }
                drawInstructions.add("matrix.postRotate(%ff);", angle);
                if(cx != 0 || cy != 0) {
                    drawInstructions.add("matrix.postTranslate(factorScale * %ff, factorScale * %ff);", -cx, -cy);
                }
            }

            parseTransformInternal(drawInstructions, left);
        }
        return drawInstructions;
    }

    /**
     * This is where the hard-to-parse paths are handled.
     * Uppercase rules are absolute positions, lowercase are relative.
     * Types of path rules:
     * <p/>
     * <ol>
     * <li>M/m - (x y)+ - Move to (without drawing)
     * <li>Z/z - (no params) - Close path (back to starting point)
     * <li>L/l - (x y)+ - Line to
     * <li>H/h - x+ - Horizontal ine to
     * <li>V/v - y+ - Vertical line to
     * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
     * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1, y1 of this bezier)
     * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
     * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t. to current point)
     * </ol>
     * <p/>
     * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a - sign)
     *
     * @param s
     *            the path string from the XML
     */
    static void doPath(InstructionRecorder drawInstructions, String s) {
        int n = s.length();
        ParserHelper ph = new ParserHelper(s, 0);
        ph.skipWhitespace();
        drawInstructions.add("p.reset();");
        float lastX = 0;
        float lastY = 0;
        float lastX1 = 0;
        float lastY1 = 0;
        float subPathStartX = 0;
        float subPathStartY = 0;
        char prevCmd = 0;
        while (ph.pos < n) {
            char cmd = s.charAt(ph.pos);
            switch (cmd) {
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (prevCmd == 'm' || prevCmd == 'M') {
                        cmd = (char) (((int) prevCmd) - 1);
                        break;
                    } else if (prevCmd == 'c' || prevCmd == 'C') {
                        cmd = prevCmd;
                        break;
                    } else if (prevCmd == 'l' || prevCmd == 'L') {
                        cmd = prevCmd;
                        break;
                    }
                default: {
                    ph.advance();
                    prevCmd = cmd;
                }
            }

            boolean wasCurve = false;
            switch (cmd) {
                case 'M':
                case 'm': {
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'm') {
                        subPathStartX += x;
                        subPathStartY += y;
                        drawInstructions.add("p.rMoveTo(factorScale * %ff, factorScale * %ff);", x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        subPathStartX = x;
                        subPathStartY = y;
                        drawInstructions.add("p.moveTo(factorScale * %ff, factorScale * %ff);", x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'Z':
                case 'z': {
                    drawInstructions.add("p.close();");
                    drawInstructions.add("p.moveTo(factorScale * %ff, factorScale * %ff);", subPathStartX, subPathStartY);
                    lastX = subPathStartX;
                    lastY = subPathStartY;
                    lastX1 = subPathStartX;
                    lastY1 = subPathStartY;
                    wasCurve = true;
                    break;
                }
                case 'L':
                case 'l': {
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'l') {
                        drawInstructions.add("p.rLineTo(factorScale * %ff, factorScale * %ff);", x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        drawInstructions.add("p.lineTo(factorScale * %ff, factorScale * %ff);", x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'H':
                case 'h': {
                    float x = ph.nextFloat();
                    if (cmd == 'h') {
                        drawInstructions.add("p.rLineTo(factorScale * %ff, 0);", x);
                        lastX += x;
                    } else {
                        drawInstructions.add("p.lineTo(factorScale * %ff, factorScale * %ff);", x, lastY);
                        lastX = x;
                    }
                    break;
                }
                case 'V':
                case 'v': {
                    float y = ph.nextFloat();
                    if (cmd == 'v') {
                        drawInstructions.add("p.rLineTo(0, factorScale * %ff);", y);
                        lastY += y;
                    } else {
                        drawInstructions.add("p.lineTo(factorScale * %ff, factorScale * %ff);", lastX, y);
                        lastY = y;
                    }
                    break;
                }
                case 'C':
                case 'c': {
                    wasCurve = true;
                    float x1 = ph.nextFloat();
                    float y1 = ph.nextFloat();
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'c') {
                        x1 += lastX;
                        x2 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y2 += lastY;
                        y += lastY;
                    }
                    drawInstructions.add("p.cubicTo(factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff);", x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'S':
                case 's': {
                    wasCurve = true;
                    float x2 = ph.nextFloat();
                    float y2 = ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 's') {
                        x2 += lastX;
                        x += lastX;
                        y2 += lastY;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    drawInstructions.add("p.cubicTo(factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff);", x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'A':
                case 'a': {
                    float rx = ph.nextFloat();
                    float ry = ph.nextFloat();
                    float theta = ph.nextFloat();
                    int largeArc = (int) ph.nextFloat();
                    int sweepArc = (int) ph.nextFloat();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    drawArc(drawInstructions, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
                    lastX = x;
                    lastY = y;
                    break;
                }
            }
            if (!wasCurve) {
                lastX1 = lastX;
                lastY1 = lastY;
            }
            ph.skipWhitespace();
        }
    }

    static void drawArc(InstructionRecorder drawInstructions, float lastX, float lastY, float x, float y, float rx, float ry, float theta, int largeArc, int sweepArc) {
        // todo - not implemented yet, may be very hard to do using Android drawing facilities.
    }

    static NumberParse getNumberParseAttr(String name, Attributes attributes) {
        int n = attributes.getLength();
        for (int i = 0; i < n; i++) {
            if (attributes.getLocalName(i).equals(name)) {
                return parseNumbers(attributes.getValue(i));
            }
        }
        return null;
    }

    static String getStringAttr(String name, Attributes attributes) {
        int n = attributes.getLength();
        for (int i = 0; i < n; i++) {
            if (attributes.getLocalName(i).equals(name)) {
                return attributes.getValue(i);
            }
        }
        return null;
    }

    static Float getFloatAttr(String name, Attributes attributes) {
        return getFloatAttr(name, attributes, null);
    }

    static Float getFloatAttr(String name, Attributes attributes, Float defaultValue) {
        String v = getStringAttr(name, attributes);
        if (v == null) {
            return defaultValue;
        } else {
            if (v.endsWith("px")) {
                v = v.substring(0, v.length() - 2);
            }
            // Log.d(TAG, "Float parsing '" + name + "=" + v + "'");
            return Float.parseFloat(v);
        }
    }

    static Integer getHexAttr(String name, Attributes attributes) {
        String v = getStringAttr(name, attributes);
        // Util.debug("Hex parsing '" + name + "=" + v + "'");
        if (v == null) {
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
}
