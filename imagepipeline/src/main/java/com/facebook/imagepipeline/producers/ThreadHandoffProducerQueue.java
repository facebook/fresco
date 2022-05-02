/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

public interface ThreadHandoffProducerQueue {

  void addToQueueOrExecute(Runnable runnable);

  void startQueueing();

  void stopQueuing();

  void remove(Runnable runnable);

  boolean isQueueing();
}
