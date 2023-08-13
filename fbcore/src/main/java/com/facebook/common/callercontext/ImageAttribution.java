// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.common.callercontext;

import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface ImageAttribution {

  String getCallingClassName();

  @Nullable
  ContextChain getContextChain();
}
