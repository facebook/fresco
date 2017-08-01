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
package com.facebook.fresco.samples.showcase.imageformat.datauri;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;

/**
 * Fragment using a SimpleDraweeView to display a Keyframes animation
 */
public class ImageFormatDataUriFragment extends BaseShowcaseFragment {

  /**
   * A 100x100px PNG image with a blue star
   */
  public static final String dataUri =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGUAAABkCAIAAAAQQmk9AAAD2ElEQVR42u2cvW/cMAzF" +
          "+T927t49c/fu3bN3zp6hQ4EbMhRIhg5BOmRIgQBN02u+2/QBAozL2WfT0pNFKRSIwPAltvwL70kkJcuztzlNHI" +
          "Hzcl7Oy3k5r3Lt+OrReWnbj9u/bz7/xE/npWofv/0BL/x0XtNt/fgPsILh2HlNtE/fbzteOHZeE8719stVxwvH" +
          "1lzMFq+D87sOVjCccV4727vVry1eOOO8htvhxf0WrGA477xUzmXQxazwWl0+DMIKhk+d14v2/uvvEV741Hm9iB" +
          "ZHYAUzElGKfecy5WLleYXoWmMWIvDyvEJ0rTELEbjU4lxGXExqcS4jLlaS11Z0rbHiEXhJXpupG72VTfJIRc5l" +
          "wcWK8doVXWusYARejNeu6FpjBSNwqc65yrqYVOdcZV2sAC9NdK2xIhF4AV6a6FpjRSJwqdS5SrnY0rw+nKyJvH" +
          "C1BnmdrZ/gCAfnd3ET+snpPq6M6+MulfFCp2F4ABjEBRY3g08x3DHcOnQjdKkkL8Qi6MHq8mGTy8JQoseHjiP6" +
          "j6eIiKtkJDPVcdk/vcGd9o6uq+Ay1/BceDo8Y8dxJMsm/ZxB+mSyDQOHfi5E+l+3Vv0owu/6X1gZVKhaJCmr2A" +
          "2qm7AyxS3ZSNZbiMn15mFNzydeG7LJesr0/Cs9V1WLaXJqqvnqa0CmTEBq5/dtI9Nna2fEQ5j7Lh8PLhBvzlpc" +
          "JnMzDS0hw7PMzWrMjrebQRYBKzKfg9vUHmOi/3H5ssj8V9Vh5mBgmD1fWCmyFFip+dXqkCXCIuSjcftaYib0M3" +
          "2hCid/bx8Za6Edrd5hGRlxVSKzPpSjXEYpuNmtP1oLM+nLePj1WjvIcqx5ylLf3j+9KQ4LfahmPYCFckmm1TtZ" +
          "eFkIyNGHOnjN3bKRz3JsBuHzGt/5uaTl2GXK52VnFpZjZ4M0Kfb5JF+aFPt8kk/mtfn2GwtG3zlD5sVdzmtwQb" +
          "C0KvaZJJ/Mi7v82eACajIva3Uj+rYZaVjsc0i+NCz2OSTf86vleLHEfu/o+vDinlWp40q+mBJ7XGEzKYpjazsl" +
          "mbwSYxd8cfrajDM4nxhjWeSVIvaTldTEqjBR8mm8+q9qVKYQ9Fk9/GZc8oP40shi9VrIedy/HX81dyiwWK/VP8" +
          "OWqEdX7fRDAfpmjleKqEeHE/qhwBYvjdhTlsdEDwUsyZcFxH6WqEcXpcaHApbk513PFC3q0W6+S0ZtrWfq95Ii" +
          "6sShgCX5HF75RJ04FFjhdbZ+yi3qlKGA8sIFofj/MqKeOBRQ9IHAK+ypf7bdwrsOzOXvm2/Oy3k5L+flvJyXt3" +
          "77Dzs5iMw8JSAYAAAAAElFTkSuQmCC";

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_datauri, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final SimpleDraweeView simpleDraweeView = view.findViewById(R.id.drawee_view);
    simpleDraweeView.setImageURI(dataUri, null);

    final TextView uriContentTextView = view.findViewById(R.id.datauri_content_textview);
    uriContentTextView.setText(dataUri);
  }

  @Override
  public int getTitleId() {
    return R.string.format_datauri_title;
  }
}
