/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.net.Uri
import com.facebook.drawee.drawable.SizingHint
import com.facebook.fresco.vito.source.EmptyImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.source.SmartFetchOptIn
import com.facebook.fresco.vito.source.SmartImageSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageSourceProviderSmartForUriTest {

  private lateinit var originalUriParser: (String) -> Uri?
  private var originalTreatEmptyUriAsImageSource = false

  @Before
  fun setup() {
    originalUriParser = ImageSourceProvider.uriParser
    originalTreatEmptyUriAsImageSource = ImageSourceProvider.treatEmptyUriAsImageSource
  }

  @After
  fun tearDown() {
    ImageSourceProvider.uriParser = originalUriParser
    ImageSourceProvider.treatEmptyUriAsImageSource = originalTreatEmptyUriAsImageSource
  }

  @Test
  fun testSmartForUri_whenUri_thenSmartImageSource() {
    assertThat(ImageSourceProvider.smartForUri(IMAGE_URL, SmartFetchOptIn.ENABLED_NO_FALLBACK))
        .isEqualTo(
            SmartImageSource(
                Uri.parse(IMAGE_URL),
                SizingHint.DEFAULT,
                null,
                SmartFetchOptIn.ENABLED_NO_FALLBACK,
            ),
        )
  }

  @Test
  fun testSmartForUri_whenSizingHintAndExtras_thenSmartImageSourceKeepsThem() {
    val source =
        ImageSourceProvider.smartForUri(
            Uri.parse(IMAGE_URL),
            SmartFetchOptIn.ENABLED_WITH_FALLBACK,
            SizingHint.LOW_QUALITY_FAST_LOADING,
            EXTRAS,
        )

    assertThat(source)
        .isEqualTo(
            SmartImageSource(
                Uri.parse(IMAGE_URL),
                SizingHint.LOW_QUALITY_FAST_LOADING,
                EXTRAS,
                SmartFetchOptIn.ENABLED_WITH_FALLBACK,
            ),
        )
  }

  @Test
  fun testSmartForUri_whenNullUri_thenSameEmptySourceAsForUri() {
    val stringSource =
        ImageSourceProvider.smartForUri(null as String?, SmartFetchOptIn.ENABLED_NO_FALLBACK)
    val uriSource =
        ImageSourceProvider.smartForUri(null as Uri?, SmartFetchOptIn.ENABLED_NO_FALLBACK)

    assertThat(stringSource).isSameAs(ImageSourceProvider.forUri(null as String?))
    assertThat(uriSource).isSameAs(ImageSourceProvider.forUri(null as Uri?))
  }

  @Test
  fun testSmartForUri_whenEmptyUriAndEmptyUriIsEmptySource_thenSameEmptySourceAsForUri() {
    ImageSourceProvider.treatEmptyUriAsImageSource = true

    assertThat(ImageSourceProvider.smartForUri(Uri.EMPTY, SmartFetchOptIn.ENABLED_NO_FALLBACK))
        .isInstanceOf(EmptyImageSource::class.java)
        .isSameAs(ImageSourceProvider.forUri(Uri.EMPTY))
    assertThat(ImageSourceProvider.smartForUri("", SmartFetchOptIn.ENABLED_NO_FALLBACK))
        .isInstanceOf(EmptyImageSource::class.java)
        .isSameAs(ImageSourceProvider.forUri(""))
  }

  @Test
  fun testSmartForUri_whenEmptyUriAndEmptyUriIsNotEmptySource_thenSmartImageSource() {
    ImageSourceProvider.treatEmptyUriAsImageSource = false

    assertThat(ImageSourceProvider.forUri("")).isNotInstanceOf(EmptyImageSource::class.java)
    assertThat(ImageSourceProvider.smartForUri("", SmartFetchOptIn.ENABLED_NO_FALLBACK))
        .isInstanceOf(SmartImageSource::class.java)
  }

  @Test
  fun testSmartForUri_whenUriParserReturnsNull_thenSameEmptySourceAsForUri() {
    ImageSourceProvider.uriParser = { null }

    assertThat(ImageSourceProvider.smartForUri(IMAGE_URL, SmartFetchOptIn.ENABLED_NO_FALLBACK))
        .isInstanceOf(EmptyImageSource::class.java)
        .isSameAs(ImageSourceProvider.forUri(IMAGE_URL))
  }

  private companion object {
    const val IMAGE_URL = "https://scontent.xx.fbcdn.net/v/t39.30808-6/1_2_3_n.jpg?ctp=s160x160"
    val EXTRAS: Map<String, Any> = mapOf("key" to "value")
  }
}
