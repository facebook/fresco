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
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;

/**
 * Simple drawee fragment that just displays an image.
 */
public class ImagePipelineQualifiedResourceFragment extends BaseShowcaseFragment {

  /**
   * This package name can pointing to another module that you include, but that does not share
   * the same "R" file as your main application.
   */
  private static final String PACKAGE_NAME = "com.facebook.fresco.samples.showcase";
  private static final int RESOURCE_ID = R.drawable.logo;

  private static final Uri QUALIFIED_RESOURCE_URI = UriUtil.getUriForQualifiedResource(
      PACKAGE_NAME,
      RESOURCE_ID);

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_qualified_resource, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    simpleDraweeView.setImageURI(QUALIFIED_RESOURCE_URI);
  }

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_qualified_resource_title;
  }
}
