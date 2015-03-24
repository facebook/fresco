/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

/**
 * Callbacks that are called when something changes in a request sequence.
 */
public interface ProducerContextCallbacks {

  /**
   * Method that is called when a client cancels the request.
   */
  public void onCancellationRequested();

  /**
   * Method that is called when a request is no longer a prefetch, or vice versa.
   */
  public void onIsPrefetchChanged();

  /**
   * Method that is called when intermediate results start or stop being expected.
   */
  public void onIsIntermediateResultExpectedChanged();

  /**
   * Method that is called when the priority of the request changes.
   */
  public void onPriorityChanged();
}
