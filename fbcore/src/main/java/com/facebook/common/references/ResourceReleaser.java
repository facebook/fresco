/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.common.references;

/**
 * Interface that abstracts the action of releasing a resource.
 *
 * <p>There are multiple components that own resources that are shared by others, like pools and
 * caches. This interface should be implemented by classes that want to perform some action
 * when a particular resource is no longer needed.
 * @param <T> type of resource managed by this ResourceReleaser
 */
public interface ResourceReleaser<T> {

  /**
   * <p>Release the given value.
   *
   * <p>After calling this method, the caller is no longer responsible for
   * managing lifetime of the value.
   * <p>This method is not permitted to throw an exception and is always required to succeed.
   * It is often called from contexts like catch blocks or finally blocks to cleanup resources.
   * Throwing an exception could result in swallowing the original exception.</p>
   * @param value
   */
  void release(T value);
}
