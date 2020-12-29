// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.common.internal;

public interface Fn<A, R> {

  R apply(A arg);
}
