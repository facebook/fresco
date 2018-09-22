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

package com.facebook.fresco.samples.showcase.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import com.facebook.common.internal.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Provider for sample URIs that are used by the samples in the showcase app
 */
public class ImageUriProvider {

  /**
   * The orientation of a sample image
   */
  public enum Orientation {
    /**
     * height > width
     */
    PORTRAIT,

    /**
     * width > height
     */
    LANDSCAPE,
  }

  /**
   * Indicates whether to perform some action on the URI before returning
   */
  public enum UriModification {

    /**
     * Do not perform any modification
     */
    NONE,

    /**
     * Add a unique parameter to the URI to prevent it to be served from any cache
     */
    CACHE_BREAKER,
  }

  public enum ImageSize {
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
    XXL("xxl");

    public final String sizeSuffix;

    ImageSize(String sizeSuffix) {
      this.sizeSuffix = sizeSuffix;
    }
  }

  private static final String PREF_KEY_CACHE_BREAKING_BY_DEFAULT = "uri_cache_breaking";
  private static final String PREF_KEY_URI_OVERRIDE = "uri_override";

  private static final int RANDOM_URI_MAX_IMAGE_ID = 1000;
  private static final String RANDOM_URI_PATTERN_S = "http://picsum.photos/400/400?image=%d";
  private static final String RANDOM_URI_PATTERN_M = "http://picsum.photos/800/800?image=%d";

  private static final String[] SAMPLE_URIS_LANDSCAPE = new String[]{
      "http://frescolib.org/static/sample-images/animal_a_%s.jpg",
      "http://frescolib.org/static/sample-images/animal_b_%s.jpg",
      "http://frescolib.org/static/sample-images/animal_c_%s.jpg",
      "http://frescolib.org/static/sample-images/animal_e_%s.jpg",
      "http://frescolib.org/static/sample-images/animal_f_%s.jpg",
      "http://frescolib.org/static/sample-images/animal_g_%s.jpg",
  };

  private static final String[] SAMPLE_URIS_PORTRAIT = new String[]{
      "http://frescolib.org/static/sample-images/animal_d_%s.jpg",
  };

  private static final String[] SAMPLE_URIS_LANDSCAPE_PNG =
      new String[] {
        "http://frescolib.org/static/sample-images/animal_a.png",
        "http://frescolib.org/static/sample-images/animal_b.png",
        "http://frescolib.org/static/sample-images/animal_c.png",
        "http://frescolib.org/static/sample-images/animal_e.png",
        "http://frescolib.org/static/sample-images/animal_f.png",
        "http://frescolib.org/static/sample-images/animal_g.png",
      };

  private static final String[] SAMPLE_URIS_PORTRAIT_PNG =
      new String[] {
        "http://frescolib.org/static/sample-images/animal_d.png",
      };

  private static final String NON_EXISTING_URI =
      "http://frescolib.org/static/sample-images/does_not_exist.jpg";

  private static final String SAMPLE_URI_WEBP_STATIC =
      "http://www.gstatic.com/webp/gallery/2.webp";

  private static final String SAMPLE_URI_WEBP_TRANSLUCENT =
      "https://www.gstatic.com/webp/gallery3/5_webp_ll.webp";

  private static final String SAMPLE_URI_WEBP_ANIMATED =
      "https://www.gstatic.com/webp/animated/1.webp";

  private static final String[] SAMPLE_URIS_GIFS =
      new String[] {
        "https://media2.giphy.com/media/3oge84qhopFbFFkwec/giphy.gif",
        "https://media3.giphy.com/media/uegrGBitPHtKM/giphy.gif",
        "https://media0.giphy.com/media/SWd9mTHEMIxQ4/giphy.gif"
      };

  private static ImageUriProvider sInstance;

  private final SharedPreferences mSharedPreferences;
  private final Random mRandom = new Random();

  private ImageUriProvider(Context context) {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static ImageUriProvider getInstance(Context context) {
    synchronized (ImageUriProvider.class) {
      if (sInstance == null) {
        sInstance = new ImageUriProvider(context.getApplicationContext());
      }
      return sInstance;
    }
  }

  /**
   * Creates an URI of an image that will result in a 404 (not found) HTTP error
   */
  public Uri createNonExistingUri() {
    return Uri.parse(NON_EXISTING_URI);
  }

  public Uri createSampleUri(ImageSize imageSize) {
    return createSampleUri(imageSize, null, UriModification.NONE);
  }

  public Uri createSampleUri(ImageSize imageSize, UriModification uriModification) {
    return createSampleUri(imageSize, null, uriModification);
  }

  public Uri createSampleUri(ImageSize imageSize, @Nullable Orientation orientation) {
    return createSampleUri(imageSize, orientation, UriModification.NONE);
  }

  public Uri createSampleUri(
      ImageSize imageSize,
      @Nullable Orientation orientation,
      UriModification urlModification) {
    final String baseUri;
    if (orientation == Orientation.PORTRAIT) {
      baseUri = chooseRandom(SAMPLE_URIS_PORTRAIT);
    } else if (orientation == Orientation.LANDSCAPE) {
      baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE);
    } else {
      baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE, SAMPLE_URIS_PORTRAIT);
    }

    final String fullUri = String.format((Locale) null, baseUri, imageSize.sizeSuffix);
    return applyOverrideSettings(fullUri, urlModification);
  }

  public Uri createPngUri() {
    return createPngUri(null, UriModification.NONE);
  }

  public Uri createPngUri(@Nullable Orientation orientation, UriModification urlModification) {
    final String baseUri;
    if (orientation == Orientation.PORTRAIT) {
      baseUri = chooseRandom(SAMPLE_URIS_PORTRAIT_PNG);
    } else if (orientation == Orientation.LANDSCAPE) {
      baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE_PNG);
    } else {
      baseUri = chooseRandom(SAMPLE_URIS_LANDSCAPE_PNG, SAMPLE_URIS_PORTRAIT_PNG);
    }
    return applyOverrideSettings(baseUri, urlModification);
  }

  public Uri createWebpStaticUri() {
    return applyOverrideSettings(SAMPLE_URI_WEBP_STATIC, UriModification.NONE);
  }

  public Uri createWebpTranslucentUri() {
    return applyOverrideSettings(SAMPLE_URI_WEBP_TRANSLUCENT, UriModification.NONE);
  }

  public Uri createWebpAnimatedUri() {
    return applyOverrideSettings(SAMPLE_URI_WEBP_ANIMATED, UriModification.NONE);
  }

  public void setUriOverride(String uri) {
    if (uri == null || uri.length() == 0) {
      mSharedPreferences.edit()
          .remove(PREF_KEY_URI_OVERRIDE)
          .apply();
    } else {
      Preconditions.checkArgument(Uri.parse(uri).isAbsolute(), "URI must be absolute");

      mSharedPreferences.edit()
          .putString(PREF_KEY_URI_OVERRIDE, uri)
          .apply();
    }
  }

  @Nullable
  public String getUriOverride() {
    String uriOverride = mSharedPreferences.getString(PREF_KEY_URI_OVERRIDE, null);
    return !TextUtils.isEmpty(uriOverride)
        ? uriOverride
        : null;
  }

  public List<Uri> getSampleUris(ImageSize imageSize) {
    ArrayList<Uri> uris = new ArrayList<>();
    for (String uri : SAMPLE_URIS_LANDSCAPE) {
      uris.add(Uri.parse(String.format((Locale) null, uri, imageSize.sizeSuffix)));
    }
    return uris;
  }

  public List<Uri> getRandomSampleUris(final ImageSize imageSize, final int numImages) {
    final String uriFormat;
    if (imageSize == ImageSize.S) {
      uriFormat = RANDOM_URI_PATTERN_S;
    } else if (imageSize == ImageSize.M) {
      uriFormat = RANDOM_URI_PATTERN_M;
    } else {
      throw new IllegalArgumentException(
          "Don't have random sample URIs for image size: " + imageSize);
    }

    Random random = new Random(0); // fix seed for reproducible order
    List<Uri> data = new ArrayList<>(numImages);

    for (int i = 0; i < numImages; i++) {
      final int imageId = random.nextInt(RANDOM_URI_MAX_IMAGE_ID);
      data.add(Uri.parse(String.format((Locale) null, uriFormat, imageId)));
    }
    return data;
  }

  public List<Uri> getSampleGifUris() {
    ArrayList<Uri> uris = new ArrayList<>();
    for (String uri : SAMPLE_URIS_GIFS) {
      uris.add(Uri.parse(uri));
    }
    return uris;
  }

  private Uri applyOverrideSettings(
      String uriString,
      UriModification urlModification) {
    if (isShouldBreakCacheByDefault()) {
      urlModification = UriModification.CACHE_BREAKER;
    }

    final String overrideUriString = getUriOverride();
    if (overrideUriString != null) {
      uriString = overrideUriString;
    }

    Uri result = Uri.parse(uriString);
    if (UriModification.CACHE_BREAKER == urlModification) {
      result = result.buildUpon()
          .appendQueryParameter("cache_breaker", UUID.randomUUID().toString())
          .build();
    }
    return result;
  }

  private boolean isShouldBreakCacheByDefault() {
    return mSharedPreferences.getBoolean(PREF_KEY_CACHE_BREAKING_BY_DEFAULT, false);
  }

  /**
   * @return a random element from a given set of arrays (uniform distribution)
   */
  private <T> T chooseRandom(final T[]... arrays) {
    int l = 0;
    for (final T[] array : arrays) {
      l += array.length;
    }
    int i = mRandom.nextInt(l);
    for (final T[] array : arrays) {
      if (i < array.length) {
        return array[i];
      }
      i -= array.length;
    }
    throw new IllegalStateException("unreachable code");
  }
}
