package com.skocken.svgconverter.svg;

import static com.skocken.svgconverter.svg.SVGParsingMethods.doPath;
import static com.skocken.svgconverter.svg.SVGParsingMethods.getFloatAttr;
import static com.skocken.svgconverter.svg.SVGParsingMethods.getNumberParseAttr;
import static com.skocken.svgconverter.svg.SVGParsingMethods.getStringAttr;
import static com.skocken.svgconverter.svg.SVGParsingMethods.parseTransform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SVGHandler extends DefaultHandler {

    List<Element> elements = new ArrayList<Element>();

    HashMap<String, Shader> gradientMap = new HashMap<String, Shader>();

    HashMap<String, Gradient> gradientRefMap = new HashMap<String, Gradient>();

    Gradient gradient = null;

    int height, width;

    float viewBox[];

    InstructionRecorder drawInstructions = new InstructionRecorder();

    String packageName, className;

    SVGHandler(String packageName, String className) {
        this.packageName = packageName;
        this.className = className;

        drawInstructions.add("private RectF rect = new RectF();");
        drawInstructions.add("private Matrix matrix = new Matrix();");
        drawInstructions.add("private Shader shader;");
        drawInstructions.add("private Path p = new Path();");
        drawInstructions.add("private Paint paint = new Paint();");
        drawInstructions.add("@Override");
        drawInstructions.add("public void draw(Canvas canvas) {");
        drawInstructions.add("paint.setAntiAlias(true);");

        drawInstructions.add("float viewBoxWidth = VIEW_BOX[2] - VIEW_BOX[0];");
        drawInstructions.add("float viewBoxHeight = VIEW_BOX[3] - VIEW_BOX[1];");
        drawInstructions.add("Rect bounds = getBounds();");
        drawInstructions.add("if (viewBoxHeight <= 0 || viewBoxWidth <= 0 || bounds.width() <= 0 || bounds.height() <= 0) {");
        drawInstructions.add("return;");
        drawInstructions.add("}");
        drawInstructions.add("canvas.save();");
        drawInstructions.add("float viewBoxRatio = viewBoxWidth / (float) viewBoxHeight;");
        drawInstructions.add("float boundsRatio = bounds.width() / (float) bounds.height();");
        drawInstructions.add("float factorScale;");
        drawInstructions.add("if (boundsRatio > viewBoxRatio) {");
        drawInstructions.add(" // canvas larger than viewbox");
        drawInstructions.add(" factorScale = bounds.height() / (float) viewBoxHeight;");
        drawInstructions.add("} else {");
        drawInstructions.add(" // canvas higher (or equals) than viewbox");
        drawInstructions.add(" factorScale = bounds.width() / (float) viewBoxWidth;");
        drawInstructions.add("}");
        drawInstructions.add("int newViewBoxHeight = Math.round(factorScale * viewBoxHeight);");
        drawInstructions.add("int newViewBoxWidth = Math.round(factorScale * viewBoxWidth);");
        drawInstructions.add("int marginX = bounds.width() - newViewBoxWidth;");
        drawInstructions.add("int marginY = bounds.height() - newViewBoxHeight;");
        drawInstructions.add("canvas.translate(bounds.left, bounds.top);");
        drawInstructions.add("canvas.translate(-Math.round(factorScale * VIEW_BOX[0]), -Math.round(factorScale * VIEW_BOX[1]));");
        drawInstructions.add("canvas.translate(Math.round(marginX / 2f), Math.round(marginY / 2f));");
        drawInstructions.add("canvas.clipRect(bounds.left, bounds.top, bounds.left + newViewBoxWidth, bounds.top + newViewBoxHeight);");
    }

    public List<String> getDrawInstructions() {
        return drawInstructions.getListInstructions();
    }

    @Override
    public void startDocument() throws SAXException {
        // Set up prior to parsing a doc
    }

    @Override
    public void endDocument() throws SAXException {
        // Clean up after parsing a doc
        if (viewBox == null) {
            viewBox = new float[4];
            viewBox[0] = 0;
            viewBox[1] = 0;
            viewBox[2] = width;
            viewBox[3] = height;
        }
        drawInstructions.addBegin("private static final float[] VIEW_BOX = { %ff, %ff, %ff, %ff };", viewBox[0], viewBox[1], viewBox[2], viewBox[3]);
        drawInstructions.add("canvas.restore();");
        drawInstructions.add("}");

        drawInstructions.add("@Override public void setAlpha(int alpha) { }");
        drawInstructions.add("@Override public void setColorFilter(ColorFilter cf) { }");
        drawInstructions.add("@Override public int getOpacity() { return 0; }");
        drawInstructions.add("@Override public int getIntrinsicHeight() { return Math.round(VIEW_BOX[3] - VIEW_BOX[1]); }");
        drawInstructions.add("@Override public int getIntrinsicWidth() { return Math.round(VIEW_BOX[2] - VIEW_BOX[0]); }");
        drawInstructions.add("}");

        drawInstructions.addBegin("public class %s extends Drawable {", className);
        drawInstructions.addBegin("import android.graphics.Canvas;");
        drawInstructions.addBegin("import android.graphics.ColorFilter;");
        drawInstructions.addBegin("import android.graphics.Matrix;");
        drawInstructions.addBegin("import android.graphics.Paint;");
        drawInstructions.addBegin("import android.graphics.Path;");
        drawInstructions.addBegin("import android.graphics.Rect;");
        drawInstructions.addBegin("import android.graphics.RectF;");
        drawInstructions.addBegin("import android.graphics.LinearGradient;");
        drawInstructions.addBegin("import android.graphics.RadialGradient;");
        drawInstructions.addBegin("import android.graphics.Shader;");
        drawInstructions.addBegin("import android.graphics.drawable.Drawable;");
        drawInstructions.addBegin("package %s;", packageName);

    }

    private boolean doFill(Properties atts, HashMap<String, Shader> gradients) {
        if ("none".equals(atts.getString("display"))) {
            return false;
        }
        String fillString = atts.getString("fill");
        if (fillString != null && fillString.startsWith("url(#")) {
            // It's a gradient fill, look it up in our map
            String id = fillString.substring("url(#".length(), fillString.length() - 1);
            Shader shader = gradients.get(id);
            if (shader != null) {
                // Util.debug("Found shader!");
                shader.printTo(drawInstructions);
                drawInstructions.add("paint.setShader(shader);");
                drawInstructions.add("paint.setStyle(Paint.Style.FILL);");
                return true;
            } else {
                // Util.debug("Didn't find shader!");
                return false;
            }
        } else {
            drawInstructions.add("paint.setShader(null);");
            Integer color = atts.getHex("fill");
            if (color != null) {
                doColor(atts, color, true);
                drawInstructions.add("paint.setStyle(Paint.Style.FILL);");
                return true;
            } else if (atts.getString("fill") == null && atts.getString("stroke") == null) {
                // Default is black fill
                drawInstructions.add("paint.setStyle(Paint.Style.FILL);");
                drawInstructions.add("paint.setColor(0xFF000000);");
                return true;
            }
        }
        return false;
    }

    private boolean doStroke(Properties atts) {
        if ("none".equals(atts.getString("display"))) {
            return false;
        }
        Integer color = atts.getHex("stroke");
        if (color != null) {
            doColor(atts, color, false);
            // Check for other stroke attributes
            Float width = atts.getFloat("stroke-width");
            // Set defaults

            if (width != null) {
                drawInstructions.add("paint.setStrokeWidth(%ff);", width);
            }
            String linecap = atts.getString("stroke-linecap");
            if ("round".equals(linecap)) {
                drawInstructions.add("paint.setStrokeCap(Paint.Cap.ROUND);");
            } else if ("square".equals(linecap)) {
                drawInstructions.add("paint.setStrokeCap(Paint.Cap.SQUARE);");
            } else if ("butt".equals(linecap)) {
                drawInstructions.add("paint.setStrokeCap(Paint.Cap.BUTT);");
            }
            String linejoin = atts.getString("stroke-linejoin");
            if ("miter".equals(linejoin)) {
                drawInstructions.add("paint.setStrokeCap(Paint.Cap.MITER);");
            } else if ("round".equals(linejoin)) {
                drawInstructions.add("paint.setStrokeCap(Paint.Cap.ROUND);");
            } else if ("bevel".equals(linejoin)) {
                drawInstructions.add("paint.setStrokeCap(Paint.Cap.BEVEL);");
            }
            drawInstructions.add("paint.setStyle(Paint.Style.STROKE);");
            return true;
        }
        return false;
    }

    private Gradient doGradient(boolean isLinear, Attributes atts) {
        Gradient gradient = new Gradient();
        gradient.id = getStringAttr("id", atts);
        gradient.isLinear = isLinear;
        if (isLinear) {
            gradient.x1 = getFloatAttr("x1", atts, 0f);
            gradient.x2 = getFloatAttr("x2", atts, 0f);
            gradient.y1 = getFloatAttr("y1", atts, 0f);
            gradient.y2 = getFloatAttr("y2", atts, 0f);
        } else {
            gradient.x = getFloatAttr("cx", atts, 0f);
            gradient.y = getFloatAttr("cy", atts, 0f);
            gradient.radius = getFloatAttr("r", atts, 0f);
        }
        String transform = getStringAttr("gradientTransform", atts);
        if (transform != null) {
            gradient.matrixInstructions = parseTransform(new InstructionRecorder(), transform);
        }
        String xlink = getStringAttr("href", atts);
        if (xlink != null) {
            if (xlink.startsWith("#")) {
                xlink = xlink.substring(1);
            }
            gradient.xlink = xlink;
        }
        return gradient;
    }

    private void doColor(Properties atts, Integer color, boolean fillMode) {
        int c = (0xFFFFFF & color) | 0xFF000000;
        drawInstructions.add("paint.setColor(%d);", c);
        Float opacity = atts.getFloat("opacity");
        if (opacity == null) {
            opacity = atts.getFloat(fillMode ? "fill-opacity" : "stroke-opacity");
        }
        if (opacity == null) {
            drawInstructions.add("paint.setAlpha(255);");
        } else {
            drawInstructions.add("paint.setAlpha(%d);", (int) (255 * opacity));
        }
    }

    private boolean hidden = false;

    private int hiddenLevel = 0;

    private boolean boundsMode = false;

    private boolean pushTransform(Attributes atts) {
        final String transform = getStringAttr("transform", atts);
        if (transform != null) {
            parseTransform(drawInstructions, transform);
            drawInstructions.add("canvas.save();");
            drawInstructions.add("canvas.concat(matrix);");
            return true;
        } else {
            return false;
        }
    }

    private void popTransform() {
        drawInstructions.add("canvas.restore();");
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        // Reset paint opacity
        drawInstructions.add("paint.setAlpha(255);");
        if (localName == null || localName.length() == 0) {
            localName = qName;
        }

        Element element = new Element(localName);
        elements.add(0, element);

        // Ignore everything but rectangles in bounds mode
        if (boundsMode) {
            return;
        }
        if (localName.equals("svg")) {
            width = (int) Math.ceil(getFloatAttr("width", atts,0f));
            height = (int) Math.ceil(getFloatAttr("height", atts,0f));
            String viewBoxStr = getStringAttr("viewBox", atts);
            if (viewBoxStr != null) {
                String[] split = viewBoxStr.split(" ");
                if (split.length == 4) {
                    viewBox = new float[4];
                    viewBox[0] = Float.parseFloat(split[0]);
                    viewBox[1] = Float.parseFloat(split[1]);
                    viewBox[2] = Float.parseFloat(split[2]);
                    viewBox[3] = Float.parseFloat(split[3]);
                	width=(int) (viewBox[2]-viewBox[0]);
                    height=(int) (viewBox[3]-viewBox[1]);
                }
            }
        } else if (localName.equals("defs")) {
            // Ignore
        } else if (localName.equals("linearGradient")) {
            gradient = doGradient(true, atts);
        } else if (localName.equals("radialGradient")) {
            gradient = doGradient(false, atts);
        } else if (localName.equals("stop")) {
            if (gradient != null) {
                float offset = getFloatAttr("offset", atts);
                String styles = getStringAttr("style", atts);
                StyleSet styleSet = new StyleSet(styles);
                String colorStyle = styleSet.getStyle("stop-color");
                int color = 0xFF000000;
                if (colorStyle != null) {
                    if (colorStyle.startsWith("#")) {
                        color = Integer.parseInt(colorStyle.substring(1), 16);
                    } else {
                        color = Integer.parseInt(colorStyle, 16);
                    }
                }
                String opacityStyle = styleSet.getStyle("stop-opacity");
                if (opacityStyle != null) {
                    float alpha = Float.parseFloat(opacityStyle);
                    int alphaInt = Math.round(255 * alpha);
                    color |= (alphaInt << 24);
                } else {
                    color |= 0xFF000000;
                }
                gradient.positions.add(offset);
                gradient.colors.add(color);
            }
        } else if (localName.equals("g")) {
            // Check to see if this is the "bounds" layer
            if ("bounds".equalsIgnoreCase(getStringAttr("id", atts))) {
                boundsMode = true;
            }
            if (hidden) {
                hiddenLevel++;
                // Util.debug("Hidden up: " + hiddenLevel);
            }
            // Go in to hidden mode if display is "none"
            if ("none".equals(getStringAttr("display", atts))) {
                if (!hidden) {
                    hidden = true;
                    hiddenLevel = 1;
                    // Util.debug("Hidden up: " + hiddenLevel);
                }
            }
            if (pushTransform(atts)) {
                element.setHasCanvasSave();
            }
        } else if (!hidden && localName.equals("rect")) {
            Float x = getFloatAttr("x", atts);
            if (x == null) {
                x = 0f;
            }
            Float y = getFloatAttr("y", atts);
            if (y == null) {
                y = 0f;
            }
            Float width = getFloatAttr("width", atts);
            Float height = getFloatAttr("height", atts);
            if (pushTransform(atts)) {
                element.setHasCanvasSave();
            }
            Properties props = new Properties(atts);
            if (doFill(props, gradientMap)) {
                drawInstructions.add("canvas.drawRect(factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, paint);", x, y, x + width, y + height);
            }
            if (doStroke(props)) {
                drawInstructions.add("canvas.drawRect(factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, paint);", x, y, x + width, y + height);
            }
        } else if (!hidden && localName.equals("line")) {
            Float x1 = getFloatAttr("x1", atts);
            Float x2 = getFloatAttr("x2", atts);
            Float y1 = getFloatAttr("y1", atts);
            Float y2 = getFloatAttr("y2", atts);
            Properties props = new Properties(atts);
            if (doStroke(props)) {
                if (pushTransform(atts)) {
                    element.setHasCanvasSave();
                }
                drawInstructions.add("canvas.drawLine(factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff, paint);", x1, y1, x2, y2);
            }
        } else if (!hidden && localName.equals("circle")) {
            Float centerX = getFloatAttr("cx", atts);
            Float centerY = getFloatAttr("cy", atts);
            Float radius = getFloatAttr("r", atts);
            if (centerX != null && centerY != null && radius != null) {
                if (pushTransform(atts)) {
                    element.setHasCanvasSave();
                }
                Properties props = new Properties(atts);
                if (doFill(props, gradientMap)) {
                    drawInstructions.add("canvas.drawCircle(factorScale * %ff, factorScale * %ff, factorScale * %ff, paint);", centerX, centerY, radius);
                }
                if (doStroke(props)) {
                    drawInstructions.add("canvas.drawCircle(factorScale * %ff, factorScale * %ff, factorScale * %ff, paint);", centerX, centerY, radius);
                }
            }
        } else if (!hidden && localName.equals("ellipse")) {
            Float centerX = getFloatAttr("cx", atts);
            Float centerY = getFloatAttr("cy", atts);
            Float radiusX = getFloatAttr("rx", atts);
            Float radiusY = getFloatAttr("ry", atts);
            if (centerX != null && centerY != null && radiusX != null && radiusY != null) {
                if (pushTransform(atts)) {
                    element.setHasCanvasSave();
                }
                Properties props = new Properties(atts);
                drawInstructions.add("rect.set(factorScale * %ff, factorScale * %ff, factorScale * %ff, factorScale * %ff);", centerX - radiusX, centerY - radiusY, centerX + radiusX, centerY + radiusY);
                if (doFill(props, gradientMap)) {
                    drawInstructions.add("canvas.drawOval(rect, paint);");
                }
                if (doStroke(props)) {
                    drawInstructions.add("canvas.drawOval(rect, paint);");
                }
            }
        } else if (!hidden && (localName.equals("polygon") || localName.equals("polyline"))) {
            NumberParse numbers = getNumberParseAttr("points", atts);
            if (numbers != null) {
                drawInstructions.add("p.reset();");
                if (numbers.size() > 1) {
                    if (pushTransform(atts)) {
                        element.setHasCanvasSave();
                    }
                    Properties props = new Properties(atts);
                    drawInstructions.add("p.moveTo(factorScale * %ff, factorScale * %ff);", numbers.getNumber(0), numbers.getNumber(1));
                    for (int i = 2; i < numbers.size(); i += 2) {
                        float x = numbers.getNumber(i);
                        float y = numbers.getNumber(i + 1);
                        drawInstructions.add("p.lineTo(factorScale * %ff, factorScale * %ff);", x, y);
                    }
                    // Don't close a polyline
                    if (localName.equals("polygon")) {
                        drawInstructions.add("p.close();");
                    }
                    if (doFill(props, gradientMap)) {
                        drawInstructions.add("canvas.drawPath(p, paint);");
                    }
                    if (doStroke(props)) {
                        drawInstructions.add("canvas.drawPath(p, paint);");
                    }
                }
            }
        } else if (!hidden && localName.equals("path")) {
            doPath(drawInstructions, getStringAttr("d", atts));
            if (pushTransform(atts)) {
                element.setHasCanvasSave();
            }
            Properties props = new Properties(atts);
            if (doFill(props, gradientMap)) {
                // doLimits(p);
                drawInstructions.add("canvas.drawPath(p, paint);");
            }
            if (doStroke(props)) {
                drawInstructions.add("canvas.drawPath(p, paint);");
            }
        } else if (!hidden) {
        }
    }

    @Override
    public void characters(char ch[], int start, int length) {
        // no-op
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName == null || localName.length() == 0) {
            localName = qName;
        }

        if (localName.equals("svg")) {
        } else if (localName.equals("linearGradient")) {
            if (gradient.id != null) {
                if (gradient.xlink != null) {
                    Gradient parent = gradientRefMap.get(gradient.xlink);
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                int[] colors = new int[gradient.colors.size()];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = gradient.colors.get(i);
                }
                float[] positions = new float[gradient.positions.size()];
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = gradient.positions.get(i);
                }
                if (colors.length == 0) {
                }
                LinearGradient g = new LinearGradient(gradient.x1, gradient.y1, gradient.x2, gradient.y2, colors, positions, "Shader.TileMode.CLAMP");
                g.setLocalMatrix(gradient.matrixInstructions);
                gradientMap.put(gradient.id, g);
                gradientRefMap.put(gradient.id, gradient);
            }
        } else if (localName.equals("radialGradient")) {
            if (gradient.id != null) {
                if (gradient.xlink != null) {
                    Gradient parent = gradientRefMap.get(gradient.xlink);
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                int[] colors = new int[gradient.colors.size()];
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = gradient.colors.get(i);
                }
                float[] positions = new float[gradient.positions.size()];
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = gradient.positions.get(i);
                }
                if (gradient.xlink != null) {
                    Gradient parent = gradientRefMap.get(gradient.xlink);
                    if (parent != null) {
                        gradient = parent.createChild(gradient);
                    }
                }
                RadialGradient g = new RadialGradient(gradient.x, gradient.y, gradient.radius, colors, positions, "Shader.TileMode.CLAMP");
                g.setLocalMatrix(gradient.matrixInstructions);
                gradientMap.put(gradient.id, g);
                gradientRefMap.put(gradient.id, gradient);
            }
        } else if (localName.equals("g")) {
            if (boundsMode) {
                boundsMode = false;
            }
            // Break out of hidden mode
            if (hidden) {
                hiddenLevel--;
                // Util.debug("Hidden down: " + hiddenLevel);
                if (hiddenLevel == 0) {
                    hidden = false;
                }
            }
            // Clear gradient map
            gradientMap.clear();
        }

        Element element = pollLastElement(localName);
        if (element != null && element.hasCanvasSave()) {
            popTransform();
        }
    }

    private Element pollLastElement(String name) {
        if (elements.size() == 0) {
            return null;
        }
        Element element = elements.get(0);
        if (element != null && name != null && name.equals(element.getName())) {
            elements.remove(0);
            return element;
        } else {
            return null;
        }
    }
}