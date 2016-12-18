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
package com.facebook.fresco.samples.showcase.imageformat.svg;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;

/**
 * SVG example. It has a toggle to enable / disable SVG support and displays 1 image.
 */
public class ImageFormatSvgFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_svg, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    simpleDraweeView
        .setImageURI("https://upload.wikimedia.org/wikipedia/commons/0/02/SVG_logo.svg");

    SwitchCompat switchCompat = (SwitchCompat) view.findViewById(R.id.decoder_switch);
    switchCompat.setChecked(CustomImageFormatConfigurator.isSvgEnabled(getContext()));
    switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CustomImageFormatConfigurator.setSvgEnabled(getContext(), isChecked);
      }
    });
  }

  @Override
  public int getTitleId() {
    return R.string.format_svg_title;
  }
}
