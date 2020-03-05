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

object Examples {

    fun getFragment(itemId: Int): ShowcaseFragment = when (itemId) {
        // Drawee
        R.id.nav_drawee_simple -> DraweeSimpleFragment()
        R.id.nav_drawee_media_picker -> DraweeMediaPickerFragment()
        R.id.nav_drawee_scaletype -> DraweeScaleTypeFragment()
        R.id.nav_drawee_span_simple -> DraweeSpanSimpleTextFragment()
        R.id.nav_drawee_rounded_corners -> DraweeRoundedCornersFragment()
        R.id.nav_drawee_hierarchy -> DraweeHierarchyFragment()
        R.id.nav_drawee_rotation -> DraweeRotationFragment()
        R.id.nav_drawee_recycler -> DraweeRecyclerViewFragment()
        R.id.nav_drawee_transition -> DraweeTransitionFragment()
        R.id.nav_drawee_retaining_supplier -> RetainingDataSourceSupplierFragment()
        R.id.nav_drawee_multi_uri -> MultiUriFragment()

        // Imagepipline
        R.id.nav_imagepipeline_notification -> ImagePipelineNotificationFragment()
        R.id.nav_imagepipeline_postprocessor -> ImagePipelinePostProcessorFragment()
        R.id.nav_imagepipeline_prefetch -> ImagePipelinePrefetchFragment()
        R.id.nav_imagepipeline_resizing -> ImagePipelineResizingFragment()
        R.id.nav_imagepipeline_qualified_resource -> ImagePipelineQualifiedResourceFragment()
        R.id.nav_imagepipeline_partial_request -> PartialRequestFragment()
        R.id.nav_imagepipeline_bitmap_factory -> ImagePipelineBitmapFactoryFragment()
        R.id.nav_imagepipeline_region_decoding -> ImagePipelineRegionDecodingFragment()

        // Image Formats
        R.id.nav_format_pjpeg -> ImageFormatProgressiveJpegFragment()
        R.id.nav_format_color -> ImageFormatColorFragment()
        R.id.nav_format_gif -> ImageFormatGifFragment()
        R.id.nav_format_webp -> ImageFormatWebpFragment()
        R.id.nav_format_svg -> ImageFormatSvgFragment()
        R.id.nav_format_keyframes -> ImageFormatKeyframesFragment()
        R.id.nav_format_override -> ImageFormatOverrideExample()
        R.id.nav_format_datauri -> ImageFormatDataUriFragment()

        // Experimental Fresco Vito samples
        R.id.nav_vito_litho_simple -> FrescoVitoLithoSimpleFragment()
        R.id.nav_vito_litho_region_decode -> FrescoVitoLithoRegionDecodeFragment()
        R.id.nav_vito_image_options_config -> FrescoVitoLithoImageOptionsConfigFragment()
        R.id.nav_vito_litho_sections -> FrescoVitoLithoSectionsFragment()
        R.id.nav_vito_litho_gallery -> FrescoVitoLithoGalleryFragment()
        R.id.nav_vito_view_simple -> VitoViewSimpleFragment()
        R.id.nav_vito_view_recycler -> VitoViewRecyclerFragment()
        R.id.nav_vito_view_prefetch -> VitoViewPrefetchFragment()
        R.id.nav_vito_view_ktx -> VitoViewKtxFragment()

        // More
        R.id.nav_welcome -> WelcomeFragment()
        R.id.nav_action_settings -> SettingsFragment()

        // Default to the welcome fragment
        else -> WelcomeFragment()
    }
}
