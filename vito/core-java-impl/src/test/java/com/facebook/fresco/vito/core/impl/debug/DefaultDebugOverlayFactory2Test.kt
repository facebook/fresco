/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.util.Pair
import com.facebook.common.internal.Supplier
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.impl.FrescoDrawable2
import com.facebook.fresco.vito.options.ImageOptions
import java.util.LinkedHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultDebugOverlayFactory2Test {

  private lateinit var debugOverlayEnabled: Supplier<Boolean>
  private lateinit var overlay: DebugOverlayDrawable
  private lateinit var frescoDrawable2: FrescoDrawable2

  @Before
  fun setUp() {
    debugOverlayEnabled = Supplier { true }
    overlay = DebugOverlayDrawable("test")
    frescoDrawable2 = mock()
    whenever(frescoDrawable2.imageId).thenReturn(42L)
    whenever(frescoDrawable2.bounds).thenReturn(Rect(0, 0, 200, 100))
    whenever(frescoDrawable2.actualImageWidthPx).thenReturn(400)
    whenever(frescoDrawable2.actualImageHeightPx).thenReturn(300)
    whenever(frescoDrawable2.actualImageFocusPoint).thenReturn(null)
    whenever(frescoDrawable2.imageRequest).thenReturn(null)
  }

  @Test
  fun testSetData_withExtendedInfo_setsDrawIdentifierTrue() {
    val factory = createFactory(showExtendedInformation = true)
    overlay.drawIdentifier = false
    factory.callSetData(overlay, frescoDrawable2, null)
    assertTrue(overlay.drawIdentifier)
  }

  @Test
  fun testSetData_withoutExtendedInfo_setsDrawIdentifierFalse() {
    val factory = createFactory(showExtendedInformation = false)
    overlay.drawIdentifier = true
    factory.callSetData(overlay, frescoDrawable2, null)
    assertFalse(overlay.drawIdentifier)
  }

  @Test
  fun testSetData_withExtendedInfo_usesIdTagForImageId() {
    val factory = createFactory(showExtendedInformation = true)
    factory.callSetData(overlay, frescoDrawable2, null)
    assertEquals("v42", getDebugData(overlay)["ID"]?.first)
  }

  @Test
  fun testSetData_withoutExtendedInfo_usesOverlayIdentifierAsTag() {
    val factory = createFactory(showExtendedInformation = false)
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertEquals("v42", data["test"]?.first)
    assertNull(data["ID"])
  }

  @Test
  fun testSetData_withFrescoDrawable2_addsDimensionData() {
    val factory = createFactory(showExtendedInformation = false)
    whenever(frescoDrawable2.bounds).thenReturn(Rect(0, 0, 320, 240))
    whenever(frescoDrawable2.actualImageWidthPx).thenReturn(640)
    whenever(frescoDrawable2.actualImageHeightPx).thenReturn(480)
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertEquals("320x240", data["D"]?.first)
    assertEquals("640x480", data["I"]?.first)
  }

  @Test
  fun testSetData_withExtendedInfo_addsAspectRatios() {
    val factory = createFactory(showExtendedInformation = true)
    whenever(frescoDrawable2.bounds).thenReturn(Rect(0, 0, 200, 100))
    whenever(frescoDrawable2.actualImageWidthPx).thenReturn(800)
    whenever(frescoDrawable2.actualImageHeightPx).thenReturn(400)
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertEquals("2.0", data["DAR"]?.first)
    assertEquals("2.0", data["IAR"]?.first)
  }

  @Test
  fun testSetData_withZeroImageHeight_skipsImageAspectRatio() {
    val factory = createFactory(showExtendedInformation = true)
    whenever(frescoDrawable2.actualImageHeightPx).thenReturn(0)
    factory.callSetData(overlay, frescoDrawable2, null)
    assertNull(getDebugData(overlay)["IAR"])
  }

  @Test
  fun testSetData_withFocusPoint_addsFocusPointData() {
    val factory = createFactory(showExtendedInformation = false)
    whenever(frescoDrawable2.actualImageFocusPoint).thenReturn(PointF(0.5f, 0.7f))
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertEquals("0.5", data["FocusPointX"]?.first)
    assertEquals("0.7", data["FocusPointY"]?.first)
  }

  @Test
  fun testSetData_withoutFocusPoint_skipsFocusPointData() {
    val factory = createFactory(showExtendedInformation = false)
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertNull(data["FocusPointX"])
    assertNull(data["FocusPointY"])
  }

  @Test
  fun testSetData_nonFrescoDrawable2_onlyAddsImageId() {
    val factory = createFactory(showExtendedInformation = true)
    val plainDrawable: FrescoDrawableInterface = mock()
    whenever(plainDrawable.imageId).thenReturn(99L)
    whenever(plainDrawable.imageRequest).thenReturn(null)
    factory.callSetData(overlay, plainDrawable, null)
    val data = getDebugData(overlay)
    assertEquals("v99", data["ID"]?.first)
    assertNull(data["D"])
    assertNull(data["I"])
  }

  @Test
  fun testSetData_nullExtras_showsUnknownOrigin_extendedInfo() {
    val factory = createFactory(showExtendedInformation = true)
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertEquals("unknown", data["origin"]?.first)
    assertEquals(
        DebugOverlayImageOriginColor.getImageOriginColor("unknown"),
        data["origin"]?.second,
    )
    assertEquals("unknown", data[HasExtraData.KEY_ORIGIN_SUBCATEGORY]?.first)
    assertEquals(Color.GRAY, data[HasExtraData.KEY_ORIGIN_SUBCATEGORY]?.second)
  }

  @Test
  fun testSetData_nullExtras_showsUnknownOrigin_compactInfo() {
    val factory = createFactory(showExtendedInformation = false)
    factory.callSetData(overlay, frescoDrawable2, null)
    val data = getDebugData(overlay)
    assertEquals("unknown | unknown", data["o"]?.first)
    assertEquals(DebugOverlayImageOriginColor.getImageOriginColor("unknown"), data["o"]?.second)
  }

  @Test
  fun testSetData_withDatasourceExtras_showsOriginFromDatasource() {
    val factory = createFactory(showExtendedInformation = true)
    val extras = Extras()
    extras.datasourceExtras =
        mapOf("origin" to "network", HasExtraData.KEY_ORIGIN_SUBCATEGORY to "http")
    factory.callSetData(overlay, frescoDrawable2, extras)
    val data = getDebugData(overlay)
    assertEquals("network", data["origin"]?.first)
    assertEquals(
        DebugOverlayImageOriginColor.getImageOriginColor("network"),
        data["origin"]?.second,
    )
    assertEquals("http", data[HasExtraData.KEY_ORIGIN_SUBCATEGORY]?.first)
  }

  @Test
  fun testSetData_withShortcutExtrasOnly_fallsBackToShortcut() {
    val factory = createFactory(showExtendedInformation = true)
    val extras = Extras()
    extras.datasourceExtras = null
    extras.shortcutExtras =
        mapOf("origin" to "memory_bitmap", HasExtraData.KEY_ORIGIN_SUBCATEGORY to "cache_hit")
    factory.callSetData(overlay, frescoDrawable2, extras)
    val data = getDebugData(overlay)
    assertEquals("memory_bitmap", data["origin"]?.first)
    assertEquals("cache_hit", data[HasExtraData.KEY_ORIGIN_SUBCATEGORY]?.first)
  }

  @Test
  fun testSetData_datasourceExtrasTakesPriorityOverShortcut() {
    val factory = createFactory(showExtendedInformation = true)
    val extras = Extras()
    extras.datasourceExtras =
        mapOf("origin" to "disk", HasExtraData.KEY_ORIGIN_SUBCATEGORY to "disk_cache")
    extras.shortcutExtras =
        mapOf("origin" to "memory_bitmap", HasExtraData.KEY_ORIGIN_SUBCATEGORY to "cache_hit")
    factory.callSetData(overlay, frescoDrawable2, extras)
    val data = getDebugData(overlay)
    assertEquals("disk", data["origin"]?.first)
    assertEquals("disk_cache", data[HasExtraData.KEY_ORIGIN_SUBCATEGORY]?.first)
  }

  @Test
  fun testSetData_imageFormatFallbackChain_prioritizesImageExtras() {
    val factory = createFactory(showExtendedInformation = false)
    val extras = Extras()
    extras.imageExtras = mapOf("image_format" to "JPEG")
    extras.shortcutExtras = mapOf("image_format" to "PNG")
    extras.datasourceExtras = mapOf("image_format" to "WEBP")
    factory.callSetData(overlay, frescoDrawable2, extras)
    assertEquals("JPEG", getDebugData(overlay)["format"]?.first)
  }

  @Test
  fun testSetData_imageFormatFallback_usesShortcutWhenNoImageExtras() {
    val factory = createFactory(showExtendedInformation = false)
    val extras = Extras()
    extras.imageExtras = null
    extras.shortcutExtras = mapOf("image_format" to "PNG")
    factory.callSetData(overlay, frescoDrawable2, extras)
    assertEquals("PNG", getDebugData(overlay)["format"]?.first)
  }

  @Test
  fun testSetData_imageFormatFallback_usesDatasourceAsLastResort() {
    val factory = createFactory(showExtendedInformation = false)
    val extras = Extras()
    extras.imageExtras = null
    extras.shortcutExtras = null
    extras.datasourceExtras = mapOf("image_format" to "WEBP")
    factory.callSetData(overlay, frescoDrawable2, extras)
    assertEquals("WEBP", getDebugData(overlay)["format"]?.first)
  }

  @Test
  fun testSetData_noImageFormat_skipsFormatEntry() {
    val factory = createFactory(showExtendedInformation = false)
    val extras = Extras()
    extras.imageExtras = null
    extras.shortcutExtras = null
    extras.datasourceExtras = null
    factory.callSetData(overlay, frescoDrawable2, extras)
    assertNull(getDebugData(overlay)["format"])
  }

  @Test
  fun testSetData_emptyImageFormat_skipsFormatEntry() {
    val factory = createFactory(showExtendedInformation = false)
    val extras = Extras()
    extras.imageExtras = mapOf("image_format" to "")
    factory.callSetData(overlay, frescoDrawable2, extras)
    assertNull(getDebugData(overlay)["format"])
  }

  @Test
  fun testSetData_imageSourceExtrasEnabled_addsSourceExtras() {
    val factory = createFactory(showExtendedInformation = false)
    factory.setShowExtendedImageSourceExtraInformation(true)
    val extras = Extras()
    extras.imageSourceExtras = mapOf("src_key1" to "val1", "src_key2" to "val2")
    factory.callSetData(overlay, frescoDrawable2, extras)
    val data = getDebugData(overlay)
    assertEquals("val1", data["src_key1"]?.first)
    assertEquals("val2", data["src_key2"]?.first)
  }

  @Test
  fun testSetData_imageSourceExtrasDisabled_skipsSourceExtras() {
    val factory = createFactory(showExtendedInformation = false)
    val extras = Extras()
    extras.imageSourceExtras = mapOf("src_key1" to "val1")
    factory.callSetData(overlay, frescoDrawable2, extras)
    assertNull(getDebugData(overlay)["src_key1"])
  }

  @Test
  fun testSetData_imageSourceExtrasEnabledButNullExtras_doesNotCrash() {
    val factory = createFactory(showExtendedInformation = false)
    factory.setShowExtendedImageSourceExtraInformation(true)
    factory.callSetData(overlay, frescoDrawable2, null)
    assertNull(getDebugData(overlay)["src_key1"])
  }

  @Test
  fun testSetData_withImageRequestAndExtendedInfo_addsScaleType() {
    val factory = createFactory(showExtendedInformation = true)
    val imageOptions = ImageOptions.defaults()
    val imageRequest = VitoImageRequest(
        resources = mock(),
        imageSource = mock(),
        imageOptions = imageOptions,
        finalImageRequest = null,
        finalImageCacheKey = null,
    )
    whenever(frescoDrawable2.imageRequest).thenReturn(imageRequest)
    factory.callSetData(overlay, frescoDrawable2, null)
    assertEquals(
        imageOptions.actualImageScaleType.toString(),
        getDebugData(overlay)["scale"]?.first,
    )
  }

  @Test
  fun testSetData_withNullImageRequest_skipsScaleType() {
    val factory = createFactory(showExtendedInformation = true)
    factory.callSetData(overlay, frescoDrawable2, null)
    assertNull(getDebugData(overlay)["scale"])
  }

  @Test
  fun testSetData_withImageRequestButNoExtendedInfo_skipsScaleType() {
    val factory = createFactory(showExtendedInformation = false)
    val imageRequest: VitoImageRequest = mock()
    whenever(frescoDrawable2.imageRequest).thenReturn(imageRequest)
    factory.callSetData(overlay, frescoDrawable2, null)
    assertNull(getDebugData(overlay)["scale"])
  }

  private fun createFactory(showExtendedInformation: Boolean = true): TestableFactory {
    return TestableFactory(showExtendedInformation, debugOverlayEnabled)
  }

  @Suppress("UNCHECKED_CAST")
  private fun getDebugData(
      overlay: DebugOverlayDrawable,
  ): LinkedHashMap<String, Pair<String, Int>> {
    val field = DebugOverlayDrawable::class.java.getDeclaredField("debugData")
    field.isAccessible = true
    return field.get(overlay) as LinkedHashMap<String, Pair<String, Int>>
  }

  private class TestableFactory(
      showExtendedInformation: Boolean,
      debugOverlayEnabled: Supplier<Boolean>,
  ) :
      DefaultDebugOverlayFactory2(
          showExtendedInformation = showExtendedInformation,
          showExtendedImageSourceExtraInformation = false,
          debugOverlayEnabled = debugOverlayEnabled,
      ) {
    fun callSetData(
        overlay: DebugOverlayDrawable,
        drawable: FrescoDrawableInterface,
        extras: Extras?,
    ) {
      setData(overlay, drawable, extras)
    }
  }
}
