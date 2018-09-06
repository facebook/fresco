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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.postprocessor.BenchmarkPostprocessorForDuplicatedBitmap;
import com.facebook.fresco.samples.showcase.postprocessor.BenchmarkPostprocessorForDuplicatedBitmapInPlace;
import com.facebook.fresco.samples.showcase.postprocessor.BenchmarkPostprocessorForManualBitmapHandling;
import com.facebook.fresco.samples.showcase.postprocessor.CachedWatermarkPostprocessor;
import com.facebook.fresco.samples.showcase.postprocessor.FasterGreyScalePostprocessor;
import com.facebook.fresco.samples.showcase.postprocessor.ScalingBlurPostprocessor;
import com.facebook.fresco.samples.showcase.postprocessor.SlowGreyScalePostprocessor;
import com.facebook.fresco.samples.showcase.postprocessor.WatermarkPostprocessor;
import com.facebook.imagepipeline.postprocessors.BlurPostProcessor;
import com.facebook.imagepipeline.postprocessors.IterativeBoxBlurPostProcessor;
import com.facebook.imagepipeline.postprocessors.RoundAsCirclePostprocessor;
import com.facebook.imagepipeline.postprocessors.RoundPostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fragment that illustrates how to use the image pipeline directly in order to create
 * notifications.
 */
public class ImagePipelinePostProcessorFragment extends BaseShowcaseFragment
    implements DurationCallback {

  private static final int WATERMARK_COUNT = 10;
  private static final String WATERMARK_STRING = "WATERMARK";

  private List<Entry> mSpinnerEntries = new ArrayList<>();

  private Button mButton;
  private SimpleDraweeView mDraweeMain;
  private Spinner mSpinner;
  private Uri mUri;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_postprocessor, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    mSpinnerEntries = getSpinnerItems();
    mUri = imageUriProvider.createSampleUri(ImageUriProvider.ImageSize.L);

    mButton = (Button) view.findViewById(R.id.button);
    mDraweeMain = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mSpinner = (Spinner) view.findViewById(R.id.spinner);

    mSpinner.setAdapter(new SimplePostprocessorAdapter());
    mSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final Entry spinnerEntry = mSpinnerEntries.get(position);
            setPostprocessor(spinnerEntry.postprocessor);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    mSpinner.setSelection(0);

    mButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            final Entry spinnerEntry = mSpinnerEntries.get(mSpinner.getSelectedItemPosition());
            setPostprocessor(spinnerEntry.postprocessor);
          }
        });
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_postprocessor_title;
  }

  @Override
  public void showDuration(long startNs) {
    final float deltaMs = startNs / 1e6f;
    final String message = String.format((Locale) null, "Duration: %.1f ms", deltaMs);
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void setPostprocessor(Postprocessor postprocessor) {
    final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(mUri)
        .setPostprocessor(postprocessor)
        .build();

    final DraweeController draweeController = Fresco.newDraweeControllerBuilder()
        .setOldController(mDraweeMain.getController())
        .setImageRequest(imageRequest)
        .build();

    mDraweeMain.setController(draweeController);
  }

  private class SimplePostprocessorAdapter extends BaseAdapter {

    @Override
    public int getCount() {
      return mSpinnerEntries.size();
    }

    @Override
    public Object getItem(int position) {
      return mSpinnerEntries.get(position);
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
      textView.setText(mSpinnerEntries.get(position).descriptionId);

      return view;
    }
  }

  private List<Entry> getSpinnerItems() {
    return Arrays.asList(
        new Entry(R.string.imagepipeline_postprocessor_show_original, null),
        new Entry(
            R.string.imagepipeline_postprocessor_set_greyscale_slow,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new SlowGreyScalePostprocessor())),
        new Entry(
            R.string.imagepipeline_postprocessor_set_greyscale,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new FasterGreyScalePostprocessor())),
        new Entry(
            R.string.imagepipeline_postprocessor_set_watermark,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new WatermarkPostprocessor(WATERMARK_COUNT, WATERMARK_STRING))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_watermark_cached,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new CachedWatermarkPostprocessor(WATERMARK_COUNT, WATERMARK_STRING))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_native_blur,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new IterativeBoxBlurPostProcessor(25, 3))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_blur,
            new BenchmarkPostprocessorForDuplicatedBitmap(
                this, new BlurPostProcessor(20, getContext()))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_scaling_blur,
            new BenchmarkPostprocessorForManualBitmapHandling(
                this, new ScalingBlurPostprocessor(25, 3, 4))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_native_round_as_circle,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new RoundAsCirclePostprocessor(false))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_round_as_aa_circle,
            new BenchmarkPostprocessorForDuplicatedBitmapInPlace(
                this, new RoundAsCirclePostprocessor(true))),
        new Entry(
            R.string.imagepipeline_postprocessor_set_round_as_circle,
            new BenchmarkPostprocessorForDuplicatedBitmap(this, new RoundPostprocessor())));
  }

  private static class Entry {

    final int descriptionId;
    final Postprocessor postprocessor;

    Entry(int descriptionId, Postprocessor postprocessor) {
      this.descriptionId = descriptionId;
      this.postprocessor = postprocessor;
    }
  }
}
