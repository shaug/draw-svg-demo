package joe.amrhein.drawsvgdemo.svg;

import java.util.LinkedList;
import java.util.List;

import joe.amrhein.drawsvgdemo.utils.FloatingPoint;


/**
 * Static helper methods to serialize paths to SVG syntax
 */
public class SVGSerializer {

    public static String serializeToSVG(int screenX, int screenY,
            LinkedList<List<FloatingPoint>> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\">");
        sb.append("x=\"0px\" y=\"0px\"");
        sb.append("viewBox=\"0 0 ").append(screenX).append(" ").append(screenY).append("\"");
        sb.append("style=\"enable-background:new 0 0 ").append(screenX).append(" ").append
                (screenY).append(";\"");
        for (int i = 0; i < paths.size(); i++) {
            createPathElement(sb, paths.get(i));
        }

        sb.append("</svg>");
        return sb.toString();
    }


    public static void createPathElement(StringBuilder sb, List<FloatingPoint> points) {
        if (points == null || points.size() < 2) {
            return;
        }
        sb.append("<path stroke=\"black\" fill=\"none\" d=\"");

        FloatingPoint p = points.get(0);

        sb.append("M").append(p.x).append(",").append(p.y);


        int size = points.size();
        for (int i = 1; i < size; i++) {
            p = points.get(i);

            sb.append("L").append(p.x).append(",").append(p.y);
        }

        sb.append("\"/>");
    }
}
