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
package com.facebook.samples.animation2.bitmap;

import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2DebugDrawListener;
import com.facebook.samples.animation2.R;
import com.facebook.samples.animation2.SampleData;
import com.facebook.samples.animation2.utils.AnimationBackendUtils;
import com.facebook.samples.animation2.utils.AnimationControlsManager;

/**
 * Fragment that creates a new {@link AnimatedDrawable2} and a custom bitmap animation backend. In
 * addition, it displays useful debug information, like cached frames.
 */
public class BitmapAnimationDebugFragment extends Fragment {

  private LinearLayout mFrameInformationContainer;

  private final SparseArray<FrameInformationHolder> mFrameInfoMap = new SparseArray<>();

  private int mActiveFrameNumber = -1;

  private BitmapAnimationBackend mBitmapAnimationBackend;
  private AnimatedDrawable2 mAnimatedDrawable;
  private AnimationControlsManager mAnimationControlsManager;

  private final BitmapAnimationBackend.FrameListener mFrameListener =
      new BitmapAnimationBackend.FrameListener() {
        @Override
        public void onDrawFrameStart(BitmapAnimationBackend backend, int frameNumber) {

        }

        @Override
        public void onFrameDrawn(
            BitmapAnimationBackend backend,
            int frameNumber,
            @BitmapAnimationBackend.FrameType int frameType) {
          FrameInformationHolder previousFrame = mFrameInfoMap.get(mActiveFrameNumber);
          if (previousFrame != null) {
            previousFrame.setFrameType(false, frameType);
          }
          mActiveFrameNumber = frameNumber;
          FrameInformationHolder activeFrame = mFrameInfoMap.get(frameNumber);
          if (activeFrame != null) {
            activeFrame.setFrameType(true, frameType);
          }
        }

        @Override
        public void onFrameDropped(BitmapAnimationBackend backend, int frameNumber) {
          // The frame could not be drawn for some reason.
          // We don't care about this since caching is independent of rendering.
        }
      };

  private final BitmapFrameCache.FrameCacheListener mFrameCacheListener =
      new BitmapFrameCache.FrameCacheListener() {
        @Override
        public void onFrameCached(
            BitmapFrameCache bitmapFrameCache,
            int frameNumber) {
          FrameInformationHolder frameInfo = mFrameInfoMap.get(frameNumber);
          if (frameInfo != null) {
            frameInfo.setCached(true);
          }
        }

        @Override
        public void onFrameEvicted(
            BitmapFrameCache bitmapFrameCache,
            int frameNumber) {
          FrameInformationHolder frameInfo = mFrameInfoMap.get(frameNumber);
          if (frameInfo != null) {
            frameInfo.setCached(false);
          }
        }
      };

  private final BitmapAnimationCacheSelectorConfigurator.BitmapFrameCacheChangedListener
      mBitmapFrameCacheChangedListener =
      new BitmapAnimationCacheSelectorConfigurator.BitmapFrameCacheChangedListener() {
        @Override
        public void onBitmapFrameCacheChanged(BitmapFrameCache bitmapFrameCache) {
          updateBitmapFrameCache(bitmapFrameCache);
        }
      };

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_debug_bitmap, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    // Get the animation container
    final ImageView imageView = (ImageView) view.findViewById(R.id.animation_container);

    mFrameInformationContainer = (LinearLayout) view.findViewById(R.id.frame_information);

    mAnimatedDrawable = new AnimatedDrawable2();
    mAnimatedDrawable.setDrawListener(new AnimatedDrawable2DebugDrawListener());

    view.findViewById(R.id.invalidate_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        imageView.invalidate();
      }
    });

    mAnimationControlsManager = new AnimationControlsManager(
        mAnimatedDrawable,
        (SeekBar) getView().findViewById(R.id.seekbar),
        (ToggleButton) getView().findViewById(R.id.playpause),
        getView().findViewById(R.id.reset));

    new BitmapAnimationCacheSelectorConfigurator(
        (Spinner) view.findViewById(R.id.spinner),
        mBitmapFrameCacheChangedListener,
        mFrameCacheListener);

    imageView.setImageDrawable(mAnimatedDrawable);
  }

  private void updateBitmapFrameCache(BitmapFrameCache bitmapFrameCache) {
    mActiveFrameNumber = -1;
    mBitmapAnimationBackend = ExampleBitmapAnimationFactory
        .createColorBitmapAnimationBackend(
            SampleData.COLORS,
            300,
            bitmapFrameCache);

    AnimationBackend backendWithInactivityCheck =
        AnimationBackendUtils.wrapAnimationBackendWithInactivityCheck(
            getContext(),
            mBitmapAnimationBackend);
    setupFrameInformationContainer(mBitmapAnimationBackend);
    mAnimationControlsManager.updateBackendData(backendWithInactivityCheck);

    mAnimatedDrawable.setAnimationBackend(backendWithInactivityCheck);
    mAnimatedDrawable.invalidateSelf();
  }

  private void setupFrameInformationContainer(BitmapAnimationBackend bitmapAnimationBackend) {
    mFrameInformationContainer.removeAllViews();
    LayoutInflater layoutInflater = LayoutInflater.from(getContext());
    for (int i = 0; i < bitmapAnimationBackend.getFrameCount(); i++) {
      FrameInformationHolder frameInformation = createFrameInformation(layoutInflater, i);
      mFrameInfoMap.put(i, frameInformation);
      mFrameInformationContainer.addView(frameInformation.getView());
    }
    bitmapAnimationBackend.setFrameListener(mFrameListener);
  }

  private FrameInformationHolder createFrameInformation(
      LayoutInflater inflater,
      int frameNumber) {
    View layout = inflater.inflate(R.layout.frame_info, mFrameInformationContainer, false);
    return new FrameInformationHolder(layout, frameNumber);
  }

  private static class FrameInformationHolder {

    private final View mView;
    private final TextView mFrameNumber;
    private final TextView mFrameType;
    private final TextView mCached;

    private final int mFrameCachedColor;
    private final int mFrameCreatedColor;
    private final int mFrameReusedColor;
    private final int mFrameFallbackColor;
    private final int mFrameUnknownColor;
    private final int mDisabledColor;

    private FrameInformationHolder(View view, int frameNumber) {
      mView = view;
      mFrameNumber = (TextView) view.findViewById(R.id.frame_number);
      mFrameType = (TextView) view.findViewById(R.id.frame_type);
      mCached = (TextView) view.findViewById(R.id.cached);
      setFrameNumber(frameNumber);

      mFrameCachedColor = view.getResources().getColor(R.color.green500);
      mFrameCreatedColor = view.getResources().getColor(R.color.red500);
      mFrameReusedColor = view.getResources().getColor(R.color.orange500);
      mFrameFallbackColor = view.getResources().getColor(R.color.blue500);
      mFrameUnknownColor = view.getResources().getColor(R.color.colorPrimary);
      mDisabledColor = view.getResources().getColor(android.R.color.transparent);
    }

    public void setFrameNumber(int frameNumber) {
      mFrameNumber.setText(String.format("#%2d", frameNumber));
    }

    public void setCached(boolean cached) {
      mCached.setBackgroundColor(cached ? mFrameCachedColor : mDisabledColor);
      mCached.setText(cached ? "cached" : "");
    }

    public void setFrameType(boolean active, @BitmapAnimationBackend.FrameType int frameType) {
      if (!active) {
        mFrameType.setText("");
        mFrameType.setBackgroundColor(mDisabledColor);
        return;
      }
      @ColorInt int frameColor;
      String text;
      switch (frameType) {
        case BitmapAnimationBackend.FRAME_TYPE_CACHED:
          frameColor = mFrameCachedColor;
          text = "cached bitmap";
          break;
        case BitmapAnimationBackend.FRAME_TYPE_CREATED:
          frameColor = mFrameCreatedColor;
          text = "created bitmap";
          break;
        case BitmapAnimationBackend.FRAME_TYPE_REUSED:
          frameColor = mFrameReusedColor;
          text = "reused bitmap";
          break;
        case BitmapAnimationBackend.FRAME_TYPE_FALLBACK:
          frameColor = mFrameFallbackColor;
          text = "fallback frame";
          break;
        case BitmapAnimationBackend.FRAME_TYPE_UNKNOWN:
        default:
          text = "unknown";
          frameColor = mFrameUnknownColor;
      }
      mFrameType.setText(text);
      mFrameType.setBackgroundColor(frameColor);
    }

    public View getView() {
      return mView;
    }
  }
}
