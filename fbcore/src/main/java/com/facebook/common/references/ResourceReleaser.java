/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import com.facebook.infer.annotation.Nullsafe;

/**
 * Interface that abstracts the action of releasing a resource.
 *
 * <p>There are multiple components that own resources that are shared by others, like pools and
 * caches. This interface should be implemented by classes that want to perform some action when a
 * particular resource is no longer needed.
 *
 * @param <T> type of resource managed by this ResourceReleaser
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface ResourceReleaser<T> {

  /**
   * Release the given value.
   *
   * <p>After calling this method, the caller is no longer responsible for managing lifetime of the
   * value.
   *
   * <p>This method is not permitted to throw an exception and is always required to succeed. It is
   * often called from contexts like catch blocks or finally blocks to cleanup resources. Throwing
   * an exception could result in swallowing the original exception.
   *
   * @param value
   */
  void release(T value);
}
