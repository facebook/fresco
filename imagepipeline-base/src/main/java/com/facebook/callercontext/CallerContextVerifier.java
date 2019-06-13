/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.callercontext;

import javax.annotation.Nullable;

/** Verifies CallerContext */
public interface CallerContextVerifier {

  void verifyCallerContext(@Nullable Object callerContext);
}
