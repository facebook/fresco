/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.common.ToggleAnimationClickListener;
import com.facebook.fresco.samples.showcase.permissions.StoragePermissionHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/** Display images from media pickers. */
public class DraweeMediaPickerFragment extends BaseShowcaseFragment {

  private static final int REQUEST_CODE_PICK_MEDIA = 1;

  private SimpleDraweeView mSimpleDraweeView;
  private TextView mImagePath;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_media_picker, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mSimpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mImagePath = (TextView) view.findViewById(R.id.image_path);

    mSimpleDraweeView.setOnClickListener(new ToggleAnimationClickListener(mSimpleDraweeView));

    View actionOpenDocumentButton = view.findViewById(R.id.pick_action_open_document);
    actionOpenDocumentButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
              Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
              intent.addCategory(Intent.CATEGORY_OPENABLE);
              intent.setType("image/*");
              startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA);
            } else {
              Toast.makeText(
                      getContext(),
                      R.string.drawee_media_picker_action_open_document_not_supported,
                      Toast.LENGTH_SHORT)
                  .show();
            }
          }
        });

    View actionGetContent = view.findViewById(R.id.pick_action_get_content);
    actionGetContent.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA);
          }
        });

    View actionPickButton = view.findViewById(R.id.pick_action_pick);
    actionPickButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            StoragePermissionHelper.INSTANCE.withStoragePermission(
                getActivity(),
                new Function1<Unit, Unit>() {
                  @Override
                  public Unit invoke(Unit unit) {
                    Intent intent =
                        new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA);
                    return null;
                  }
                });
          }
        });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_PICK_MEDIA) {
      if (resultCode != Activity.RESULT_OK) {
        mSimpleDraweeView.setImageURI((Uri) null);
        mImagePath.setText(R.string.drawee_media_picker_no_image);
      } else {
        mSimpleDraweeView.setImageURI(data.getData());
        mImagePath.setText(data.getDataString());
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
