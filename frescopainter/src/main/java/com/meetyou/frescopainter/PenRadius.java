package com.meetyou.frescopainter;

/**
 * 圆笔
 * Created by Linhh on 16/9/10.
 */
public class PenRadius implements Cloneable{

    private int mTopLeftRadius;
    private int mTopRightRadius;
    private int mBottomLeftRadius;
    private int mBottomRightRadius;

    public int getTopLeftRadius() {
        return mTopLeftRadius;
    }

    public void setTopLeftRadius(int mTopLeftRadius) {
        this.mTopLeftRadius = mTopLeftRadius;
    }

    public int getTopRightRadius() {
        return mTopRightRadius;
    }

    public void setTopRightRadius(int mTopRightRadius) {
        this.mTopRightRadius = mTopRightRadius;
    }

    public int getBottomLeftRadius() {
        return mBottomLeftRadius;
    }

    public void setBottomLeftRadius(int mBottomLeftRadius) {
        this.mBottomLeftRadius = mBottomLeftRadius;
    }

    public int getBottomRightRadius() {
        return mBottomRightRadius;
    }

    public void setBottomRightRadius(int mBottomRightRadius) {
        this.mBottomRightRadius = mBottomRightRadius;
    }

    private PenRadius(){

    }

    public PenRadius(int radius){
        this(radius,radius,radius,radius);
    }

    public PenRadius(int topLeftRadius, int topRightRadius, int bottomLeftRadius,int bottomRightRadius){
        mTopLeftRadius = topLeftRadius;
        mTopRightRadius = topRightRadius;
        mBottomLeftRadius = bottomLeftRadius;
        mBottomRightRadius = bottomRightRadius;
    }

    public PenRadius clone(){
        try {
            return (PenRadius)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return this;
    }


}
