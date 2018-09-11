/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

/**
 * Empty implementation of {@link ProducerContextCallbacks}.
 */
public class BaseProducerContextCallbacks implements ProducerContextCallbacks {

  @Override
  public void onCancellationRequested() {
  }

  @Override
  public void onIsPrefetchChanged() {
  }

  @Override
  public void onIsIntermediateResultExpectedChanged() {
  }

  @Override
  public void onPriorityChanged() {
  }
}
