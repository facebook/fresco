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

package com.facebook.samples.mediavariations;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.MediaVariations;
import com.facebook.samples.mediavariations.widget.ResizableFrameLayout;

public class MainActivity extends Activity {
  private static final String URI_TEMPLATE
      = "http://frescolib.org/static/sample-images/monkey-selfie-%s.jpg";
  private static final String MEDIA_ID = "monkey-selfie";

  private enum Size {
    XS(R.id.thumb_xs, "xs", 377, 523),
    S(R.id.thumb_s, "s", 629, 871),
    M(R.id.thumb_m, "m", 1048, 1451),
    L(R.id.thumb_l, "l", 1747, 2418),
    XL(R.id.thumb_xl, "xl", 2912, 4030);

    final @IdRes int thumbViewId;
    final String name;
    final Uri uri;
    final int width;
    final int height;

    Size(@IdRes int thumbViewId, String name, int width, int height) {
      this.thumbViewId = thumbViewId;
      this.name = name;
      this.uri = Uri.parse(String.format(URI_TEMPLATE, name));
      this.width = width;
      this.height = height;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    populateImagesAfterInitialLayout();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.clear_cache) {
      Fresco.getImagePipeline().clearCaches();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        recreate();
      } else {
        Toast.makeText(this, R.string.restart_toast, Toast.LENGTH_SHORT).show();
        finish();
      }
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void populateImagesAfterInitialLayout() {
    final View rootView = findViewById(R.id.layout_root);
    rootView.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            populateMainImage();
            for (Size size : Size.values()) {
              populateThumb(size.thumbViewId, size);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
              rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
              rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
          }
        });
  }

  private void populateThumb(@IdRes int viewId, final Size size) {
    final SimpleDraweeView draweeView = (SimpleDraweeView) findViewById(viewId);

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
        .build();
    DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setImageRequest(request)
        .setOldController(draweeView.getController())
        .build();
    draweeView.setController(controller);
  }

  private void populateMainImage() {
    final SimpleDraweeView draweeView = (SimpleDraweeView) findViewById(R.id.img_main);
    loadMainImage(draweeView);

    draweeView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        loadMainImage(draweeView);
      }
    });

    ResizableFrameLayout mainImageFrameLayout =
        (ResizableFrameLayout) findViewById(R.id.frame_main);
    mainImageFrameLayout.init(findViewById(R.id.btn_resize));
  }

  private void loadMainImage(SimpleDraweeView draweeView) {
    MediaVariations.Builder variationsBuilder = MediaVariations.newBuilderForMediaId(MEDIA_ID);

    // Request a non-existent image to force fallback to the variations
    Uri uri = Uri.parse(String.format(URI_TEMPLATE, "full"));
    ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
        .setMediaVariations(variationsBuilder.build())
        .setResizeOptions(new ResizeOptions(draweeView.getWidth(), draweeView.getHeight()))
        .build();
    DraweeController controller = Fresco.newDraweeControllerBuilder()
        .setImageRequest(request)
        .setOldController(draweeView.getController())
        .setRetainImageOnFailure(true)
        .build();
    draweeView.setController(controller);
  }
}
