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

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Fragment that illustrates how to use the image pipeline directly in order to create
 * notifications.
 */
public class ImagePipelineNotificationFragment extends BaseShowcaseFragment {

  private static final int NOTIFICATION_ID = 1;
  private static final Uri URI =
      Uri.parse("http://frescolib.org/static/sample-images/animal_b_s.jpg");

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_notification, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final Button button = (Button) view.findViewById(R.id.button);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        createNotification();
      }
    });
  }

  private void createNotification() {
    final ImagePipeline imagePipeline = Fresco.getImagePipeline();
    final ImageRequest imageRequest = ImageRequest.fromUri(URI);

    final DataSource<CloseableReference<CloseableImage>> dataSource =
        imagePipeline.fetchDecodedImage(imageRequest, null);

    dataSource.subscribe(
        new BaseBitmapDataSubscriber() {

          @Override
          protected void onNewResultImpl(Bitmap bitmap) {
            displayNotification(bitmap);
          }

          @Override
          protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
            showToastText("Failed to fetch image directly: " + dataSource.getFailureCause());

            // In general, failing to fetch the image should not keep us from displaying the
            // notification. We proceed without the bitmap.
            displayNotification(null);
          }
        },
        UiThreadImmediateExecutorService.getInstance());
  }

  private void displayNotification(@Nullable Bitmap bitmap) {
    final NotificationCompat.Builder notificationBuilder =
        new NotificationCompat.Builder(getContext())
            .setSmallIcon(R.drawable.ic_done)
            .setLargeIcon(bitmap)
            .setContentTitle(getString(R.string.imagepipeline_notification_content_title))
            .setContentText(getString(R.string.imagepipeline_notification_content_text));

    final NotificationManager notificationManager =
        (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
  }

  private void showToastText(String text) {
    Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_notification_title;
  }
}
