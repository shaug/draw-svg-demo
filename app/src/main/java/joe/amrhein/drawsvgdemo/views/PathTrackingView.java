package joe.amrhein.drawsvgdemo.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import joe.amrhein.drawsvgdemo.utils.Bezier;
import joe.amrhein.drawsvgdemo.utils.ControlFloatingPoints;
import joe.amrhein.drawsvgdemo.utils.FloatingPoint;


public class PathTrackingView extends View {

    private static final float STROKE_WIDTH = 5f;

    /**
     * Need to track this so the dirty region can accommodate the stroke.
     **/
    private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
    private static final String TAG = PathTrackingView.class.getSimpleName();

    //View state
    private List<FloatingPoint> points;
    private boolean isEmpty;
    private float lastTouchX;
    private float lastTouchY;
    private RectF dirtyRect;

    private Paint paint = new Paint();
    private Bitmap viewBitmap = null;
    private Canvas canvas = null;

    private LinkedList<List<FloatingPoint>> svgPaths = new LinkedList<>();
    private List<FloatingPoint> svgPath = new ArrayList<>();


    public PathTrackingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Fixed parameters
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(STROKE_WIDTH);

        //Dirty rectangle to update only the changed portion of the view
        dirtyRect = new RectF();

        clear();
    }


    public LinkedList<List<FloatingPoint>> getSvgPaths() {
        return svgPaths;
    }


    public void clear() {
        points = new ArrayList<>();
        svgPaths = new LinkedList<>();
        svgPath = new ArrayList<>();

        if (viewBitmap != null) {
            viewBitmap = null;
            ensureViewBitmap();
        }

        setIsEmpty(true);

        invalidate();
    }


    public void removeLastPath() {
        if (svgPaths.isEmpty()) {
            Log.d(TAG, "No stored paths");
            return;
        }

        svgPaths.removeLast();

        if (viewBitmap != null) {
            viewBitmap = null;
            ensureViewBitmap();
        }

        if (svgPaths.isEmpty()) {
            setIsEmpty(true);
        } else {
            List<FloatingPoint> aPath;
            for (int i = 0; i < svgPaths.size(); i++) {
                aPath = svgPaths.get(i);
                points.clear();
                for (int j = 0; j < aPath.size(); j++) {
                    addPoint(aPath.get(j));
                }
            }
        }

        invalidate();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        float eventX = event.getX();
        float eventY = event.getY();

        FloatingPoint p = new FloatingPoint(eventX, eventY);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                points.clear();
                svgPath = new ArrayList<>();
                lastTouchX = eventX;
                lastTouchY = eventY;
                addPoint(p);
                svgPath.add(p);

            case MotionEvent.ACTION_MOVE:
                resetDirtyRect(eventX, eventY);
                addPoint(p);
                svgPath.add(p);
                break;

            case MotionEvent.ACTION_UP:
                resetDirtyRect(eventX, eventY);
                addPoint(p);
                getParent().requestDisallowInterceptTouchEvent(true);
                setIsEmpty(false);
                svgPath.add(p);
                svgPaths.add(svgPath);
                break;

            default:
                return false;
        }


        invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

        return true;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (viewBitmap != null) {
            canvas.drawBitmap(viewBitmap, 0, 0, paint);
        }
    }


    public boolean isEmpty() {
        return isEmpty;
    }


    private void addPoint(FloatingPoint newPoint) {
        points.add(newPoint);
        if (points.size() > 2) {
            // To reduce the initial lag make it work with 3 points
            // by copying the first point to the beginning.
            if (points.size() == 3) {
                points.add(0, points.get(0));
            }

            ControlFloatingPoints tmp = calculateCurveControlPoints(points.get(0), points.get(1),
                    points.get(2));
            FloatingPoint c2 = tmp.c2;
            tmp = calculateCurveControlPoints(points.get(1), points.get(2), points.get(3));
            FloatingPoint c3 = tmp.c1;
            Bezier curve = new Bezier(points.get(1), c2, c3, points.get(2));

            addBezier(curve);

            // Remove the first element from the list,
            // so that we always have no more than 4 points in points array.
            points.remove(0);
        }
    }


    private void addBezier(Bezier curve) {
        ensureViewBitmap();
        float drawSteps = (float) Math.floor(curve.length());

        for (int i = 0; i < drawSteps; i++) {
            // Calculate the Bezier (x, y) coordinate for this step.
            float t = ((float) i) / drawSteps;
            float tt = t * t;
            float ttt = tt * t;
            float u = 1 - t;
            float uu = u * u;
            float uuu = uu * u;

            float x = uuu * curve.startPoint.x;
            x += 3 * uu * t * curve.control1.x;
            x += 3 * u * tt * curve.control2.x;
            x += ttt * curve.endPoint.x;

            float y = uuu * curve.startPoint.y;
            y += 3 * uu * t * curve.control1.y;
            y += 3 * u * tt * curve.control2.y;
            y += ttt * curve.endPoint.y;

            canvas.drawPoint(x, y, paint);
            expandDirtyRect(x, y);
        }
    }


    private ControlFloatingPoints calculateCurveControlPoints(FloatingPoint s1, FloatingPoint s2,
            FloatingPoint s3) {
        float dx1 = s1.x - s2.x;
        float dy1 = s1.y - s2.y;
        float dx2 = s2.x - s3.x;
        float dy2 = s2.y - s3.y;

        FloatingPoint m1 = new FloatingPoint((s1.x + s2.x) / 2.0f, (s1.y + s2.y) / 2.0f);
        FloatingPoint m2 = new FloatingPoint((s2.x + s3.x) / 2.0f, (s2.y + s3.y) / 2.0f);

        float l1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
        float l2 = (float) Math.sqrt(dx2 * dx2 + dy2 * dy2);

        float dxm = (m1.x - m2.x);
        float dym = (m1.y - m2.y);
        float k = l2 / (l1 + l2);
        FloatingPoint cm = new FloatingPoint(m2.x + dxm * k, m2.y + dym * k);

        float tx = s2.x - cm.x;
        float ty = s2.y - cm.y;

        return new ControlFloatingPoints(new FloatingPoint(m1.x + tx, m1.y + ty),
                new FloatingPoint(m2.x + tx, m2.y + ty));
    }


    /**
     * Called when replaying history to ensure the dirty region includes all points.
     *
     * @param historicalX the previous x coordinate.
     * @param historicalY the previous y coordinate.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < dirtyRect.left) {
            dirtyRect.left = historicalX;
        } else if (historicalX > dirtyRect.right) {
            dirtyRect.right = historicalX;
        }
        if (historicalY < dirtyRect.top) {
            dirtyRect.top = historicalY;
        } else if (historicalY > dirtyRect.bottom) {
            dirtyRect.bottom = historicalY;
        }
    }


    /**
     * Resets the dirty region when the motion event occurs.
     *
     * @param eventX the event x coordinate.
     * @param eventY the event y coordinate.
     */
    private void resetDirtyRect(float eventX, float eventY) {
        // The lastTouchX and lastTouchY were set when the ACTION_DOWN motion event occurred.
        dirtyRect.left = Math.min(lastTouchX, eventX);
        dirtyRect.right = Math.max(lastTouchX, eventX);
        dirtyRect.top = Math.min(lastTouchY, eventY);
        dirtyRect.bottom = Math.max(lastTouchY, eventY);
    }


    private void setIsEmpty(boolean newValue) {
        isEmpty = newValue;
    }


    private void ensureViewBitmap() {
        if (viewBitmap == null) {
            viewBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(viewBitmap);
        }
    }
}

