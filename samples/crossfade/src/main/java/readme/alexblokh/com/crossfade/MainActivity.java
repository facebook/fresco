package readme.alexblokh.com.crossfade;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;

import com.facebook.drawee.backends.pipeline.Fresco;

public class MainActivity extends AppCompatActivity {

  private RecyclerView recyclerView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Fresco.initialize(this);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    assert recyclerView != null;
    recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
    recyclerView.setHasFixedSize(true);


    ItemsAdapter adapter = new ItemsAdapter(this, Data.items());
    recyclerView.setAdapter(adapter);
  }
}
