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

package com.facebook.samples.comparison.instrumentation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;

/**
 * Tracks state of image request.
 *
 * <p/> Components that implement {@link Instrumented} interface can use this class to track their
 * requests.
 */
public class Instrumentation {
  private static final String TAG = "Instrumentation";

  private static enum ImageRequestState {
    NOT_STARTED,
    STARTED,
    SUCCESS,
    FAILURE,
    CANCELLATION,
  }

  private final Paint mPaint;
  private final Rect mTextRect;
  private final View mView;

  private PerfListener mPerfListener;
  private long mStartTime;
  private String mTag;
  private long mFinishTime;
  private ImageRequestState mState;

  public Instrumentation(View view) {
    mPaint = new Paint();
    mTextRect = new Rect();
    mView = view;
    mState = ImageRequestState.NOT_STARTED;
  }

  public void init(final String tag, final PerfListener perfListener) {
    mTag = Preconditions.checkNotNull(tag);
    mPerfListener = Preconditions.checkNotNull(perfListener);
  }

  public void onStart() {
    Preconditions.checkNotNull(mTag);
    Preconditions.checkNotNull(mPerfListener);
    if (mState == ImageRequestState.STARTED) {
      onCancellation();
    }
    mStartTime = System.currentTimeMillis();
    mFinishTime = 0;
    mPerfListener.reportStart();
    mState = ImageRequestState.STARTED;
    FLog.i(TAG, "Image [%s]: loading started...", mTag);
  }

  public void onSuccess() {
    Preconditions.checkState(mState == ImageRequestState.STARTED);
    mState = ImageRequestState.SUCCESS;
    mFinishTime = System.currentTimeMillis();
    final long elapsedTime = mFinishTime - mStartTime;
    mPerfListener.reportSuccess(elapsedTime);
    FLog.i(TAG, "Image [%s]: loaded after %d ms", mTag, elapsedTime);
  }

  public void onFailure() {
    Preconditions.checkState(mState == ImageRequestState.STARTED);
    mState = ImageRequestState.FAILURE;
    mFinishTime = System.currentTimeMillis();
    final long elapsedTime = mFinishTime - mStartTime;
    mPerfListener.reportFailure(elapsedTime);
    FLog.i(TAG, "Image [%s]: failed after %d ms", mTag, elapsedTime);
  }

  public void onCancellation() {
    if (mState != ImageRequestState.STARTED) {
      return;
    }
    mState = ImageRequestState.CANCELLATION;
    mFinishTime = System.currentTimeMillis();
    final long elapsedTime = mFinishTime - mStartTime;
    mPerfListener.reportCancellation(elapsedTime);
    FLog.i(TAG, "Image [%s]: cancelled after %d ms", mTag, elapsedTime);
  }

  /** Draws overlay with request state for easier visual inspection. */
  public void onDraw(final Canvas canvas) {
    mPaint.setColor(0xC0000000);
    mTextRect.set(0, 0, mView.getWidth(), 35);
    canvas.drawRect(mTextRect, mPaint);

    mPaint.setColor(Color.WHITE);
    canvas.drawText("[" + mTag + "]", 10, 15, mPaint);

    String message = "Not started";
    switch (mState) {
      case STARTED:
        message = "Loading...";
        break;
      case SUCCESS:
        message = "Loaded after " + (mFinishTime - mStartTime) + "ms";
        break;
      case FAILURE:
        message = "Failed after " + (mFinishTime - mStartTime) + "ms";
        break;
      case CANCELLATION:
        message = "Cancelled after " + (mFinishTime - mStartTime) + "ms";
        break;
    }
    canvas.drawText(message, 10, 30, mPaint);
  }
}
