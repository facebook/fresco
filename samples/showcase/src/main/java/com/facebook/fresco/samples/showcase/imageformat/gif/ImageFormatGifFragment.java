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
package com.facebook.fresco.samples.showcase.imageformat.gif;

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
import android.widget.Spinner;
import android.widget.TextView;
import com.facebook.animated.giflite.GifDecoder;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * GIF example that illustrates how to display a simple GIF file
 */
public class ImageFormatGifFragment extends BaseShowcaseFragment {

  public static final Uri URI_GIF_S =
      Uri.parse("http://frescolib.org/static/sample-images/fresco_logo_anim_full_frames_with_pause_s.gif");
  public static final Uri URI_GIF_M =
      Uri.parse("http://frescolib.org/static/sample-images/fresco_logo_anim_full_frames_with_pause_m.gif");
  public static final Uri URI_GIF_L =
      Uri.parse("http://frescolib.org/static/sample-images/fresco_logo_anim_full_frames_with_pause_l.gif");

  private static final Entry[] SPINNER_ENTRIES = new Entry[]{
      new Entry(R.string.format_gif_label_small, URI_GIF_S),
      new Entry(R.string.format_gif_label_medium, URI_GIF_M),
      new Entry(R.string.format_gif_label_large, URI_GIF_L),
  };

  private Spinner mSpinner;
  private SimpleDraweeView mSimpleDraweeView;
  private @Nullable GifDecoder mGifDecoder;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_gif, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

    mSimpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);

    final SwitchCompat switchBackground = (SwitchCompat) view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSimpleDraweeView.getHierarchy().setBackgroundImage(isChecked
            ? new CheckerBoardDrawable(getResources())
            : null);
      }
    });

    final SwitchCompat switchAspect = (SwitchCompat) view.findViewById(R.id.switch_aspect_ratio);
    switchAspect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        ViewGroup.LayoutParams layoutParams = mSimpleDraweeView.getLayoutParams();
        layoutParams.height = layoutParams.width * (isChecked ? 2 : 1);
        mSimpleDraweeView.setLayoutParams(layoutParams);
      }
    });

    mSpinner = (Spinner) view.findViewById(R.id.spinner);
    mSpinner.setAdapter(new SimpleUriListAdapter());
    mSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            refreshAnimation();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    mSpinner.setSelection(0);

    final Spinner decoderSpinner = (Spinner) view.findViewById(R.id.spinner_select_decoder);
    decoderSpinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
              case 0:
                mGifDecoder = null;
                break;
              case 1:
                mGifDecoder = new GifDecoder();
                break;
              default:
                throw new IllegalArgumentException("Unknown decoder selected");
            }
            refreshAnimation();
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
    decoderSpinner.setSelection(0);
  }

  private void refreshAnimation() {
    final Entry spinnerEntry = SPINNER_ENTRIES[mSpinner.getSelectedItemPosition()];
    setAnimationUri(spinnerEntry.uri);
  }

  private void setAnimationUri(Uri uri) {
    final PipelineDraweeControllerBuilder controllerBuilder =
        Fresco.newDraweeControllerBuilder()
            .setAutoPlayAnimations(true)
            .setOldController(mSimpleDraweeView.getController());
    if (mGifDecoder != null) {
      controllerBuilder.setImageRequest(
          ImageRequestBuilder.newBuilderWithSource(uri)
              .setImageDecodeOptions(
                  ImageDecodeOptions.newBuilder().setCustomImageDecoder(mGifDecoder).build())
              .build());
    } else {
      controllerBuilder.setUri(uri).build();
    }
    mSimpleDraweeView.setController(controllerBuilder.build());
  }

  @Override
  public int getTitleId() {
    return R.string.format_gif_title;
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
