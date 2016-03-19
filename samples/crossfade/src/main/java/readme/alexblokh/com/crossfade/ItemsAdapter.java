package readme.alexblokh.com.crossfade;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

/**
 * DataSet adapter for showcase app
 * Created by AlexBlokh on 3/13/16.
 */
public class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ViewHolder> {

  private final List<Item> items;
  private final RoundingParams roundingParams;

  public ItemsAdapter(Context context, @NonNull List<Item> items) {
    this.items = items;
    int cornerRadius = dp(2, context);
    roundingParams = new RoundingParams();
    roundingParams.setCornersRadii(cornerRadius, cornerRadius, 0, 0);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    ViewGroup root = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
    root.addView(new ItemLayout(parent.getContext()));
    return new ViewHolder(root);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    ItemLayout itemLayout = (ItemLayout) ((ViewGroup) holder.itemView).getChildAt(0);
    itemLayout.price.setText("$1.99");
    itemLayout.description.setText("Mock item description");

    Item item = items.get(position);


    SimpleDraweeView draweeView = itemLayout.roundedImageView;
    draweeView.getHierarchy().setRoundingParams(roundingParams);
    draweeView.getHierarchy().setCrossFadeEnabled(false);
    draweeView.getHierarchy().setPlaceholderImage(new ColorDrawable(item.color));
    draweeView.getHierarchy().setFadeDuration(2000);
    draweeView.setAspectRatio((float) item.width / item.height);
    draweeView.setImageURI(Uri.parse(item.url));

  }

  private int dp(int pxToDp, Context context) {
    return (int) (context.getResources().getDisplayMetrics().density * pxToDp + .5f);
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }
  }
}
