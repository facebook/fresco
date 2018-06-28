package com.facebook.fresco.samples.showcase.imageformat.webplist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.imageformat.webplist.controller.SwitchController;
import com.facebook.fresco.samples.showcase.imageformat.webplist.controller.WebpListController;

/**
 * This Fragment is used to display a large number of webp pictures in a list
 * will appear to be stuck while sliding.
 */
public class ImageFormatWebpListFragment extends BaseShowcaseFragment implements
        SwitchController.OnEnableChangeListener {

  private WebpListController mListController;

  @Nullable
  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater,
          @Nullable ViewGroup container,
          @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.framgent_format_webp_list,
            container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    mListController = new WebpListController((RecyclerView) view.findViewById(R.id.rcy_webp_list));
    SwitchController dropFrameController = new SwitchController(
            (SwitchCompat) view.findViewById(R.id.sw_enable_drop_frame));

    mListController.bindData();
    dropFrameController.setDropChangeListener(this);
  }

  @Override
  public int getTitleId() {
    return R.string.format_webp_list_title;
  }

  @Override
  public void onDropChanged(boolean enable) {
    if (mListController != null) {
      mListController.onDropChanged(enable);
    }
  }
}
