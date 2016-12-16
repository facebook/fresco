/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.samples.zoomable;

import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Tap gesture listener for double tap to zoom / unzoom and double-tap-and-drag to zoom.
 *
 * @see ZoomableDraweeView#setTapListener(GestureDetector.SimpleOnGestureListener)
 */
public class DoubleTapGestureListener extends GestureDetector.SimpleOnGestureListener {
  private static final int DURATION_MS = 300;
  private static final int DOUBLE_TAP_SCROLL_THRESHOLD = 20;

  private final ZoomableDraweeView mDraweeView;
  private final PointF mDoubleTapViewPoint = new PointF();
  private final PointF mDoubleTapImagePoint = new PointF();
  private float mDoubleTapScale = 1;
  private boolean mDoubleTapScroll = false;

  public DoubleTapGestureListener(ZoomableDraweeView zoomableDraweeView) {
    mDraweeView = zoomableDraweeView;
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent e) {
    AbstractAnimatedZoomableController zc =
        (AbstractAnimatedZoomableController) mDraweeView.getZoomableController();
    PointF vp = new PointF(e.getX(), e.getY());
    PointF ip = zc.mapViewToImage(vp);
    switch (e.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        mDoubleTapViewPoint.set(vp);
        mDoubleTapImagePoint.set(ip);
        mDoubleTapScale = zc.getScaleFactor();
        break;
      case MotionEvent.ACTION_MOVE:
        mDoubleTapScroll = mDoubleTapScroll || shouldStartDoubleTapScroll(vp);
        if (mDoubleTapScroll) {
          float scale = calcScale(vp);
          zc.zoomToPoint(scale, mDoubleTapImagePoint, mDoubleTapViewPoint);
        }
        break;
      case MotionEvent.ACTION_UP:
        if (mDoubleTapScroll) {
          float scale = calcScale(vp);
          zc.zoomToPoint(scale, mDoubleTapImagePoint, mDoubleTapViewPoint);
        } else {
          final float maxScale = zc.getMaxScaleFactor();
          final float minScale = zc.getMinScaleFactor();
          if (zc.getScaleFactor() < (maxScale + minScale) / 2) {
            zc.zoomToPoint(
                maxScale,
                ip,
                vp,
                DefaultZoomableController.LIMIT_ALL,
                DURATION_MS,
                null);
          } else {
            zc.zoomToPoint(
                minScale,
                ip,
                vp,
                DefaultZoomableController.LIMIT_ALL,
                DURATION_MS,
                null);
          }
        }
        mDoubleTapScroll = false;
        break;
    }
    return true;
  }

  private boolean shouldStartDoubleTapScroll(PointF viewPoint) {
    double dist = Math.hypot(
        viewPoint.x - mDoubleTapViewPoint.x,
        viewPoint.y - mDoubleTapViewPoint.y);
    return dist > DOUBLE_TAP_SCROLL_THRESHOLD;
  }

  private float calcScale(PointF currentViewPoint) {
    float dy = (currentViewPoint.y - mDoubleTapViewPoint.y);
    float t = 1 + Math.abs(dy) * 0.001f;
    return (dy < 0) ? mDoubleTapScale / t : mDoubleTapScale * t;
  }
}
