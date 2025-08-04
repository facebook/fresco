/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common

import com.facebook.imagepipeline.common.BytesRange.Companion.from
import com.facebook.imagepipeline.common.BytesRange.Companion.fromContentRangeHeader
import com.facebook.imagepipeline.common.BytesRange.Companion.toMax
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BytesRangeTest {
  @Test
  fun testHeaderValueForRangeFrom() {
    assertThat(from(2000).toHttpRangeHeaderValue()).isEqualTo("bytes=2000-")
  }

  @Test
  fun testHeaderValueForRangeTo() {
    assertThat(toMax(1000).toHttpRangeHeaderValue()).isEqualTo("bytes=0-1000")
  }

  @Test
  fun testContains() {
    assertThat(toMax(1000).contains(toMax(999))).isTrue()
    assertThat(toMax(1000).contains(toMax(1000))).isTrue()
    assertThat(toMax(1000).contains(toMax(1001))).isFalse()

    assertThat(from(1000).contains(from(999))).isFalse()
    assertThat(from(1000).contains(from(1000))).isTrue()
    assertThat(from(1000).contains(from(1001))).isTrue()

    assertThat(from(1000).contains(toMax(999))).isFalse()

    assertThat(BytesRange(0, BytesRange.TO_END_OF_CONTENT).contains(BytesRange(0, 1000))).isTrue()
    assertThat(BytesRange(0, 1000).contains(BytesRange(0, BytesRange.TO_END_OF_CONTENT))).isFalse()
  }

  @Test
  fun testFromContentRangeHeaderWithValidHeader() {
    assertValidFromContentRangeHeader("bytes 0-499/1234", 0, 499)
    assertValidFromContentRangeHeader("bytes 500-999/1234", 500, 999)
    assertValidFromContentRangeHeader("bytes 500-1233/1234", 500, BytesRange.TO_END_OF_CONTENT)
    assertValidFromContentRangeHeader("bytes 734-1233/1234", 734, BytesRange.TO_END_OF_CONTENT)
  }

  @Test
  fun testFromContentRangeHeaderWithInvalidHeader() {
    assertThat(fromContentRangeHeader(null)).isNull()
    assertInvalidFromContentRangeHeader("not bytes 0-499/1234")
    assertInvalidFromContentRangeHeader("bytes -499/1234")
    assertInvalidFromContentRangeHeader("bytes 0-/1234")
    assertInvalidFromContentRangeHeader("bytes 499/1234")
    assertInvalidFromContentRangeHeader("bytes 0-499")
    assertInvalidFromContentRangeHeader("bytes 0-/")
  }

  companion object {
    private fun assertValidFromContentRangeHeader(
        header: String?,
        expectedFrom: Int,
        expectedEnd: Int
    ) {
      val bytesRange = fromContentRangeHeader(header)
      assertThat(bytesRange).isNotNull()
      assertThat(bytesRange?.from).isEqualTo(expectedFrom)
      assertThat(bytesRange?.to).isEqualTo(expectedEnd)
    }

    private fun assertInvalidFromContentRangeHeader(header: String?) {
      try {
        fromContentRangeHeader(header)
        failBecauseExceptionWasNotThrown(IllegalArgumentException::class.java)
      } catch (x: IllegalArgumentException) {
        // Expected
      }
    }
  }
}
