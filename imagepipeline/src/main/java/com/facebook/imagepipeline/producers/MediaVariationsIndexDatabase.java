/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.CacheKeyUtil;
import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.MediaVariations;

import bolts.Task;

public class MediaVariationsIndexDatabase implements MediaVariationsIndex {
  private static final String TAG = MediaVariationsIndexDatabase.class.getSimpleName();

  private static final String[] PROJECTION = {
      IndexEntry.COLUMN_NAME_CACHE_KEY,
      IndexEntry.COLUMN_NAME_WIDTH,
      IndexEntry.COLUMN_NAME_HEIGHT
  };

  private static final String SQL_DELETE_ENTRIES =
      "DROP TABLE IF EXISTS " + IndexEntry.TABLE_NAME;

  private final LazyIndexDbOpenHelper mDbHelper;
  private final Executor mReadExecutor;
  private final Executor mWriteExecutor;

  public MediaVariationsIndexDatabase(
      Context context,
      Executor readExecutor,
      Executor writeExecutor) {
    mDbHelper = new LazyIndexDbOpenHelper(context);
    mReadExecutor = readExecutor;
    mWriteExecutor = writeExecutor;
  }

  @Override
  public Task<List<MediaVariations.Variant>> getCachedVariants(final String mediaId) {
    try {
      return Task.call(
          new Callable<List<MediaVariations.Variant>>() {
            @Override
            public List<MediaVariations.Variant> call() throws Exception {
              return getCachedVariantsSync(mediaId);
            }
          },
          mReadExecutor);
    } catch (Exception exception) {
      FLog.w(TAG, exception, "Failed to schedule query task for %s", mediaId);
      return Task.forError(exception);
    }
  }

  private synchronized List<MediaVariations.Variant> getCachedVariantsSync(String mediaId) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    Cursor c = null;
    try {
      String selection = IndexEntry.COLUMN_NAME_MEDIA_ID + " = ?";
      String[] selectionArgs = {mediaId};

      c = db.query(
          IndexEntry.TABLE_NAME,
          PROJECTION,
          selection,
          selectionArgs,
          null, // groupBy
          null, // having
          null); // orderBy

      if (c.getCount() == 0) {
        return null;
      }

      final int columnIndexCacheKey =
          c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_CACHE_KEY);
      final int columnIndexWidth = c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_WIDTH);
      final int columnIndexHeight =
          c.getColumnIndexOrThrow(IndexEntry.COLUMN_NAME_HEIGHT);

      List<MediaVariations.Variant> variants = new ArrayList<>(c.getCount());
      while (c.moveToNext()) {
        variants.add(new MediaVariations.Variant(
            Uri.parse(c.getString(columnIndexCacheKey)),
            c.getInt(columnIndexWidth),
            c.getInt(columnIndexHeight)
        ));
      }

      return variants;
    } catch (SQLException x) {
      FLog.e(TAG, x, "Error reading for %s", mediaId);
      throw x;
    } finally {
      if (c != null) {
        c.close();
      }
      db.close();
    }
  }

  @Override
  public void saveCachedVariant(
      final String mediaId,
      final CacheKey cacheKey,
      final EncodedImage encodedImage) {
    mWriteExecutor.execute(new Runnable() {
      @Override
      public void run() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
          db.beginTransaction();

          ContentValues contentValues = new ContentValues();
          contentValues.put(IndexEntry.COLUMN_NAME_MEDIA_ID, mediaId);
          contentValues.put(IndexEntry.COLUMN_NAME_WIDTH, encodedImage.getWidth());
          contentValues.put(IndexEntry.COLUMN_NAME_HEIGHT, encodedImage.getHeight());
          contentValues.put(IndexEntry.COLUMN_NAME_CACHE_KEY, cacheKey.getUriString());
          contentValues
              .put(IndexEntry.COLUMN_NAME_RESOURCE_ID, CacheKeyUtil.getFirstResourceId(cacheKey));

          db.insertOrThrow(IndexEntry.TABLE_NAME, null, contentValues);

          db.setTransactionSuccessful();
        } catch (Exception x) {
          FLog.e(TAG, x, "Error writing for %s", mediaId);
        } finally {
          db.endTransaction();
        }
      }
    });
  }

  private static final class IndexEntry implements BaseColumns {

    public static final String TABLE_NAME = "media_variations_index";
    public static final String COLUMN_NAME_MEDIA_ID = "media_id";
    public static final String COLUMN_NAME_WIDTH = "width";
    public static final String COLUMN_NAME_HEIGHT = "height";
    public static final String COLUMN_NAME_CACHE_KEY = "cache_key";
    public static final String COLUMN_NAME_RESOURCE_ID = "resource_id";
  }

  private static class LazyIndexDbOpenHelper {
    private final Context mContext;
    private @Nullable IndexDbOpenHelper mIndexDbOpenHelper;

    private LazyIndexDbOpenHelper(Context context) {
      mContext = context;
    }

    public synchronized SQLiteDatabase getWritableDatabase() {
      if (mIndexDbOpenHelper == null) {
        mIndexDbOpenHelper = new IndexDbOpenHelper(mContext);
      }
      return mIndexDbOpenHelper.getWritableDatabase();
    }
  }

  private static class IndexDbOpenHelper extends SQLiteOpenHelper {

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + IndexEntry.TABLE_NAME + " (" +
            IndexEntry._ID + " INTEGER PRIMARY KEY," +
            IndexEntry.COLUMN_NAME_MEDIA_ID + TEXT_TYPE + "," +
            IndexEntry.COLUMN_NAME_WIDTH + INTEGER_TYPE + "," +
            IndexEntry.COLUMN_NAME_HEIGHT + INTEGER_TYPE + "," +
            IndexEntry.COLUMN_NAME_CACHE_KEY + TEXT_TYPE + "," +
            IndexEntry.COLUMN_NAME_RESOURCE_ID + TEXT_TYPE + " )";
    private static final String SQL_CREATE_INDEX =
        "CREATE INDEX index_media_id ON " + IndexEntry.TABLE_NAME + " (" +
            IndexEntry.COLUMN_NAME_MEDIA_ID + ")";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FrescoMediaVariationsIndex.db";

    public IndexDbOpenHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
      db.beginTransaction();
      try {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(SQL_CREATE_INDEX);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      db.beginTransaction();
      try {
        db.execSQL(SQL_DELETE_ENTRIES);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
      }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      onUpgrade(db, oldVersion, newVersion);
    }
  }
}
