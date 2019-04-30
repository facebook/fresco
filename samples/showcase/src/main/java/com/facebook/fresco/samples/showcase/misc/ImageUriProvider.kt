/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.fresco.samples.showcase.misc

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.TextUtils
import androidx.preference.PreferenceManager
import com.facebook.common.internal.Preconditions
import java.util.*

/**
 * Provider for sample URIs that are used by the samples in the showcase app
 */
class ImageUriProvider constructor(context: Context) {

    private val mSharedPreferences: SharedPreferences
    private val mRandom = Random()

    var uriOverride: String?
        get() {
            val uriOverride = mSharedPreferences.getString(PREF_KEY_URI_OVERRIDE, null)
            return if (!TextUtils.isEmpty(uriOverride))
                uriOverride
            else
                null
        }
        set(uri) = if (uri == null || uri.length == 0) {
            mSharedPreferences.edit()
                    .remove(PREF_KEY_URI_OVERRIDE)
                    .apply()
        } else {
            Preconditions.checkArgument(Uri.parse(uri).isAbsolute, "URI must be absolute")

            mSharedPreferences.edit()
                    .putString(PREF_KEY_URI_OVERRIDE, uri)
                    .apply()
        }

    val sampleGifUris: List<Uri>
        get() {
            val uris = ArrayList<Uri>()
            for (uri in SAMPLE_URIS_GIFS) {
                uris.add(Uri.parse(uri))
            }
            return uris
        }

    private val isShouldBreakCacheByDefault: Boolean
        get() = mSharedPreferences.getBoolean(PREF_KEY_CACHE_BREAKING_BY_DEFAULT, false)

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
        LANDSCAPE
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

    init {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Creates an URI of an image that will result in a 404 (not found) HTTP error
     */
    fun createNonExistingUri(): Uri {
        return Uri.parse(NON_EXISTING_URI)
    }

    fun createSampleUri(imageSize: ImageSize, uriModification: UriModification): Uri {
        return createSampleUri(imageSize, null, uriModification)
    }

    @JvmOverloads
    fun createSampleUri(
            imageSize: ImageSize,
            orientation: Orientation? = null,
            urlModification: UriModification = UriModification.NONE): Uri {
        val baseUri: String
        if (orientation == Orientation.PORTRAIT) {
            baseUri = chooseRandom(SAMPLE_URIS_PORTRAIT)
        } else if (orientation == Orientation.LANDSCAPE) {
            baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE)
        } else {
            baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE, SAMPLE_URIS_PORTRAIT)
        }

        val fullUri = String.format(baseUri, imageSize.sizeSuffix)
        return applyOverrideSettings(fullUri, urlModification)
    }

    fun createPJPEGSlow(): Uri {
        return applyOverrideSettings(SAMPLE_URI_PJPEG_SLOW, UriModification.NONE)
    }

    @JvmOverloads
    fun createPngUri(orientation: Orientation? = null, urlModification: UriModification = UriModification.NONE): Uri {
        val baseUri: String
        if (orientation == Orientation.PORTRAIT) {
            baseUri = chooseRandom(SAMPLE_URIS_PORTRAIT_PNG)
        } else if (orientation == Orientation.LANDSCAPE) {
            baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE_PNG)
        } else {
            baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE_PNG, SAMPLE_URIS_PORTRAIT_PNG)
        }
        return applyOverrideSettings(baseUri, urlModification)
    }

    fun createWebpStaticUri(): Uri {
        return applyOverrideSettings(SAMPLE_URI_WEBP_STATIC, UriModification.NONE)
    }

    fun createWebpTranslucentUri(): Uri {
        return applyOverrideSettings(SAMPLE_URI_WEBP_TRANSLUCENT, UriModification.NONE)
    }

    fun createWebpAnimatedUri(): Uri {
        return applyOverrideSettings(SAMPLE_URI_WEBP_ANIMATED, UriModification.NONE)
    }

    fun createGifUri(imageSize: ImageSize): Uri {
        return applyOverrideSettings(
                String.format((null as Locale?)!!, SAMPLE_URI_GIF_PATTERN, imageSize.sizeSuffix),
                UriModification.NONE)
    }

    fun createGifUriWithPause(imageSize: ImageSize): Uri {
        return applyOverrideSettings(
                String.format((null as Locale?)!!, SAMPLE_URI_GIF_WITH_PAUSE_PATTERN, imageSize.sizeSuffix),
                UriModification.NONE)
    }

    fun createKeyframesUri(): Uri {
        return applyOverrideSettings(SAMPLE_URI_KEYFRAMES, UriModification.NONE)
    }

    fun createSvgUri(): Uri {
        return applyOverrideSettings(SAMPLE_URI_SVG, UriModification.NONE)
    }

    fun getSampleUris(imageSize: ImageSize): List<Uri> {
        val uris = ArrayList<Uri>()
        for (uri in SAMPLE_URIS_LANDSCAPE) {
            uris.add(Uri.parse(String.format((null as Locale?)!!, uri, imageSize.sizeSuffix)))
        }
        return uris
    }

    fun getRandomSampleUris(imageSize: ImageSize, numImages: Int): List<Uri> {
        val uriFormat: String
        if (imageSize == ImageSize.S) {
            uriFormat = RANDOM_URI_PATTERN_S
        } else if (imageSize == ImageSize.M) {
            uriFormat = RANDOM_URI_PATTERN_M
        } else {
            throw IllegalArgumentException(
                    "Don't have random sample URIs for image size: $imageSize")
        }

        val random = Random(0) // fix seed for reproducible order
        val data = ArrayList<Uri>(numImages)

        for (i in 0 until numImages) {
            val imageId = random.nextInt(RANDOM_URI_MAX_IMAGE_ID)
            data.add(Uri.parse(String.format((null as Locale?)!!, uriFormat, imageId)))
        }
        return data
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

    /**
     * @return a random element from a given set of arrays (uniform distribution)
     */
    private fun <T> chooseRandom(vararg arrays: Array<T>): T {
        var l = 0
        for (array in arrays) {
            l += array.size
        }
        var i = mRandom.nextInt(l)
        for (array in arrays) {
            if (i < array.size) {
                return array[i]
            }
            i -= array.size
        }
        throw IllegalStateException("unreachable code")
    }

    companion object {

        private val PREF_KEY_CACHE_BREAKING_BY_DEFAULT = "uri_cache_breaking"
        private val PREF_KEY_URI_OVERRIDE = "uri_override"

        private val RANDOM_URI_MAX_IMAGE_ID = 1000
        private val RANDOM_URI_PATTERN_S = "https://picsum.photos/400/400?image=%d"
        private val RANDOM_URI_PATTERN_M = "https://picsum.photos/800/800?image=%d"

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
