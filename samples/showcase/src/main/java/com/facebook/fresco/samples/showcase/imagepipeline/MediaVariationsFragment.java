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

package com.facebook.fresco.samples.showcase.imagepipeline;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.imagepipeline.widget.ResizableFrameLayout;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequest.CacheChoice;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.MediaVariations;

public class MediaVariationsFragment extends BaseShowcaseFragment {

  private static final String TAG = "MediaVariationsFragment";
  private static final String URI_TEMPLATE
      = "http://frescolib.org/static/sample-images/monkey-selfie-%s.%s";
  private static final String MEDIA_ID = "monkey-selfie";
  private SimpleDraweeView mMainImageDraweeView;

  private enum Size {
    XS(R.id.thumb_xs, "xs", "png", 377, 523, CacheChoice.SMALL),
    S(R.id.thumb_s, "s", "webp", 629, 871, CacheChoice.SMALL),
    M(R.id.thumb_m, "m", "jpg", 1048, 1451, CacheChoice.DEFAULT),
    L(R.id.thumb_l, "l", "jpg", 1747, 2418, CacheChoice.DEFAULT),
    XL(R.id.thumb_xl, "xl", "jpg", 2912, 4030, CacheChoice.DEFAULT);

    final @IdRes int thumbViewId;
    final String name;
    final Uri uri;
    final int width;
    final int height;
    final CacheChoice cacheChoice;

    Size(
        @IdRes int thumbViewId,
        String name,
        String extension,
        int width,
        int height,
        CacheChoice cacheChoice) {
      this.thumbViewId = thumbViewId;
      this.name = name;
      this.uri = Uri.parse(String.format(URI_TEMPLATE, name, extension));
      this.width = width;
      this.height = height;
      this.cacheChoice = cacheChoice;
    }
  }

  private enum Mode {
    MEDIA_ID_IN_REQUEST(
        R.id.media_variations_mode_media_id,
        R.string.imagepipeline_media_variations_toast_mode_media_id),
    LISTED_IN_REQUEST(
        R.id.media_variations_mode_listed_variants,
        R.string.imagepipeline_media_variations_toast_mode_listed_variants);

    final @IdRes int menuItemId;
    final @StringRes int toastMessageId;

    Mode(int menuItemId, int toastMessageId) {
      this.menuItemId = menuItemId;
      this.toastMessageId = toastMessageId;
    }
  }

  private Mode mMode;

  public MediaVariationsFragment() {
    setHasOptionsMenu(true);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_media_variations, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    populateImagesAfterInitialLayout(view);

    setMode(Mode.MEDIA_ID_IN_REQUEST);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.fragment_imagepipeline_media_variations, menu);

    menu.findItem(mMode.menuItemId).setChecked(true);

    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.media_variations_clear_cache) {
      Fresco.getImagePipeline().clearCaches();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        getActivity().recreate();
      } else {
        Toast.makeText(
            getActivity(),
            R.string.imagepipeline_media_variations_restart_toast,
            Toast.LENGTH_SHORT).show();
        getActivity().finish();
      }
      return true;
    } else if (item.getGroupId() == R.id.media_variations_modes) {
      for (Mode mode : Mode.values()) {
        if (mode.menuItemId == item.getItemId()) {
          setMode(mode);
          break;
        }
      }
      clearMainImageAndBitmapCache();
      item.setChecked(true);
    }

    return super.onOptionsItemSelected(item);
  }

  private void setMode(Mode mode) {
    mMode = mode;
    Toast.makeText(getActivity(), mMode.toastMessageId, Toast.LENGTH_SHORT).show();
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_media_variations_title;
  }

  private void populateImagesAfterInitialLayout(final View rootView) {
    rootView.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            populateMainImage(rootView);
            for (Size size : Size.values()) {
              populateThumb(rootView, size.thumbViewId, size);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
              rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
              rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
          }
        });
  }

  private void populateThumb(View rootView, @IdRes int viewId, final Size size) {
    final SimpleDraweeView draweeView = (SimpleDraweeView) rootView.findViewById(viewId);

    loadThumb(draweeView, size, ImageRequest.RequestLevel.DISK_CACHE);

    draweeView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loadThumb(draweeView, size, ImageRequest.RequestLevel.FULL_FETCH);
      }
    });
  }

  private void loadThumb(
      SimpleDraweeView draweeView,
      Size size,
      ImageRequest.RequestLevel requestLevel) {
    ImageRequest request = ImageRequestBuilder.newBuilderWithSource(size.uri)
        .setMediaVariations(MediaVariations.newBuilderForMediaId(MEDIA_ID)
            .setForceRequestForSpecifiedUri(true)
            .build())
        .setLowestPermittedRequestLevel(requestLevel)
        .setResizeOptions(new ResizeOptions(draweeView.getWidth(), draweeView.getHeight()))
        .setCacheChoice(size.cacheChoice)
        .build();
    DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setImageRequest(request)
        .setOldController(draweeView.getController())
        .build();
    draweeView.setController(controller);
  }

  private void populateMainImage(View rootView) {
    mMainImageDraweeView = (SimpleDraweeView) rootView.findViewById(R.id.img_main);
    loadMainImage();

    mMainImageDraweeView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loadMainImage();
      }
    });

    ResizableFrameLayout mainImageFrameLayout =
        (ResizableFrameLayout) rootView.findViewById(R.id.frame_main);
    mainImageFrameLayout.init(rootView.findViewById(R.id.btn_resize));
  }

  private void loadMainImage() {
    // Request a non-existent image to force fallback to the variations
    Uri uri = Uri.parse(String.format(URI_TEMPLATE, "full", "jpg"));
    ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
        .setMediaVariations(getMediaVariationsForMode(mMode))
        .setResizeOptions(new ResizeOptions(
            mMainImageDraweeView.getWidth(),
            mMainImageDraweeView.getHeight()))
        .build();

    Log.i(
        TAG,
        request.getMediaVariations() == null ? "null" : request.getMediaVariations().toString());

    setDraweeControllerForRequest(request);
  }

  private void clearMainImageAndBitmapCache() {
    Uri uri = Uri.parse(String.format(URI_TEMPLATE, "full", "jpg"));
    setDraweeControllerForRequest(ImageRequest.fromUri(uri));

    Fresco.getImagePipeline().clearMemoryCaches();
  }

  private void setDraweeControllerForRequest(ImageRequest imageRequest) {
    DraweeController controller = Fresco.newDraweeControllerBuilder()
      .setImageRequest(imageRequest)
      .setOldController(mMainImageDraweeView.getController())
      .setRetainImageOnFailure(true)
      .build();
    mMainImageDraweeView.setController(controller);
  }

  @Nullable
  private static MediaVariations getMediaVariationsForMode(Mode mode) {
    switch (mode) {
      case LISTED_IN_REQUEST:
        MediaVariations.Builder builder = MediaVariations.newBuilderForMediaId(null);
        for (Size size : Size.values()) {
          builder.addVariant(size.uri, size.width, size.height);
        }
        return builder.build();
      case MEDIA_ID_IN_REQUEST:
        return MediaVariations.forMediaId(MEDIA_ID);
    }
    throw new IllegalStateException("Invalid media variations mode set");
  }
}
