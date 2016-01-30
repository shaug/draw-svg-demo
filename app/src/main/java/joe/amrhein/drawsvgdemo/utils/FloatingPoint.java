package joe.amrhein.drawsvgdemo.utils;

public class FloatingPoint {
    public final float x;
    public final float y;

    public FloatingPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float distanceTo(FloatingPoint point) {
        return (float) Math.sqrt(Math.pow(point.x - this.x, 2) + Math.pow(point.y - this.y, 2));
    }
}
