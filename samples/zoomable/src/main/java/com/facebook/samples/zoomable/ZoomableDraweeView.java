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

import javax.annotation.Nullable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;

/**
 * DraweeView that has zoomable capabilities.
 * <p>
 * Once the image loads, pinch-to-zoom and translation gestures are enabled.
 *
 */
public class ZoomableDraweeView extends DraweeView<GenericDraweeHierarchy>
    implements ZoomableController.Listener {

  private static final Class<?> TAG = ZoomableDraweeView.class;

  private static final float HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f;

  private final RectF mImageBounds = new RectF();
  private final RectF mViewBounds = new RectF();

  private final ControllerListener mControllerListener = new BaseControllerListener<Object>() {
    @Override
    public void onFinalImageSet(
        String id,
        @Nullable Object imageInfo,
        @Nullable Animatable animatable) {
      ZoomableDraweeView.this.onFinalImageSet();
    }

    @Override
    public void onRelease(String id) {
      ZoomableDraweeView.this.onRelease();
    }
  };

  private DraweeController mHugeImageController;
  private ZoomableController mZoomableController = DefaultZoomableController.newInstance();

  public ZoomableDraweeView(Context context) {
    super(context);
    init();
  }

  public ZoomableDraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public ZoomableDraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    mZoomableController.setListener(this);
  }

  public void setZoomableController(ZoomableController zoomableController) {
    Preconditions.checkNotNull(zoomableController);
    mZoomableController.setListener(null);
    mZoomableController = zoomableController;
    mZoomableController.setListener(this);
  }

  @Override
  public void setController(@Nullable DraweeController controller) {
    setControllers(controller, null);
  }

  private void setControllersInternal(
      @Nullable DraweeController controller,
      @Nullable DraweeController hugeImageController) {
    removeControllerListener(getController());
    addControllerListener(controller);
    mHugeImageController = hugeImageController;
    super.setController(controller);
  }

    /**
     * Sets the controllers for the normal and huge image.
     *
     * <p> IMPORTANT: in order to avoid a flicker when switching to the huge image, the huge image
     * controller should have the normal-image-uri set as its low-res-uri.
     *
     * @param controller controller to be initially used
     * @param hugeImageController controller to be used after the client starts zooming-in
     */
  public void setControllers(
      @Nullable DraweeController controller,
      @Nullable DraweeController hugeImageController) {
    setControllersInternal(null, null);
    mZoomableController.setEnabled(false);
    setControllersInternal(controller, hugeImageController);
  }

  private void maybeSetHugeImageController() {
    if (mHugeImageController != null &&
        mZoomableController.getScaleFactor() > HUGE_IMAGE_SCALE_FACTOR_THRESHOLD) {
      setControllersInternal(mHugeImageController, null);
    }
  }

  private void removeControllerListener(DraweeController controller) {
    if (controller instanceof AbstractDraweeController) {
      ((AbstractDraweeController) controller)
          .removeControllerListener(mControllerListener);
    }
  }

  private void addControllerListener(DraweeController controller) {
    if (controller instanceof AbstractDraweeController) {
      ((AbstractDraweeController) controller)
          .addControllerListener(mControllerListener);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int saveCount = canvas.save();
    canvas.concat(mZoomableController.getTransform());
    super.onDraw(canvas);
    canvas.restoreToCount(saveCount);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (mZoomableController.onTouchEvent(event)) {
      if (mZoomableController.getScaleFactor() > 1.0f) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      FLog.v(TAG, "onTouchEvent: view %x, handled by zoomable controller", this.hashCode());
      return true;
    }
    FLog.v(TAG, "onTouchEvent: view %x, handled by the super", this.hashCode());
    return super.onTouchEvent(event);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    FLog.v(TAG, "onLayout: view %x", this.hashCode());
    super.onLayout(changed, left, top, right, bottom);
    updateZoomableControllerBounds();
  }

  private void onFinalImageSet() {
    FLog.v(TAG, "onFinalImageSet: view %x", this.hashCode());
    if (!mZoomableController.isEnabled()) {
      updateZoomableControllerBounds();
      mZoomableController.setEnabled(true);
    }
  }

  private void onRelease() {
    FLog.v(TAG, "onRelease: view %x", this.hashCode());
    mZoomableController.setEnabled(false);
  }

  @Override
  public void onTransformChanged(Matrix transform) {
    FLog.v(TAG, "onTransformChanged: view %x", this.hashCode());
    maybeSetHugeImageController();
    invalidate();
  }

  private void updateZoomableControllerBounds() {
    getHierarchy().getActualImageBounds(mImageBounds);
    mViewBounds.set(0, 0, getWidth(), getHeight());
    mZoomableController.setImageBounds(mImageBounds);
    mZoomableController.setViewBounds(mViewBounds);
    FLog.v(
        TAG,
        "updateZoomableControllerBounds: view %x, view bounds: %s, image bounds: %s",
        this.hashCode(),
        mViewBounds,
        mImageBounds);
  }
}
