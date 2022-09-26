/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource

import com.facebook.common.references.CloseableReference
import com.facebook.datasource.AbstractDataSource
import com.facebook.datasource.DataSource
import javax.annotation.concurrent.ThreadSafe

/**
 * A [DataSource] whose result may be set by a [set(CloseableReference<T>)] or
 * [setException(Throwable)] call. It may also be closed.
 *
 * This data source has no intermediate results - calling [set(CloseableReference<T>)] means that
 * the data source is finished. </T></T>
 */
@ThreadSafe
class SettableDataSource<T> private constructor() : AbstractDataSource<CloseableReference<T>?>() {

  /**
   * Sets the value of this data source.
   *
   * This method will return `true` if the value was successfully set, or `false` if the data source
   * has already been set, failed or closed.
   *
   * Passed CloseableReference is cloned, caller of this method still owns passed reference after
   * the method returns.
   *
   * @param valueRef closeable reference to the value the data source should hold.
   * @return true if the value was successfully set.
   */
  fun set(valueRef: CloseableReference<T>?): Boolean {
    val clonedRef = CloseableReference.cloneOrNull(valueRef)
    return super.setResult(clonedRef, /* isLast */ true, null)
  }

  /**
   * Sets the data source to having failed with the given exception.
   *
   * This method will return `true` if the exception was successfully set, or `false` if the data
   * source has already been set, failed or closed.
   *
   * @param throwable the exception the data source should hold.
   * @return true if the exception was successfully set.
   */
  fun setException(throwable: Throwable): Boolean = super.setFailure(throwable)

  /**
   * Sets the progress.
   *
   * @param progress the progress in range [0, 1] to be set.
   * @return true if the progress was successfully set.
   */
  public override fun setProgress(progress: Float): Boolean = super.setProgress(progress)

  /**
   * Gets the result if any, null otherwise.
   *
   * Value will be cloned and it's the caller's responsibility to close the returned value.
   */
  override fun getResult(): CloseableReference<T>? =
      CloseableReference.cloneOrNull(super.getResult())

  override fun closeResult(result: CloseableReference<T>?) {
    CloseableReference.closeSafely(result)
  }

  companion object {
    /** Creates a new `SettableDataSource` */
    @JvmStatic fun <V> create(): SettableDataSource<V> = SettableDataSource()
  }
}
