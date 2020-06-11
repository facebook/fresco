// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.fresco.ui.common;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class ForwardingControllerListener2<I> extends BaseControllerListener2<I> {

  private static final String TAG = "FwdControllerListener2";

  private final List<ControllerListener2<I>> mListeners = new ArrayList<>(2);

  public synchronized void addListener(ControllerListener2<I> listener) {
    mListeners.add(listener);
  }

  public synchronized void removeListener(ControllerListener2<I> listener) {
    int index = mListeners.indexOf(listener);
    if (index != -1) {
      mListeners.set(index, null);
    }
  }

  public synchronized void removeAllListeners() {
    mListeners.clear();
  }

  private synchronized void onException(String message, Throwable t) {
    Log.e(TAG, message, t);
  }

  @Override
  public void onSubmit(String id, Object callerContext, Extras extras) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener2<I> listener = mListeners.get(i);
        if (listener != null) {
          listener.onSubmit(id, callerContext, extras);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onSubmit", exception);
      }
    }
  }

  @Override
  public void onFinalImageSet(String id, @Nullable I imageInfo, Extras extraData) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener2<I> listener = mListeners.get(i);
        if (listener != null) {
          listener.onFinalImageSet(id, imageInfo, extraData);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onFinalImageSet", exception);
      }
    }
  }

  @Override
  public void onFailure(String id, Throwable throwable, Extras extras) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener2<I> listener = mListeners.get(i);
        if (listener != null) {
          listener.onFailure(id, throwable, extras);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onFailure", exception);
      }
    }
  }

  @Override
  public void onRelease(String id, Extras extras) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener2<I> listener = mListeners.get(i);
        if (listener != null) {
          listener.onRelease(id, extras);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("ForwardingControllerListener2 exception in onRelease", exception);
      }
    }
  }
}
