/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.controller;

import javax.annotation.Nullable;

import android.graphics.drawable.Animatable;

/**
 * Interface for {@link AbstractDraweeController} listener.
 *
 * <p> Controller id is passed to each of the listener methods which is useful for debugging and
 * instrumentation purposes where those events can then be associated with a sequence. Subscriber
 * is free to completely ignore this id, as late callbacks and other such correctness issues are
 * taken care of by the controller itself.
 *
 * @param <INFO> image info type
 */
public interface ControllerListener<INFO> {

  /**
   * Called before the image request is submitted.
   * <p> IMPORTANT: It is not safe to reuse the controller from within this callback!
   * @param id controller id
   * @param callerContext caller context
   */
  public void onSubmit(String id, Object callerContext);

  /**
   * Called after the final image has been set.
   * @param id controller id
   * @param imageInfo image info
   * @param animatable
   */
  public void onFinalImageSet(String id, @Nullable INFO imageInfo, @Nullable Animatable animatable);

  /**
   * Called after any intermediate image has been set.
   * @param id controller id
   * @param imageInfo image info
   */
  public void onIntermediateImageSet(String id, @Nullable INFO imageInfo);

  /**
   * Called after the fetch of the intermediate image failed.
   * @param id controller id
   * @param throwable failure cause
   */
  public void onIntermediateImageFailed(String id, Throwable throwable);

  /**
   * Called after the fetch of the final image failed.
   * @param id controller id
   * @param throwable failure cause
   */
  public void onFailure(String id, Throwable throwable);

  /**
   * Called after the controller released the fetched image.
   * <p> IMPORTANT: It is not safe to reuse the controller from within this callback!
   * @param id controller id
   */
  public void onRelease(String id);
}
