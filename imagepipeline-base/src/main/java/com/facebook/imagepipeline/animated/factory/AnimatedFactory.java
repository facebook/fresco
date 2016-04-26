/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.animated.factory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.ScheduledExecutorService;

import android.content.Context;
import android.content.res.Resources;
import com.facebook.common.time.MonotonicClock;
import com.facebook.common.executors.SerialExecutorService;
import com.facebook.imagepipeline.animated.factory.AnimatedDrawableFactory;
import com.facebook.imagepipeline.animated.factory.AnimatedImageFactory;

@NotThreadSafe
public interface AnimatedFactory {

  AnimatedDrawableFactory getAnimatedDrawableFactory(Context context);

  AnimatedImageFactory getAnimatedImageFactory();

}
