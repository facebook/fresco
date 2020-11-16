/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import com.facebook.fresco.samples.showcase.drawee.*
import com.facebook.fresco.samples.showcase.drawee.transition.DraweeTransitionFragment
import com.facebook.fresco.samples.showcase.imageformat.color.ImageFormatColorFragment
import com.facebook.fresco.samples.showcase.imageformat.datauri.ImageFormatDataUriFragment
import com.facebook.fresco.samples.showcase.imageformat.gif.ImageFormatGifFragment
import com.facebook.fresco.samples.showcase.imageformat.keyframes.ImageFormatKeyframesFragment
import com.facebook.fresco.samples.showcase.imageformat.override.ImageFormatOverrideExample
import com.facebook.fresco.samples.showcase.imageformat.pjpeg.ImageFormatProgressiveJpegFragment
import com.facebook.fresco.samples.showcase.imageformat.svg.ImageFormatSvgFragment
import com.facebook.fresco.samples.showcase.imageformat.webp.ImageFormatWebpFragment
import com.facebook.fresco.samples.showcase.imagepipeline.*
import com.facebook.fresco.samples.showcase.misc.WelcomeFragment
import com.facebook.fresco.samples.showcase.settings.SettingsFragment
import com.facebook.fresco.samples.showcase.vito.*

object ExampleDatabase {

  val welcome = ExampleItem("Welcome") { WelcomeFragment() }
  val settings = ExampleItem("Settings", "Settings") { SettingsFragment() }

  val examples =
      listOf(
          ExampleCategory(
              "Drawee",
              listOf(
                  ExampleItem("Simple Drawee") { DraweeSimpleFragment() },
                  ExampleItem("Media Provider") { DraweeMediaPickerFragment() },
                  ExampleItem("Scale Type") { DraweeScaleTypeFragment() },
                  ExampleItem("Simple DraweeSpan") { DraweeSpanSimpleTextFragment() },
                  ExampleItem("Rounded Corners") { DraweeRoundedCornersFragment() },
                  ExampleItem("Placeholder, Progress, Failure") { DraweeHierarchyFragment() },
                  ExampleItem("Rotation") { DraweeRotationFragment() },
                  ExampleItem("Recycler View") { DraweeRecyclerViewFragment() },
                  ExampleItem("Drawee Transistion") { DraweeTransitionFragment() },
                  ExampleItem("Retaining Data Source Supplier") {
                    RetainingDataSourceSupplierFragment()
                  },
                  ExampleItem("Multi URI") { MultiUriFragment() })),
          ExampleCategory(
              "Image Pipeline",
              listOf(
                  ExampleItem("Notifications") { ImagePipelineNotificationFragment() },
                  ExampleItem("Post-Processor") { ImagePipelinePostProcessorFragment() },
                  ExampleItem("Prefetch") { ImagePipelinePrefetchFragment() },
                  ExampleItem("Resizing") { ImagePipelineResizingFragment() },
                  ExampleItem("Qualified Resource URI") {
                    ImagePipelineQualifiedResourceFragment()
                  },
                  ExampleItem("Partial Image Loading") { PartialRequestFragment() },
                  ExampleItem("Platform Bitmap Factory") { ImagePipelineBitmapFactoryFragment() },
                  ExampleItem("Region Decoding") { ImagePipelineRegionDecodingFragment() })),
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
                  ExampleItem("Data URI") { ImageFormatDataUriFragment() })),
          ExampleCategory(
              "Fresco Vito",
              listOf(
                  ExampleItem("Vito Litho: Simple") { FrescoVitoLithoSimpleFragment() },
                  ExampleItem("Vito Litho: Region decoding") {
                    FrescoVitoLithoRegionDecodeFragment()
                  },
                  ExampleItem("Vito Litho: Image Options configurator") {
                    FrescoVitoLithoImageOptionsConfigFragment()
                  },
                  ExampleItem("Vito Litho: Sections (RecyclerView)") {
                    FrescoVitoLithoSectionsFragment()
                  },
                  ExampleItem("Vito Litho: Gallery") { FrescoVitoLithoGalleryFragment() },
                  ExampleItem("Vito View: Simple") { VitoViewSimpleFragment() },
                  ExampleItem("Vito View: Recycler") { VitoViewRecyclerFragment() },
                  ExampleItem("Vito View: Prefetch") { VitoViewPrefetchFragment() },
                  ExampleItem("Vito View: Kotlin Extensions") { VitoViewKtxFragment() },
                  ExampleItem("Vito Litho: Slideshow") { FrescoVitoLithoSlideshowFragment() })))
}
