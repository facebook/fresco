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
package com.facebook.fresco.samples.showcase.imageformat.pjpeg;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ProgressBarDrawable;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.LogcatImagePerfDataListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Progressive JPEG example that logs which frames of a progressive JPEG are rendered
 */
public class ImageFormatProgressiveJpegFragment extends BaseShowcaseFragment {

  public static final Uri URI_PJPEG_LARGE =
      Uri.parse("http://frescolib.org/static/sample-images/animal_c_l.jpg");
  public static final Uri URI_PJPEG_MEDIUM =
      Uri.parse("http://frescolib.org/static/sample-images/animal_d_m.jpg");
  public static final Uri URI_PJPEG_SMALL =
      Uri.parse("http://frescolib.org/static/sample-images/animal_e_s.jpg");
  public static final Uri URI_PJPEG_SLOW =
      Uri.parse("http://pooyak.com/p/progjpeg/jpegload.cgi?o=1");

  private static final Entry[] SPINNER_ENTRIES = new Entry[]{
      new Entry(R.string.format_pjpeg_label_small, URI_PJPEG_SMALL),
      new Entry(R.string.format_pjpeg_label_medium, URI_PJPEG_MEDIUM),
      new Entry(R.string.format_pjpeg_label_large, URI_PJPEG_LARGE),
      new Entry(R.string.format_pjpeg_label_slow, URI_PJPEG_SLOW),
  };

  private final DateFormat mDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
  private final ImagePerfDataListener mImagePerfDataListener = new LogcatImagePerfDataListener();

  private SimpleDraweeView mSimpleDraweeView;
  private boolean mProgressiveRenderingEnabled;
  private TextView mDebugOutput;
  private ScrollView mDebugOutputScrollView;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_progressive_jpeg, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    ProgressBarDrawable progressBarDrawable = new ProgressBarDrawable();
    progressBarDrawable.setColor(getResources().getColor(R.color.progress_bar_color));
    progressBarDrawable.setBackgroundColor(
        getResources().getColor(R.color.progress_bar_background));

    mSimpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mSimpleDraweeView.getHierarchy().setProgressBarImage(progressBarDrawable);

    mDebugOutput = (TextView) view.findViewById(R.id.debug_output);
    mDebugOutputScrollView = (ScrollView) view.findViewById(R.id.debug_output_scroll_view);

    final SwitchCompat switchProgressiveRenderingEnabled =
        (SwitchCompat) view.findViewById(R.id.switch_progressive_enabled);
    switchProgressiveRenderingEnabled.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mProgressiveRenderingEnabled = isChecked;
          }
        });

    mProgressiveRenderingEnabled = switchProgressiveRenderingEnabled.isChecked();

    final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
    spinner.setAdapter(new SimpleUriListAdapter());
    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final Entry spinnerEntry = SPINNER_ENTRIES[spinner.getSelectedItemPosition()];
        setImageUri(spinnerEntry.uri);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    spinner.setSelection(0);
  }

  private void setImageUri(Uri uri) {
    mDebugOutput.setText("");
    ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
        .setProgressiveRenderingEnabled(mProgressiveRenderingEnabled)
        .build();
    DraweeController controller =
        Fresco.newDraweeControllerBuilder()
            .setImageRequest(request)
            .setRetainImageOnFailure(true)
            .setPerfDataListener(mImagePerfDataListener)
            .setControllerListener(
                new BaseControllerListener<ImageInfo>() {
                  @Override
                  public void onFinalImageSet(
                      String id,
                      @javax.annotation.Nullable ImageInfo imageInfo,
                      @javax.annotation.Nullable Animatable animatable) {
                    if (imageInfo != null) {
                      QualityInfo qualityInfo = imageInfo.getQualityInfo();
                      logScan(qualityInfo, true);
                    }
                  }

                  @Override
                  public void onIntermediateImageSet(
                      String id, @javax.annotation.Nullable ImageInfo imageInfo) {
                    if (imageInfo != null) {
                      QualityInfo qualityInfo = imageInfo.getQualityInfo();
                      logScan(qualityInfo, false);
                    }
                  }

                  @Override
                  public void onIntermediateImageFailed(String id, Throwable throwable) {
                    mDebugOutput.append(
                        String.format(
                            Locale.getDefault(),
                            "onIntermediateImageFailed, %s\n",
                            throwable.getMessage()));
                  }
                })
            .build();
    mSimpleDraweeView.setController(controller);
  }

  private void logScan(QualityInfo qualityInfo, boolean isFinalImage) {
    mDebugOutput.append(
        String.format(
            Locale.getDefault(),
            "%s: %s, goodEnough=%b, fullQuality=%b, quality=%d\n\n",
            mDateFormat.format(new Date(System.currentTimeMillis())),
            isFinalImage ? "final" : "intermediate",
            qualityInfo.isOfGoodEnoughQuality(),
            qualityInfo.isOfFullQuality(),
            qualityInfo.getQuality()));
    // Scroll to the bottom
    mDebugOutputScrollView.post(new Runnable() {
      @Override
      public void run() {
        mDebugOutputScrollView.scrollTo(0, mDebugOutputScrollView.getBottom());
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.format_pjpeg_title;
  }

  private class SimpleUriListAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return SPINNER_ENTRIES.length;
    }

    @Override
    public Entry getItem(int position) {
      return SPINNER_ENTRIES[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final LayoutInflater layoutInflater = getLayoutInflater(null);

      final View view = convertView != null
          ? convertView
          : layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

      final TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(SPINNER_ENTRIES[position].descriptionId);

      return view;
    }
  }

  private static class Entry {

    final int descriptionId;
    final Uri uri;

    private Entry(int descriptionId, Uri uri) {
      this.descriptionId = descriptionId;
      this.uri = uri;
    }
  }
}
