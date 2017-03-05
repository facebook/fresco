/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

/**
 * Abstraction we use in order to allocate memory for bitmaps in different OS versions. It also
 * contains implementation of the Releaser which is responsible to close the specific
 * CloseableReference when not used anymore.
 */
package com.facebook.imagepipeline.bitmaps;
