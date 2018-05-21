package com.facebook.fresco.samples.showcase.imageformat.webplist.utils;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

public class RecyclerListUtil {

  public static void bindData(RecyclerView view, RecyclerView.Adapter adapter) {
    view.setHasFixedSize(true);
    view.setItemAnimator(null);
    view.setLayoutManager(new StaggeredGridLayoutManager(4,
            RecyclerView.VERTICAL));
    view.setAdapter(adapter);
  }

}
