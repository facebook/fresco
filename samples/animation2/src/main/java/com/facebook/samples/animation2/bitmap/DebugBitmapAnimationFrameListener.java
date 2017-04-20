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

import com.facebook.common.logging.FLog;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;

/**
 * {@link com.facebook.fresco.animation.bitmap.BitmapAnimationBackend.FrameListener}
 * that logs animation statistics.
 */
public class DebugBitmapAnimationFrameListener implements BitmapAnimationBackend.FrameListener {

  private static final Class<?> TAG = DebugBitmapAnimationFrameListener.class;

  private int mCachedCount;
  private int mReusedCount;
  private int mCreatedCount;
  private int mFallbackCount;
  private int mUnknownCount;

  private int mDroppedFrameCount;

  private long mLastFrameStart;

  @Override
  public void onDrawFrameStart(BitmapAnimationBackend backend, int frameNumber) {
    mLastFrameStart = System.currentTimeMillis();
    FLog.d(TAG, "Frame: event=start, number=%d", frameNumber);
  }

  @Override
  public void onFrameDrawn(
      BitmapAnimationBackend backend,
      int frameNumber,
      @BitmapAnimationBackend.FrameType int frameType) {
    increaseFrameTypeCount(frameType);
    FLog.d(
        TAG,
        "Frame: event=drawn, number=%d, type=%s, render_time=%d ms",
        frameNumber,
        getFrameTypeName(frameType),
        System.currentTimeMillis() - mLastFrameStart);
    logStatistics();
  }

  @Override
  public void onFrameDropped(BitmapAnimationBackend backend, int frameNumber) {
    mDroppedFrameCount++;
    FLog.d(
        TAG,
        "Frame: event=dropped, number=%d, render_time=%d ms",
        frameNumber,
        System.currentTimeMillis() - mLastFrameStart);
    logStatistics();
  }

  private void logStatistics() {
    FLog.d(
        TAG,
        "Stats: cached=%s, reused=%s, created=%s, fallback=%s, dropped=%s, unknown=%s",
        mCachedCount,
        mReusedCount,
        mCreatedCount,
        mFallbackCount,
        mDroppedFrameCount,
        mUnknownCount);
  }

  private void increaseFrameTypeCount(@BitmapAnimationBackend.FrameType int frameType) {
    switch (frameType) {
      case BitmapAnimationBackend.FRAME_TYPE_CACHED:
        mCachedCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_REUSED:
        mReusedCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_CREATED:
        mCreatedCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_FALLBACK:
        mFallbackCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_UNKNOWN:
      default:
        mUnknownCount++;
        break;
    }
  }

  private static String getFrameTypeName(@BitmapAnimationBackend.FrameType int frameType) {
    switch (frameType) {
      case BitmapAnimationBackend.FRAME_TYPE_CACHED:
        return "cached";
      case BitmapAnimationBackend.FRAME_TYPE_REUSED:
        return "reused";
      case BitmapAnimationBackend.FRAME_TYPE_CREATED:
        return "created";
      case BitmapAnimationBackend.FRAME_TYPE_FALLBACK:
        return "fallback";
      case BitmapAnimationBackend.FRAME_TYPE_UNKNOWN:
      default:
        return "unknown";
    }
  }
}
