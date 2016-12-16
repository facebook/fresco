package com.facebook.samples.zoomable;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * DoubleTapListener for zoom or turn back.
 */
public class ZoomDoubleTabListener implements GestureDetector.OnDoubleTapListener {
    private DefaultZoomableController mZoomableController;

    public ZoomDoubleTabListener(DefaultZoomableController zoomableController) {
        mZoomableController = zoomableController;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        float maxScaleFactor = mZoomableController.getMaxScaleFactor();
        RectF bounds = mZoomableController.getImageBounds();
        float x = e.getX();
        float y = e.getY();
        PointF imagePoint = new PointF((x - bounds.left) / bounds.width(), (y - bounds.top) / bounds.height());
        float scaleFactor = mZoomableController.getScaleFactor() < maxScaleFactor ? maxScaleFactor : mZoomableController.getMinScaleFactor();
        if (mZoomableController instanceof AbstractAnimatedZoomableController) {
            ((AbstractAnimatedZoomableController) mZoomableController).zoomToPoint(scaleFactor, imagePoint, new PointF(x, y), DefaultZoomableController.LIMIT_ALL, 200, null);
        } else {
            mZoomableController.zoomToPoint(scaleFactor, imagePoint, new PointF(x, y));
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }
}
