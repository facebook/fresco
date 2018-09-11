/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

/**
 * Abstraction we use in order to allocate memory for bitmaps in different OS versions. It also
 * contains implementation of the Releaser which is responsible to close the specific
 * CloseableReference when not used anymore.
 */
package com.facebook.imagepipeline.bitmaps;
