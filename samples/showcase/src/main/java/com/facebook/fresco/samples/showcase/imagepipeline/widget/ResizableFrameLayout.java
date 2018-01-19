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
package com.facebook.fresco.samples.showcase.imagepipeline.widget;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import javax.annotation.Nullable;

public class ResizableFrameLayout extends FrameLayout {

  public interface SizeChangedListener {

    void onSizeChanged(int widthPx, int heightPx);
  }

  private View mCornerIndicator;
  private boolean mResizing;
  private float mLastX;
  private float mLastY;
  private int mMaximumWidth;
  private int mMaximumHeight;
  private boolean mUpdateMaximumDimensionOnNextSizeChange;

  private @Nullable SizeChangedListener mSizeChangedListener;

  public ResizableFrameLayout(Context context) {
    super(context);
  }

  public ResizableFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ResizableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void init(View cornerIndicator) {
    mCornerIndicator = cornerIndicator;

    mMaximumWidth = getWidth();
    mMaximumHeight = getHeight();
  }

  public void setUpdateMaximumDimensionOnNextSizeChange(
      boolean updateMaximumDimensionOnNextSizeChange) {
    mUpdateMaximumDimensionOnNextSizeChange = updateMaximumDimensionOnNextSizeChange;
  }

  public void setSizeChangedListener(@Nullable SizeChangedListener sizeChangedListener) {
    mSizeChangedListener = sizeChangedListener;
  }

  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
  @Override
  public boolean onInterceptTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN &&
        event.getX() >= mCornerIndicator.getX() &&
        event.getY() >= mCornerIndicator.getY()) {
      ViewGroup.LayoutParams layoutParams = getLayoutParams();
      layoutParams.width = getWidth();
      layoutParams.height = getHeight();
      setLayoutParams(layoutParams);
      mResizing = true;
      return true;
    }

    return false;
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!mResizing) {
      return false;
    }

    switch (event.getAction()) {
      case MotionEvent.ACTION_MOVE:
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        layoutParams.width += event.getX() - mLastX;
        layoutParams.width =
            Math.max(Math.min(layoutParams.width, mMaximumWidth), getMinimumWidth());

        layoutParams.height += event.getY() - mLastY;
        layoutParams.height =
            Math.max(Math.min(layoutParams.height, mMaximumHeight), getMinimumHeight());
        setLayoutParams(layoutParams);
    }

    mLastX = Math.max(
        Math.min(event.getX(), mMaximumWidth),
        getMinimumWidth() - mCornerIndicator.getWidth());
    mLastY = Math.max(
        Math.min(event.getY(), mMaximumHeight),
        getMinimumHeight() - mCornerIndicator.getHeight());
    return true;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (mUpdateMaximumDimensionOnNextSizeChange) {
      mMaximumWidth = w;
      mMaximumHeight = h;
      mUpdateMaximumDimensionOnNextSizeChange = false;
    }
    if (mSizeChangedListener != null) {
      mSizeChangedListener.onSizeChanged(w, h);
    }
  }
}
