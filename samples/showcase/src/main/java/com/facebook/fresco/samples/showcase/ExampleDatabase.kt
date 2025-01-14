/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import com.facebook.fresco.samples.showcase.drawee.*
import com.facebook.fresco.samples.showcase.imageformat.color.ImageFormatColorFragment
import com.facebook.fresco.samples.showcase.imageformat.datauri.ImageFormatDataUriFragment
import com.facebook.fresco.samples.showcase.imageformat.gif.ImageFormatGifFragment
import com.facebook.fresco.samples.showcase.imageformat.keyframes.ImageFormatKeyframesFragment
import com.facebook.fresco.samples.showcase.imageformat.override.ImageFormatOverrideExample
import com.facebook.fresco.samples.showcase.imageformat.pjpeg.ImageFormatProgressiveJpegFragment
import com.facebook.fresco.samples.showcase.imageformat.svg.ImageFormatSvgFragment
import com.facebook.fresco.samples.showcase.imageformat.webp.ImageFormatWebpFragment
import com.facebook.fresco.samples.showcase.imageformat.xml.ImageFormatXmlFragment
import com.facebook.fresco.samples.showcase.imagepipeline.*
import com.facebook.fresco.samples.showcase.misc.WelcomeFragment
import com.facebook.fresco.samples.showcase.settings.SettingsFragment
import com.facebook.fresco.samples.showcase.vito.*
import com.facebook.fresco.samples.showcase.vito.ninepatch.LithoNinePatchSample
import com.facebook.fresco.samples.showcase.vito.renderer.RendererColorFilterExampleFragment
import com.facebook.fresco.samples.showcase.vito.renderer.RendererFadeExampleFragment
import com.facebook.fresco.samples.showcase.vito.renderer.RendererShapeExampleFragment
import com.facebook.fresco.samples.showcase.vito.renderer.VitoLayerExample
import com.facebook.fresco.samples.showcase.vito.transition.VitoTransitionFragment

object ExampleDatabase {

  val welcome = ExampleItem("Welcome") { WelcomeFragment() }
  val settings = ExampleItem("Settings", "Settings") { SettingsFragment() }

  val examples =
      listOf(
          ExampleCategory(
              "Image Pipeline",
              listOf(
                  ExampleItem("Downsample Modes") { ImagePipelineDownsampleFragment() },
                  ExampleItem("Notifications") { ImagePipelineNotificationFragment() },
                  ExampleItem("Post-Processor") { ImagePipelinePostProcessorFragment() },
                  ExampleItem("Prefetch") { ImagePipelinePrefetchFragment() },
                  ExampleItem("Resizing") { ImagePipelineResizingFragment() },
                  ExampleItem("Qualified Resource URI") {
                    ImagePipelineQualifiedResourceFragment()
                  },
                  ExampleItem("Partial Image Loading") { PartialRequestFragment() },
                  ExampleItem("Platform Bitmap Factory") { ImagePipelineBitmapFactoryFragment() },
                  ExampleItem("Region Decoding") { ImagePipelineRegionDecodingFragment() },
              ),
          ),
          ExampleCategory(
              "Image Formats",
              listOf(
                  ExampleItem("Progressive JPEG") { ImageFormatProgressiveJpegFragment() },
                  ExampleItem("Color") { ImageFormatColorFragment() },
                  ExampleItem("GIF") { ImageFormatGifFragment() },
                  ExampleItem("WebP") { ImageFormatWebpFragment() },
                  ExampleItem("SVG") { ImageFormatSvgFragment() },
                  ExampleItem("Keyframes") { ImageFormatKeyframesFragment() },
                  ExampleItem("Decoder Override") { ImageFormatOverrideExample() },
                  ExampleItem("Data URI") { ImageFormatDataUriFragment() },
                  ExampleItem("XML") { ImageFormatXmlFragment() },
              ),
          ),
          ExampleCategory(
              "Fresco Vito",
              listOf(
                  ExampleItem(
                      "Vito Litho: Simple",
                      FrescoVitoLithoSimpleExample,
                      "Simple Fresco Vito Litho component."),
                  ExampleItem("Vito Litho: Region decoding") {
                    FrescoVitoLithoRegionDecodeFragment()
                  },
                  ExampleItem("Vito Litho: Image Options configurator") {
                    FrescoVitoLithoImageOptionsConfigFragment()
                  },
                  ExampleItem("Vito Litho: Sections (RecyclerView)") {
                    FrescoVitoLithoSectionsFragment()
                  },
                  ExampleItem(
                      "Vito Litho: Listener",
                      FrescoVitoLithoListenerExample,
                      "Vito Litho component with listener."),
                  ExampleItem("Scale Type") { VitoScaleTypeFragment() },
                  ExampleItem("Rotation") { VitoRotationFragment() },
                  ExampleItem("Vito Litho: Gallery") { FrescoVitoLithoGalleryFragment() },
                  ExampleItem("Vito View: Simple") { VitoViewSimpleFragment() },
                  ExampleItem("Vito View: Simple 2") { VitoSimpleFragment() },
                  ExampleItem("Vito View: Recycler") { VitoViewRecyclerFragment() },
                  ExampleItem("Vito View: Prefetch") { VitoViewPrefetchFragment() },
                  ExampleItem("Vito View: Kotlin Extensions") { VitoViewKtxFragment() },
                  ExampleItem(
                      "Vito Litho: Slideshow",
                      LithoSlideshowSample(),
                      "Litho slideshow that fades between images"),
                  ExampleItem(
                      "Vito Litho: NinePatch", LithoNinePatchSample(), "Litho nine patch example"),
                  ExampleItem("Vito Text Span") { VitoSpanFragment() },
                  ExampleItem(
                      "Vito Litho DrawableImageSource", FrescoVitoLithoDrawableImageSourceExample),
                  ExampleItem("Media Provider") { VitoMediaPickerFragment() },
                  ExampleItem("Multi URI") { MultiUriFragment() },
                  ExampleItem("Placeholder, Progress, Failure") { ImageLayersFragment() },
                  ExampleItem("Rounded Corners") { VitoRoundedCornersFragment() },
                  ExampleItem("Image Transition") { VitoTransitionFragment() },
                  ExampleItem("Retaining Data Source Supplier") {
                    RetainingDataSourceSupplierFragment()
                  },
              )),
          ExampleCategory(
              "Vito Renderer",
              listOf(
                  ExampleItem("Renderer: Shapes") { RendererShapeExampleFragment() },
                  ExampleItem("Renderer: Color Filters") { RendererColorFilterExampleFragment() },
                  ExampleItem("Renderer: Fading") { RendererFadeExampleFragment() },
                  ExampleItem("Layers: Scaling") { VitoLayerExample() })))
}
