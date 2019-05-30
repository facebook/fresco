/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.common.logging.FLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ForwardingImageOriginListener implements ImageOriginListener {

  private static final String TAG = "ForwardingImageOriginListener";

  private final List<ImageOriginListener> mImageOriginListeners;

  public ForwardingImageOriginListener(Set<ImageOriginListener> imageOriginListeners) {
    mImageOriginListeners = new ArrayList<>(imageOriginListeners);
  }

  public ForwardingImageOriginListener(ImageOriginListener... imageOriginListeners) {
    mImageOriginListeners = new ArrayList<>(imageOriginListeners.length);
    Collections.addAll(mImageOriginListeners, imageOriginListeners);
  }

  public synchronized void addImageOriginListener(ImageOriginListener listener) {
    mImageOriginListeners.add(listener);
  }

  public synchronized void removeImageOriginListener(ImageOriginListener listener) {
    mImageOriginListeners.remove(listener);
  }

  @Override
  public synchronized void onImageLoaded(
      String controllerId, int imageOrigin, boolean successful, String ultimateProducerName) {
    final int numberOfListeners = mImageOriginListeners.size();
    for (int i = 0; i < numberOfListeners; i++) {
      ImageOriginListener listener = mImageOriginListeners.get(i);
      if (listener != null) {
        try {
          listener.onImageLoaded(controllerId, imageOrigin, successful, ultimateProducerName);
        } catch (Exception e) {
          FLog.e(TAG, "InternalListener exception in onImageLoaded", e);
        }
      }
    }
  }
}
