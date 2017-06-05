package com.facebook.drawee.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Created by Linhh on 16/10/19.
 */

public class RichDrawable extends BitmapDrawable implements CustomPainterDrawable{

  public static final int POS_LEFT = 1;
  public static final int POS_RIGHT = 2;
  public static final int POS_TOP = 3;
  public static final int POS_BOTTOM = 4;
  public static final int POS_CENTER = 5;
  public static final int POS_LEFT_TOP = 6;
  public static final int POS_LEFT_BOTTOM = 7;
  public static final int POS_RIGHT_TOP = 9;
  public static final int POS_RIGHT_BOTTOM = 10;

  private int mMarginLeft = 0;
  private int mMarginRight = 0;
  private int mMarginTop = 0;
  private int mMarginBottom = 0;

  private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  Bitmap mBitmap;
  private int mPos = POS_CENTER;
  private int bitmapWidth = 0;
  private int bitmapHeight = 0;

  private int mPadingLeft = 0;
  private int mPadingRight = 0;
  private int mPadingTop = 0;
  private int mPadingBottom = 0;

  private String mText;
  private int mSize = 16;
  private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  public RichDrawable(Bitmap bitmap){
    BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
    mBitmap = bitmapDrawable.getBitmap();
    bitmapWidth = bitmapDrawable.getIntrinsicWidth();
    bitmapHeight = bitmapDrawable.getIntrinsicHeight();
  }

  public RichDrawable(Drawable drawable){
    BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
    mBitmap = bitmapDrawable.getBitmap();
    bitmapWidth = bitmapDrawable.getIntrinsicWidth();
    bitmapHeight = bitmapDrawable.getIntrinsicHeight();
  }

  public void setText(String text){
    mText = text;
  }

  public void setTextColor(int color){
    mTextPaint.setColor(color);
  }

  public void setTextSize(float size){
    mTextPaint.setTextSize(size);
  }

  public void setTextPadding(int left, int right, int top, int bottom){
    mPadingLeft = left;
    mPadingRight = right;
    mPadingTop = top;
    mPadingBottom = bottom;
  }

  @Override
  public void draw(Canvas canvas) {
    if(mBitmap == null){
      return;
    }
//    super.draw(canvas);
    int left = getBounds().left;
    int right = getBounds().right;
    int top = getBounds().top;
    int bottom = getBounds().bottom;
    int w = right - left;
    int h = bottom - top;
    if(mPos == POS_CENTER){
      canvas.drawBitmap(mBitmap,left + w/2 + mMarginLeft,top + h / 2 + mMarginTop,mPaint);
//      if(!TextUtils.isEmpty(mText)) {
//        canvas.drawText(mText, left + w / 2 + mMarginLeft + mPadingLeft, top + h / 2 + mMarginTop + mPadingTop, mTextPaint);
//      }
      return ;
    }
    if(mPos == POS_BOTTOM || mPos == POS_LEFT_BOTTOM){
      canvas.drawBitmap(mBitmap,left + mMarginLeft, bottom - getBitmapHeight() - mMarginBottom,mPaint);
      return;
    }
    if(mPos == POS_LEFT || mPos == POS_TOP || mPos == POS_LEFT_TOP){
      canvas.drawBitmap(mBitmap, left + mMarginLeft, top + mMarginTop, mPaint);
      return;
    }
    if(mPos == POS_RIGHT || mPos == POS_RIGHT_TOP){
      canvas.drawBitmap(mBitmap, right - getBitmapWidth() - mMarginRight, top + mMarginTop, mPaint);
      return;
    }

    if(mPos == POS_RIGHT_BOTTOM){
      canvas.drawBitmap(mBitmap,right - getBitmapWidth() - mMarginRight, bottom - getBitmapHeight() - mMarginBottom, mPaint);
      return;
    }

  }

  public int getBitmapWidth(){
    return bitmapWidth;
  }

  public int getBitmapHeight(){
    return bitmapHeight;
  }

  public void setPosition(int pos){
    mPos = pos;
  }

  public void setMargin(int left, int right, int top, int bottom){
    mMarginLeft = left;
    mMarginRight = right;
    mMarginTop = top;
    mMarginBottom = bottom;
  }

  public void setMarginLeft(int left){
    mMarginLeft = left;
  }

  public void setMarginRight(int right){
    mMarginRight = right;
  }

  public void setMarginTop(int top){
    mMarginTop = top;
  }

  public void setMarginBottom(int bottom){
    mMarginBottom = bottom;
  }

  @Override
  public void setAlpha(int alpha) {
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {

  }

  @Override
  public int getOpacity() {
    return 0;
  }
}
