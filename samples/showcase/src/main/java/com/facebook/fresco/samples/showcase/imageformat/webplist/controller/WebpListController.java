package com.facebook.fresco.samples.showcase.imageformat.webplist.controller;

import android.support.v7.widget.RecyclerView;

import com.facebook.fresco.samples.showcase.imageformat.webplist.adapter.WebpAdapter;
import com.facebook.fresco.samples.showcase.imageformat.webplist.utils.RecyclerListUtil;

public class WebpListController {

  private RecyclerView mWebpListRcy;

  public WebpListController(RecyclerView recyclerView) {
    mWebpListRcy = recyclerView;
  }

  public void bindData() {
    RecyclerListUtil.bindData(mWebpListRcy, new WebpAdapter());
  }

  public void onDropChanged(boolean enableDropped) {
    WebpAdapter adapter = new WebpAdapter();
    adapter.setEnableDropFrame(enableDropped);
    RecyclerListUtil.bindData(mWebpListRcy, adapter);
  }

}
