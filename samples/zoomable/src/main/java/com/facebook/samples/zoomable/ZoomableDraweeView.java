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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;

/**
 * DraweeView that has zoomable capabilities.
 * <p>
 * Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
public class ZoomableDraweeView extends DraweeView<GenericDraweeHierarchy> {

  private static final Class<?> TAG = ZoomableDraweeView.class;

  private static final float HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f;

  private final RectF mImageBounds = new RectF();
  private final RectF mViewBounds = new RectF();

  private DraweeController mHugeImageController;
  private ZoomableController mZoomableController;
  private GestureDetector mTapGestureDetector;

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

  private final ZoomableController.Listener mZoomableListener = new ZoomableController.Listener() {
    @Override
    public void onTransformChanged(Matrix transform) {
      ZoomableDraweeView.this.onTransformChanged(transform);
    }
  };

  private final GestureListenerWrapper mTapListenerWrapper = new GestureListenerWrapper();

  public ZoomableDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
    super(context);
    setHierarchy(hierarchy);
    init();
  }

  public ZoomableDraweeView(Context context) {
    super(context);
    inflateHierarchy(context, null);
    init();
  }

  public ZoomableDraweeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    inflateHierarchy(context, attrs);
    init();
  }

  public ZoomableDraweeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    inflateHierarchy(context, attrs);
    init();
  }

  protected void inflateHierarchy(Context context, @Nullable AttributeSet attrs) {
    Resources resources = context.getResources();
    GenericDraweeHierarchyBuilder builder = new GenericDraweeHierarchyBuilder(resources)
        .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER);
    GenericDraweeHierarchyInflater.updateBuilder(builder, context, attrs);
    setAspectRatio(builder.getDesiredAspectRatio());
    setHierarchy(builder.build());
  }

  private void init() {
    mZoomableController = AnimatedZoomableController.newInstance();
    mZoomableController.setListener(mZoomableListener);
    mTapGestureDetector = new GestureDetector(getContext(), mTapListenerWrapper);
  }

  /**
   * Gets the original image bounds, in view-absolute coordinates.
   *
   * <p> The original image bounds are those reported by the hierarchy. The hierarchy itself may
   * apply scaling on its own (e.g. due to scale type) so the reported bounds are not necessarily
   * the same as the actual bitmap dimensions. In other words, the original image bounds correspond
   * to the image bounds within this view when no zoomable transformation is applied, but including
   * the potential scaling of the hierarchy.
   * Having the actual bitmap dimensions abstracted away from this view greatly simplifies
   * implementation because the actual bitmap may change (e.g. when a high-res image arrives and
   * replaces the previously set low-res image). With proper hierarchy scaling (e.g. FIT_CENTER),
   * this underlying change will not affect this view nor the zoomable transformation in any way.
   */
  protected void getImageBounds(RectF outBounds) {
    getHierarchy().getActualImageBounds(outBounds);
  }

  /**
   * Gets the bounds used to limit the translation, in view-absolute coordinates.
   *
   * <p> These bounds are passed to the zoomable controller in order to limit the translation. The
   * image is attempted to be centered within the limit bounds if the transformed image is smaller.
   * There will be no empty spaces within the limit bounds if the transformed image is bigger.
   * This applies to each dimension (horizontal and vertical) independently.
   * <p> Unless overridden by a subclass, these bounds are same as the view bounds.
   */
  protected void getLimitBounds(RectF outBounds) {
    outBounds.set(0, 0, getWidth(), getHeight());
  }

  /**
   * Sets a custom zoomable controller, instead of using the default one.
   */
  public void setZoomableController(ZoomableController zoomableController) {
    Preconditions.checkNotNull(zoomableController);
    mZoomableController.setListener(null);
    mZoomableController = zoomableController;
    mZoomableController.setListener(mZoomableListener);
  }

  /**
   * Gets the zoomable controller.
   *
   * <p> Zoomable controller can be used to zoom to point, or to map point from view to image
   * coordinates for instance.
   */
  public ZoomableController getZoomableController() {
    return mZoomableController;
  }

  /**
   * Sets the tap listener.
   */
  public void setTapListener(GestureDetector.SimpleOnGestureListener tapListener) {
    mTapListenerWrapper.setListener(tapListener);
  }

  /**
   * Sets whether long-press tap detection is enabled.
   * Unfortunately, long-press conflicts with onDoubleTapEvent.
   */
  public void setIsLongpressEnabled(boolean enabled) {
    mTapGestureDetector.setIsLongpressEnabled(enabled);
  }

  /**
   * Sets the image controller.
   */
  @Override
  public void setController(@Nullable DraweeController controller) {
    setControllers(controller, null);
  }

  /**
   * Sets the controllers for the normal and huge image.
   *
   * <p> The huge image controller is used after the image gets scaled above a certain threshold.
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

  private void setControllersInternal(
      @Nullable DraweeController controller,
      @Nullable DraweeController hugeImageController) {
    removeControllerListener(getController());
    addControllerListener(controller);
    mHugeImageController = hugeImageController;
    super.setController(controller);
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
    int a = event.getActionMasked();
    FLog.v(TAG, "onTouchEvent: %d, view %x, received", a, this.hashCode());
    if (mTapGestureDetector.onTouchEvent(event)) {
      FLog.v(TAG, "onTouchEvent: %d, view %x, handled by tap gesture detector", a, this.hashCode());
      return true;
    }
    if (mZoomableController.onTouchEvent(event)) {
      if (!mZoomableController.isIdentity()) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      FLog.v(TAG, "onTouchEvent: %d, view %x, handled by zoomable controller", a, this.hashCode());
      return true;
    }
    if (super.onTouchEvent(event)) {
      FLog.v(TAG, "onTouchEvent: %d, view %x, handled by the super", a, this.hashCode());
      return true;
    }
    return false;
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

  protected void onTransformChanged(Matrix transform) {
    FLog.v(TAG, "onTransformChanged: view %x, transform: %s", this.hashCode(), transform);
    maybeSetHugeImageController();
    invalidate();
  }

  protected void updateZoomableControllerBounds() {
    getImageBounds(mImageBounds);
    getLimitBounds(mViewBounds);
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
