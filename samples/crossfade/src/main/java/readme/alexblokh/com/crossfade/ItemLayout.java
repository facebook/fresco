package readme.alexblokh.com.crossfade;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

public class ItemLayout extends ViewGroup {

    public SimpleDraweeView roundedImageView;
    public ImageView whatsHotCornor;
    public TextView price;
    public TextView description;

    public ItemLayout(Context context) {
        super(context);

        roundedImageView = new SimpleDraweeView(context);
        addView(roundedImageView, new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        price = new TextView(context);
        price.setTextSize(14);
        price.setTextColor(Color.BLACK);
        price.setTypeface(mediumTypefaceCompat());
        price.setSingleLine();
        price.setIncludeFontPadding(false);
        addView(price);

        description = new TextView(context);
        description.setTextColor(Color.parseColor("#999999"));
        description.setTextSize(14);
        description.setMaxLines(3);
        description.setEllipsize(TextUtils.TruncateAt.END);
        description.setIncludeFontPadding(false);
        addView(description, new ViewGroup.MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        whatsHotCornor = new ImageView(context);
        addView(whatsHotCornor, new ViewGroup.LayoutParams(dp(40), dp(40)));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;

        measureChild(roundedImageView, widthMeasureSpec, heightMeasureSpec);
        height += Math.max(roundedImageView.getMeasuredHeight(), dp(40));

        measureChild(whatsHotCornor, widthMeasureSpec, heightMeasureSpec);

        measureChild(price, widthMeasureSpec, heightMeasureSpec);
        height += dp(12); //price margin
        height += price.getMeasuredHeight();

        measureChildWithMargins(description, widthMeasureSpec, dp(32), heightMeasureSpec, 0);
        height += dp(4); //description margin
        height += description.getMeasuredHeight();
        height += dp(12); //bottom padding

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = roundedImageView.getMeasuredWidth();
        int height = roundedImageView.getMeasuredHeight();
        roundedImageView.layout(0, 0, width, height);

        int validImageHeight = Math.max(height, dp(40));

        int priceWidth = price.getMeasuredWidth();
        int priceHeight = price.getMeasuredHeight();
        int priceLeft = dp(16);
        int priceTop = validImageHeight + dp(12);
        price.layout(
                priceLeft,
                priceTop,
                priceLeft + priceWidth,
                priceTop + priceHeight
        );

        int descriptionWidth = description.getMeasuredWidth();
        int descriptionHeight = description.getMeasuredHeight();
        int descriptionLeft = dp(16);
        int descriptionTop = priceTop + priceHeight + dp(4);
        description.layout(descriptionLeft,
                descriptionTop,
                descriptionLeft + descriptionWidth,
                descriptionTop + descriptionHeight);


        int whatsHotWidth = whatsHotCornor.getMeasuredWidth();
        int whatsHotHeight = whatsHotCornor.getMeasuredHeight();
        whatsHotCornor.layout(width - whatsHotWidth, 0, width, whatsHotHeight);
    }

    public static Typeface mediumTypefaceCompat() {
        if (Build.VERSION.SDK_INT >= 21) {
            return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            return Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        }
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
