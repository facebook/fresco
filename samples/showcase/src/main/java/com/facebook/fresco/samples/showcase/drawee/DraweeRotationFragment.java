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
package com.facebook.fresco.samples.showcase.drawee;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Simple drawee fragment that just displays an image.
 */
public class DraweeRotationFragment extends BaseShowcaseFragment {

  private SimpleDraweeView mSimpleDraweeView;
  private Uri mUri;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_rotation, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    mUri = imageUriProvider.createSampleUri(ImageUriProvider.ImageSize.M);

    mSimpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);

    final SimpleRotationOptionsAdapter adapter = new SimpleRotationOptionsAdapter();

    spinner.setAdapter(adapter);
    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final SimpleRotationOptionsAdapter.Entry spinnerEntry =
            (SimpleRotationOptionsAdapter.Entry) adapter.getItem(position);
        setRotationOptions(spinnerEntry.rotationOptions);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    spinner.setSelection(0);
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_rotation_title;
  }

  private void setRotationOptions(RotationOptions rotationOptions) {
    ImageRequest imageRequest =
        ImageRequestBuilder.newBuilderWithSource(mUri)
            .setRotationOptions(rotationOptions)
            .setImageDecodeOptions(new ImageDecodeOptionsBuilder().setTransformToSRGB(true).build())
            .build();
    mSimpleDraweeView.setImageRequest(imageRequest);
  }

  public class SimpleRotationOptionsAdapter extends BaseAdapter {

    private final Entry[] SPINNER_ENTRIES = new Entry[]{
        new Entry(RotationOptions.disableRotation(), "disableRotation"),
        new Entry(RotationOptions.autoRotate(), "autoRotate"),
        new Entry(RotationOptions.autoRotateAtRenderTime(), "autoRotateAtRenderTime"),
        new Entry(RotationOptions.forceRotation(RotationOptions.NO_ROTATION), "NO_ROTATION"),
        new Entry(RotationOptions.forceRotation(RotationOptions.ROTATE_90), "ROTATE_90"),
        new Entry(RotationOptions.forceRotation(RotationOptions.ROTATE_180), "ROTATE_180"),
        new Entry(RotationOptions.forceRotation(RotationOptions.ROTATE_270), "ROTATE_270"),
    };

    @Override
    public int getCount() {
      return SPINNER_ENTRIES.length;
    }

    @Override
    public Object getItem(int position) {
      return SPINNER_ENTRIES[position];
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

      final View view = convertView != null
          ? convertView
          : layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

      final TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(SPINNER_ENTRIES[position].description);

      return view;
    }

    public class Entry {

      final RotationOptions rotationOptions;
      final String description;

      private Entry(
          RotationOptions rotationOptions,
          String description) {
        this.rotationOptions = rotationOptions;
        this.description = description;
      }
    }
  }
}
