/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomable;

import android.graphics.Matrix;
import com.facebook.infer.annotation.Nullsafe;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ZoomableController.Listener} that allows multiple child listeners to
 * be added and notified about {@link ZoomableController} events.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public class MultiZoomableControllerListener implements ZoomableController.Listener {

  private final List<ZoomableController.Listener> mListeners = new ArrayList<>();

  @Override
  public synchronized void onTransformBegin(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformBegin(transform);
    }
  }

  @Override
  public synchronized void onTransformChanged(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformChanged(transform);
    }
  }

  @Override
  public synchronized void onTransformEnd(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformEnd(transform);
    }
  }

  public synchronized void addListener(ZoomableController.Listener listener) {
    mListeners.add(listener);
  }

  public synchronized void removeListener(ZoomableController.Listener listener) {
    mListeners.remove(listener);
  }
}
