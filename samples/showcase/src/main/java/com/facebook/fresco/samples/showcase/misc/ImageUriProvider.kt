/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import androidx.preference.PreferenceManager
import com.facebook.common.internal.Preconditions
import com.facebook.fresco.samples.showcase.imageformat.keyframes.KeyframesDecoderExample
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormat
import java.util.*

/**
 * Provider for sample URIs that are used by the samples in the showcase app
 */
class ImageUriProvider constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

    var uriOverride: String?
        get() = sharedPreferences.getString(PREF_KEY_URI_OVERRIDE, null)
        set(uri) = if (uri.isNullOrEmpty()) {
            sharedPreferences.edit()
                    .remove(PREF_KEY_URI_OVERRIDE)
                    .apply()
        } else {
            Preconditions.checkArgument(Uri.parse(uri).isAbsolute, "URI must be absolute")

            sharedPreferences.edit()
                    .putString(PREF_KEY_URI_OVERRIDE, uri)
                    .apply()
        }

    val sampleGifUris = SAMPLE_URIS_GIFS.map(Uri::parse)

    private val isShouldBreakCacheByDefault: Boolean
        get() = sharedPreferences.getBoolean(PREF_KEY_CACHE_BREAKING_BY_DEFAULT, false)

    /**
     * The orientation of a sample image
     */
    enum class Orientation {
        /**
         * height > width
         */
        PORTRAIT,

        /**
         * width > height
         */
        LANDSCAPE,

        /**
         * Any orientation
         */
        ANY
    }

    /**
     * Indicates whether to perform some action on the URI before returning
     */
    enum class UriModification {

        /**
         * Do not perform any modification
         */
        NONE,

        /**
         * Add a unique parameter to the URI to prevent it to be served from any cache
         */
        CACHE_BREAKER
    }

    enum class ImageSize(val sizeSuffix: String) {
        /**
         * Within ~250x250 px bounds
         */
        XS("xs"),

        /**
         * Within ~450x450 px bounds
         */
        S("s"),

        /**
         * Within ~800x800 px bounds
         */
        M("m"),

        /**
         * Within ~1400x1400 px bounds
         */
        L("l"),

        /**
         * Within ~2500x2500 px bounds
         */
        XL("xl"),

        /**
         * Within ~4096x4096 px bounds
         */
        XXL("xxl")
    }

    /**
     * Creates an URI of an image that will result in a 404 (not found) HTTP error
     */
    val nonExistingUri: Uri by lazy { Uri.parse(NON_EXISTING_URI) }

    fun create(imageFormat: ImageFormat = DefaultImageFormats.JPEG): Uri? {
        return when (imageFormat) {
            DefaultImageFormats.JPEG -> createSampleUri()
            DefaultImageFormats.PNG -> createPngUri()
            DefaultImageFormats.GIF -> createGifUri()
            DefaultImageFormats.WEBP_SIMPLE -> createWebpStaticUri()
            DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA -> createWebpTranslucentUri()
            DefaultImageFormats.WEBP_ANIMATED -> createWebpAnimatedUri()
            KeyframesDecoderExample.IMAGE_FORMAT_KEYFRAMES -> createKeyframesUri()
            else -> null
        }
    }

    @JvmOverloads
    fun createSampleUri(
            imageSize: ImageSize = ImageSize.M,
            orientation: Orientation = Orientation.ANY,
            urlModification: UriModification = UriModification.NONE): Uri {
        val fullUri = String.format(randomBaseJpegUri(orientation), imageSize.sizeSuffix)
        return applyOverrideSettings(fullUri, urlModification)
    }

    @JvmOverloads
    fun createSampleUriSet(
            orientation: Orientation = Orientation.ANY,
            urlModification: UriModification = UriModification.NONE) : List<Uri> {
        val baseUri: String = randomBaseJpegUri(orientation)
        return listOf(
                applyOverrideSettings(String.format(baseUri, ImageSize.XS.sizeSuffix), urlModification),
                applyOverrideSettings(String.format(baseUri, ImageSize.S.sizeSuffix), urlModification),
                applyOverrideSettings(String.format(baseUri, ImageSize.M.sizeSuffix), urlModification),
                applyOverrideSettings(String.format(baseUri, ImageSize.L.sizeSuffix), urlModification),
                applyOverrideSettings(String.format(baseUri, ImageSize.XL.sizeSuffix), urlModification),
                applyOverrideSettings(String.format(baseUri, ImageSize.XXL.sizeSuffix), urlModification)
        )
    }

    fun createPJPEGSlow(): Uri = applyOverrideSettings(SAMPLE_URI_PJPEG_SLOW, UriModification.NONE)

    @JvmOverloads
    fun createPngUri(
            orientation: Orientation = Orientation.ANY,
            urlModification: UriModification = UriModification.NONE): Uri {
        val baseUri = when (orientation) {
            Orientation.PORTRAIT -> chooseRandom(*SAMPLE_URIS_PORTRAIT_PNG)
            Orientation.LANDSCAPE -> chooseRandom(*SAMPLE_URIS_LANDSCAPE_PNG)
            Orientation.ANY -> chooseRandom(*SAMPLE_URIS_LANDSCAPE_PNG, *SAMPLE_URIS_PORTRAIT_PNG)
        }
        return applyOverrideSettings(baseUri, urlModification)
    }

    fun createWebpStaticUri(): Uri = applyOverrideSettings(SAMPLE_URI_WEBP_STATIC, UriModification.NONE)

    fun createWebpTranslucentUri(): Uri = applyOverrideSettings(SAMPLE_URI_WEBP_TRANSLUCENT, UriModification.NONE)

    fun createWebpAnimatedUri(): Uri = applyOverrideSettings(SAMPLE_URI_WEBP_ANIMATED, UriModification.NONE)

    fun createGifUri(imageSize: ImageSize = ImageSize.M): Uri {
        return applyOverrideSettings(
                String.format(SAMPLE_URI_GIF_PATTERN, imageSize.sizeSuffix),
                UriModification.NONE)
    }

    fun createGifUriWithPause(imageSize: ImageSize): Uri {
        return applyOverrideSettings(
                String.format(SAMPLE_URI_GIF_WITH_PAUSE_PATTERN, imageSize.sizeSuffix),
                UriModification.NONE)
    }

    fun createKeyframesUri() = applyOverrideSettings(SAMPLE_URI_KEYFRAMES, UriModification.NONE)

    fun createSvgUri() = applyOverrideSettings(SAMPLE_URI_SVG, UriModification.NONE)

    fun getRandomSampleUris(imageSize: ImageSize, numImages: Int): List<Uri> {
        val uriFormat: String = when (imageSize) {
            ImageSize.S -> RANDOM_URI_PATTERN_S
            ImageSize.M -> RANDOM_URI_PATTERN_M
            ImageSize.XS, ImageSize.L, ImageSize.XL, ImageSize.XXL -> throw IllegalArgumentException(
                    "Don't have random sample URIs for image size: $imageSize")
        }

        val random = Random(0) // fix seed for reproducible order
        val data = List<Uri>(numImages) {
            val imageId = random.nextInt(RANDOM_URI_MAX_IMAGE_ID)
            Uri.parse(String.format(uriFormat, imageId))
        }
        return data
    }

    fun getMediaStoreUris(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null)?.use {
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                uris.add(
                        ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                it.getLong(dataIndex)))
            }
        }
        return uris
    }

    private fun applyOverrideSettings(
            uriString: String,
            urlModification: UriModification): Uri {
        var uriString = uriString
        var urlModification = urlModification
        if (isShouldBreakCacheByDefault) {
            urlModification = UriModification.CACHE_BREAKER
        }

        val overrideUriString = uriOverride
        if (overrideUriString != null) {
            uriString = overrideUriString
        }

        var result = Uri.parse(uriString)
        if (UriModification.CACHE_BREAKER == urlModification) {
            result = result.buildUpon()
                    .appendQueryParameter("cache_breaker", UUID.randomUUID().toString())
                    .build()
        }
        return result
    }

    private fun randomBaseJpegUri(orientation: Orientation = Orientation.ANY): String {
        return when (orientation) {
            Orientation.PORTRAIT -> chooseRandom(*SAMPLE_URIS_PORTRAIT)
            Orientation.LANDSCAPE -> chooseRandom(*SAMPLE_URIS_LANDSCAPE)
            Orientation.ANY -> chooseRandom(*SAMPLE_URIS_LANDSCAPE, *SAMPLE_URIS_PORTRAIT)
        }
    }

    /**
     * @return a random element from a given set of data (uniform distribution)
     */
    private fun <T> chooseRandom(vararg data: T): T = data.random()

    companion object {

        private const val PREF_KEY_CACHE_BREAKING_BY_DEFAULT = "uri_cache_breaking"
        private const val PREF_KEY_URI_OVERRIDE = "uri_override"

        private const val RANDOM_URI_MAX_IMAGE_ID = 1000
        private const val RANDOM_URI_PATTERN_S = "https://picsum.photos/400/400?random=%d"
        private const val RANDOM_URI_PATTERN_M = "https://picsum.photos/800/800?random=%d"

        private val SAMPLE_URIS_LANDSCAPE = arrayOf("https://frescolib.org/static/sample-images/animal_a_%s.jpg", "https://frescolib.org/static/sample-images/animal_b_%s.jpg", "https://frescolib.org/static/sample-images/animal_c_%s.jpg", "https://frescolib.org/static/sample-images/animal_e_%s.jpg", "https://frescolib.org/static/sample-images/animal_f_%s.jpg", "https://frescolib.org/static/sample-images/animal_g_%s.jpg")

        private val SAMPLE_URIS_PORTRAIT = arrayOf("https://frescolib.org/static/sample-images/animal_d_%s.jpg")

        private val SAMPLE_URI_PJPEG_SLOW = "http://pooyak.com/p/progjpeg/jpegload.cgi?o=1"

        private val SAMPLE_URIS_LANDSCAPE_PNG = arrayOf("https://frescolib.org/static/sample-images/animal_a.png", "https://frescolib.org/static/sample-images/animal_b.png", "https://frescolib.org/static/sample-images/animal_c.png", "https://frescolib.org/static/sample-images/animal_e.png", "https://frescolib.org/static/sample-images/animal_f.png", "https://frescolib.org/static/sample-images/animal_g.png")

        private val SAMPLE_URIS_PORTRAIT_PNG = arrayOf("https://frescolib.org/static/sample-images/animal_d.png")

        private val NON_EXISTING_URI = "https://frescolib.org/static/sample-images/does_not_exist.jpg"

        private val SAMPLE_URI_WEBP_STATIC = "https://www.gstatic.com/webp/gallery/2.webp"

        private val SAMPLE_URI_WEBP_TRANSLUCENT = "https://www.gstatic.com/webp/gallery3/5_webp_ll.webp"

        private val SAMPLE_URI_WEBP_ANIMATED = "https://www.gstatic.com/webp/animated/1.webp"

        private val SAMPLE_URI_GIF_PATTERN = "https://frescolib.org/static/sample-images/fresco_logo_anim_full_frames_%s.gif"

        private val SAMPLE_URI_GIF_WITH_PAUSE_PATTERN = "https://frescolib.org/static/sample-images/fresco_logo_anim_full_frames_with_pause_%s.gif"

        private val SAMPLE_URIS_GIFS = arrayOf("https://media2.giphy.com/media/3oge84qhopFbFFkwec/giphy.gif", "https://media3.giphy.com/media/uegrGBitPHtKM/giphy.gif", "https://media0.giphy.com/media/SWd9mTHEMIxQ4/giphy.gif")

        private val SAMPLE_URI_KEYFRAMES = "https://frescolib.org/static/sample-images/animation.keyframes"

        private val SAMPLE_URI_SVG = "https://frescolib.org/static/sample-images/fresco_logo_half_transparent.svg"
    }
}
