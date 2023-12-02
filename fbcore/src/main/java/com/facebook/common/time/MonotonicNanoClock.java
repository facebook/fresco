/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.time;

import javax.annotation.concurrent.ThreadSafe;

/** A clock that is guaranteed not to go backward. */
@ThreadSafe
public interface MonotonicNanoClock extends MonotonicClock {}
