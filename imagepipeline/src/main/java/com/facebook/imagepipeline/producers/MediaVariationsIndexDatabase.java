/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.concurrent.Executor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class MediaVariationsIndexDatabase {

  private static final String[] PROJECTION = {
      IndexEntry.COLUMN_NAME_CACHE_KEY,
      IndexEntry.COLUMN_NAME_WIDTH,
      IndexEntry.COLUMN_NAME_HEIGHT
  };

  private static final String SQL_DELETE_ENTRIES =
      "DROP TABLE IF EXISTS " + IndexEntry.TABLE_NAME;

  private final IndexDbOpenHelper mDbHelper;
  private final Executor mReadExecutor;
  private final Executor mWriteExecutor;

  public MediaVariationsIndexDatabase(
      Context context,
      Executor readExecutor,
      Executor writeExecutor) {
    mDbHelper = new IndexDbOpenHelper(context);
    mReadExecutor = readExecutor;
    mWriteExecutor = writeExecutor;
  }

  private static final class IndexEntry implements BaseColumns {

    public static final String TABLE_NAME = "media_variations_index";
    public static final String COLUMN_NAME_MEDIA_ID = "media_id";
    public static final String COLUMN_NAME_WIDTH = "width";
    public static final String COLUMN_NAME_HEIGHT = "height";
    public static final String COLUMN_NAME_CACHE_KEY = "cache_key";
    public static final String COLUMN_NAME_RESOURCE_ID = "resource_id";
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
