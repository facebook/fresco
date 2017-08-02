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

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Fragment that illustrates how to use the image pipeline directly in order to create
 * notifications.
 */
public class ImagePipelineResizingFragment extends BaseShowcaseFragment {

  private final Entry[] SPINNER_ENTRIES = new Entry[]{
      new Entry(null),
      new Entry(ResizeOptions.forDimensions(2560, 1440)),
      new Entry(ResizeOptions.forDimensions(1920, 1080)),
      new Entry(ResizeOptions.forDimensions(1200, 1200)),
      new Entry(ResizeOptions.forDimensions(720, 1280)),
      new Entry(ResizeOptions.forSquareSize(800)),
      new Entry(ResizeOptions.forDimensions(800, 600)),
      new Entry(ResizeOptions.forSquareSize(480)),
      new Entry(ResizeOptions.forDimensions(320, 240)),
      new Entry(ResizeOptions.forDimensions(240, 320)),
      new Entry(ResizeOptions.forDimensions(160, 90)),
      new Entry(ResizeOptions.forSquareSize(100)),
      new Entry(ResizeOptions.forSquareSize(64)),
      new Entry(ResizeOptions.forSquareSize(16)),
  };

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
    return inflater.inflate(R.layout.fragment_imagepipeline_resizing, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageUriProvider imageUriProvider = ImageUriProvider.getInstance(getContext());
    mUri = imageUriProvider.createSampleUri(ImageUriProvider.ImageSize.L);

    mButton = (Button) view.findViewById(R.id.button);
    mDraweeMain = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mSpinner = (Spinner) view.findViewById(R.id.spinner);

    mSpinner.setAdapter(new SimpleResizeOptionsAdapter());
    mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        reloadImageUsingResizeOptions(SPINNER_ENTRIES[position].resizeOptions);
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    mSpinner.setSelection(0);

    mButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        reloadImageUsingResizeOptions(
            SPINNER_ENTRIES[mSpinner.getSelectedItemPosition()].resizeOptions);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_resizing_title;
  }

  private void reloadImageUsingResizeOptions(@Nullable ResizeOptions resizeOptions) {
    final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(mUri)
        .setResizeOptions(resizeOptions)
        .build();

    final DraweeController draweeController = Fresco.newDraweeControllerBuilder()
        .setOldController(mDraweeMain.getController())
        .setImageRequest(imageRequest)
        .build();

    mDraweeMain.setController(draweeController);
  }

  private class SimpleResizeOptionsAdapter extends BaseAdapter {

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
      final LayoutInflater layoutInflater = getLayoutInflater();

      final View view = convertView != null
          ? convertView
          : layoutInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);

      final TextView textView = (TextView) view.findViewById(android.R.id.text1);
      textView.setText(SPINNER_ENTRIES[position].toString());

      return view;
    }
  }

  private class Entry {

    final @Nullable ResizeOptions resizeOptions;

    Entry(@Nullable ResizeOptions resizeOptions) {
      this.resizeOptions = resizeOptions;
    }

    @Override
    public String toString() {
      return resizeOptions == null
          ? getString(R.string.imagepipeline_resizing_disabled)
          : resizeOptions.toString();
    }
  }
}
