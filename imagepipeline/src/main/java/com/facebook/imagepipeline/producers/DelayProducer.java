/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;

/**
 * A ScheduledExecutorService is a significant dependency and we do not want to require it. If not
 * provided, this producer is a no-op.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DelayProducer implements Producer<CloseableReference<CloseableImage>> {

  private final Producer<CloseableReference<CloseableImage>> mInputProducer;
  private final @Nullable ScheduledExecutorService mBackgroundTasksExecutor;

  public DelayProducer(
      final Producer<CloseableReference<CloseableImage>> inputProducer,
      final @Nullable ScheduledExecutorService backgroundTasksExecutor) {
    mInputProducer = inputProducer;
    mBackgroundTasksExecutor = backgroundTasksExecutor;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer, final ProducerContext context) {
    ImageRequest request = context.getImageRequest();
    if (mBackgroundTasksExecutor != null) {
      mBackgroundTasksExecutor.schedule(
          new Runnable() {
            @Override
            public void run() {
              mInputProducer.produceResults(consumer, context);
            }
          },
          request.getDelayMs(),
          MILLISECONDS);
    } else {
      mInputProducer.produceResults(consumer, context);
    }
  }
}
