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

package com.facebook.samples.animation2.local;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.facebook.fresco.animation.backend.AnimationBackend;
import java.util.ArrayList;
import java.util.List;

/**
 * Local drawable animation backend that chains local drawables together.
 */
public class LocalDrawableAnimationBackend implements AnimationBackend {

  private final Resources mResources;
  private final List<Integer> mDrawableResourceIds;
  private final int mLoopCount;
  private final int mFrameDurationMs;

  private final SparseArray<Drawable> mCache = new SparseArray<>();
  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private Rect mBounds;

  private LocalDrawableAnimationBackend(Builder builder) {
    mResources = builder.mResources;
    mDrawableResourceIds = builder.mResourceIds;
    mLoopCount = builder.mLoopCount;
    mFrameDurationMs = builder.mFrameDurationMs;
  }

  @Override
  public int getFrameCount() {
    return mDrawableResourceIds.size();
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mFrameDurationMs;
  }

  @Override
  public int getLoopCount() {
    return mLoopCount;
  }

  @Override
  public boolean drawFrame(Drawable parent, Canvas canvas, int frameNumber) {
    Drawable drawable = getDrawable(mDrawableResourceIds.get(frameNumber));
    drawable.setBounds(mBounds != null ? mBounds : parent.getBounds());
    drawable.draw(canvas);
    return true;
  }

  private Drawable getDrawable(int drawableResId) {
    Drawable drawable = mCache.get(drawableResId);
    if (drawable == null) {
      drawable = mResources.getDrawable(drawableResId);
      mCache.put(drawableResId, drawable);
    }
    return drawable;
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
  }

  @Override
  public void setBounds(Rect bounds) {
    mBounds = bounds;
  }

  @Override
  public int getIntrinsicWidth() {
    return INTRINSIC_DIMENSION_UNSET;
  }

  @Override
  public int getIntrinsicHeight() {
    return INTRINSIC_DIMENSION_UNSET;
  }

  @Override
  public void clear() {
    mCache.clear();
  }

  @Override
  public int getSizeInBytes() {
    return 0;
  }

  public static class Builder {

    private final List<Integer> mResourceIds;
    private final Resources mResources;
    private int mLoopCount = LOOP_COUNT_INFINITE;
    private int mFrameDurationMs = 100;

    public Builder(Resources resources) {
      this(resources, new ArrayList<Integer>());
    }

    public Builder(Resources resources, List<Integer> resourceIds) {
      mResourceIds = resourceIds;
      mResources = resources;
    }

    public Builder addDrawableFrame(int drawableResId) {
      mResourceIds.add(drawableResId);
      return this;
    }

    public Builder loopCount(int loopCount) {
      mLoopCount = loopCount;
      return this;
    }

    public Builder frameDurationMs(int frameDurationMs) {
      mFrameDurationMs = frameDurationMs;
      return this;
    }

    public LocalDrawableAnimationBackend build() {
      return new LocalDrawableAnimationBackend(this);
    }
  }
}
