/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.datasource

import com.facebook.common.internal.Preconditions

/** Settable [DataSource]. */
class SimpleDataSource<T> private constructor() : AbstractDataSource<T>() {
  /**
   * Sets the result to `value`.
   *
   * This method will return `true` if the value was successfully set, or `false` if the data source
   * has already been set, failed or closed.
   *
   * If the value was successfully set and `isLast` is `true`, state of the data source will be set
   * to [AbstractDataSource.DataSourceStatus.SUCCESS].
   *
   * This will also notify the subscribers if the value was successfully set.
   *
   * @param value the value to be set
   * @param isLast whether or not the value is last.
   * @return true if the value was successfully set.
   */
  public override fun setResult(value: T?, isLast: Boolean, extras: Map<String, Any>?): Boolean {
    return super.setResult(Preconditions.checkNotNull<T>(value), isLast, extras)
  }

  /**
   * Sets the value as the last result.
   *
   * See [.setResult].
   */
  fun setResult(value: T): Boolean {
    return super.setResult(Preconditions.checkNotNull<T>(value), /* isLast */ true, null)
  }

  /**
   * Sets the failure.
   *
   * This method will return `true` if the failure was successfully set, or `false` if the data
   * source has already been set, failed or closed.
   *
   * If the failure was successfully set, state of the data source will be set to
   * [ ][AbstractDataSource.DataSourceStatus.FAILURE].
   *
   * This will also notify the subscribers if the failure was successfully set.
   *
   * @param throwable the failure cause to be set.
   * @return true if the failure was successfully set.
   */
  public override fun setFailure(throwable: Throwable): Boolean {
    return super.setFailure(Preconditions.checkNotNull(throwable))
  }

  /**
   * Sets the progress.
   *
   * @param progress the progress in range [0, 1] to be set.
   * @return true if the progress was successfully set.
   */
  public override fun setProgress(progress: Float): Boolean {
    return super.setProgress(progress)
  }

  companion object {
    /** Creates a new [SimpleDataSource]. */
    @JvmStatic
    fun <T> create(): SimpleDataSource<T> {
      return SimpleDataSource()
    }
  }
}
