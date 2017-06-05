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

package com.meetyou.frescopainter.zoom.zoomable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

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
 * 经过改造的ZoomView，请不要随意调整代码
 * by linhonghong
 */
public class ZoomableDraweeView extends DraweeView<GenericDraweeHierarchy>
    implements ZoomableController.Listener{

//  private static final Class<?> TAG = ZoomableDraweeView.class;

  private static final float HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f;

  private final RectF mImageBounds = new RectF();
  private final RectF mViewBounds = new RectF();

  private final ControllerListener mControllerListener = new BaseControllerListener<Object>() {
    @Override
    public void onFinalImageSet(
        String id,
        Object imageInfo,
        Animatable animatable) {
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
  public void setController( DraweeController controller) {
    setControllers(controller, null);
  }

  private void setControllersInternal(
      DraweeController controller,
      DraweeController hugeImageController) {
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
      DraweeController controller,
      DraweeController hugeImageController) {
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

  public OnClickListener mOnClickListener;

  public void setOnDraweeClickListener(OnClickListener l) {
    mOnClickListener = l;
  }

  public void setOnLongDraweeClickListener(OnLongClickListener l){
    mGestureListenerWrapper.setOnLongClickListener(l);
  }

  public long mCurrDownTime = 0;

  //标志当前状态
  GestureListenerWrapper mGestureListenerWrapper = new GestureListenerWrapper(this);
  GestureDetector mGestureDetector = new GestureDetector(mGestureListenerWrapper);

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    mGestureDetector.onTouchEvent(event);
    if(event.getAction() == MotionEvent.ACTION_DOWN){
      mCurrDownTime = event.getEventTime();
    }

//    if(event.getAction() == MotionEvent.ACTION_MOVE){
//      //如果在移动
//      if(Math.abs(event.getRawX() - mX) >= 8 || Math.abs(event.getRawY() - mY) >= 8){
//        removeLong();
//      }
//    }

    if(event.getAction() == MotionEvent.ACTION_UP){
      if(event.getEventTime() - mCurrDownTime <= ViewConfiguration.getTapTimeout()){
        //点击
        if(mOnClickListener != null) {
          mOnClickListener.onClick(this);
        }
      }
    }

    if (mZoomableController.onTouchEvent(event)) {
      if (mZoomableController.getScaleFactor() > 1.0f) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
      return true;
    }
    return super.onTouchEvent(event);
  }

  public void clearZoom(){
    if(mZoomableController != null && mZoomableController instanceof DefaultZoomableController){
      PointF imagePoint = new PointF(getWidth() / 2, getHeight() / 2);
      ((DefaultZoomableController)mZoomableController).zoomToImagePoint(1, imagePoint);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    updateZoomableControllerBounds();
  }

  private void onFinalImageSet() {
    if (!mZoomableController.isEnabled()) {
      updateZoomableControllerBounds();
      mZoomableController.setEnabled(true);
    }
  }

  private void onRelease() {
    mZoomableController.setEnabled(false);
  }

  @Override
  public void onTransformChanged(Matrix transform) {
    maybeSetHugeImageController();
    invalidate();
  }

  private void updateZoomableControllerBounds() {
    getHierarchy().getActualImageBounds(mImageBounds);
    mViewBounds.set(0, 0, getWidth(), getHeight());
    mZoomableController.setImageBounds(mImageBounds);
    mZoomableController.setViewBounds(mViewBounds);
  }
}