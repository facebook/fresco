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
package com.facebook.fresco.samples.showcase.imageformat.color;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;

/**
 * Color XML example. It has a toggle to enable / disable Color XML support and displays 1 image.
 *
 * The supported XML color format is: <color>#rrggbb</color>
 */
public class ImageFormatColorFragment extends BaseShowcaseFragment {

  private SimpleDraweeView mSimpleDraweeView1;
  private SimpleDraweeView mSimpleDraweeView2;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_color, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mSimpleDraweeView1 = (SimpleDraweeView) view.findViewById(R.id.drawee1);
    mSimpleDraweeView2 = (SimpleDraweeView) view.findViewById(R.id.drawee2);


    // Set a simple custom color resource as the image.
    // The format of custom_color1 is <color>#rrggbb</color>
    mSimpleDraweeView1.setImageURI(UriUtil.getUriForResourceId(R.raw.custom_color1));
    mSimpleDraweeView2.setImageURI(UriUtil.getUriForResourceId(R.raw.custom_color2));

    final SwitchCompat switchBackground = (SwitchCompat) view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mSimpleDraweeView1.getHierarchy().setBackgroundImage(isChecked
            ? new CheckerBoardDrawable(getResources())
            : null);
        mSimpleDraweeView2.getHierarchy().setBackgroundImage(isChecked
            ? new CheckerBoardDrawable(getResources())
            : null);
      }
    });

    SwitchCompat switchCompat = (SwitchCompat) view.findViewById(R.id.decoder_switch);
    switchCompat.setChecked(CustomImageFormatConfigurator.isGlobalColorDecoderEnabled(getContext()));
    switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CustomImageFormatConfigurator.setGlobalColorDecoderEnabled(getContext(), isChecked);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.format_color_title;
  }
}
