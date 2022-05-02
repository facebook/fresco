/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.controller;

import android.graphics.drawable.Animatable;
import android.util.Log;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.ui.common.OnDrawControllerListener;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/** Listener that forwards controller events to multiple listeners. */
@ThreadSafe
public class ForwardingControllerListener<INFO>
    implements ControllerListener<INFO>, OnDrawControllerListener<INFO> {
  // lint only allows 23 characters in a tag
  private static final String TAG = "FdingControllerListener";

  private final List<ControllerListener<? super INFO>> mListeners = new ArrayList<>(2);

  public ForwardingControllerListener() {}

  public static <INFO> ForwardingControllerListener<INFO> create() {
    return new ForwardingControllerListener<INFO>();
  }

  public static <INFO> ForwardingControllerListener<INFO> of(
      ControllerListener<? super INFO> listener) {
    ForwardingControllerListener<INFO> forwarder = create();
    forwarder.addListener(listener);
    return forwarder;
  }

  public static <INFO> ForwardingControllerListener<INFO> of(
      ControllerListener<? super INFO> listener1, ControllerListener<? super INFO> listener2) {
    ForwardingControllerListener<INFO> forwarder = create();
    forwarder.addListener(listener1);
    forwarder.addListener(listener2);
    return forwarder;
  }

  public synchronized void addListener(ControllerListener<? super INFO> listener) {
    mListeners.add(listener);
  }

  public synchronized void removeListener(ControllerListener<? super INFO> listener) {
    int index = mListeners.indexOf(listener);
    if (index != -1) {
      mListeners.set(index, null);
    }
  }

  public synchronized void clearListeners() {
    mListeners.clear();
  }

  private synchronized void onException(String message, Throwable t) {
    Log.e(TAG, message, t);
  }

  @Override
  public synchronized void onSubmit(String id, Object callerContext) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener != null) {
          listener.onSubmit(id, callerContext);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onSubmit", exception);
      }
    }
  }

  @Override
  public synchronized void onFinalImageSet(
      String id, @Nullable INFO imageInfo, @Nullable Animatable animatable) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener != null) {
          listener.onFinalImageSet(id, imageInfo, animatable);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onFinalImageSet", exception);
      }
    }
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable INFO imageInfo) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener != null) {
          listener.onIntermediateImageSet(id, imageInfo);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onIntermediateImageSet", exception);
      }
    }
  }

  @Override
  public void onIntermediateImageFailed(String id, Throwable throwable) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener != null) {
          listener.onIntermediateImageFailed(id, throwable);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onIntermediateImageFailed", exception);
      }
    }
  }

  @Override
  public synchronized void onFailure(String id, Throwable throwable) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener != null) {
          listener.onFailure(id, throwable);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onFailure", exception);
      }
    }
  }

  @Override
  public synchronized void onRelease(String id) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener != null) {
          listener.onRelease(id);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onRelease", exception);
      }
    }
  }

  @Override
  public void onImageDrawn(String id, INFO imageInfo, DimensionsInfo dimensionsInfo) {
    final int numberOfListeners = mListeners.size();
    for (int i = 0; i < numberOfListeners; ++i) {
      try {
        ControllerListener<? super INFO> listener = mListeners.get(i);
        if (listener instanceof OnDrawControllerListener) {
          ((OnDrawControllerListener) listener).onImageDrawn(id, imageInfo, dimensionsInfo);
        }
      } catch (Exception exception) {
        // Don't punish the other listeners if we're given a bad one.
        onException("InternalListener exception in onImageDrawn", exception);
      }
    }
  }
}
