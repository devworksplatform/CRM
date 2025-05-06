package crmapp.petsfort;


import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import crmapp.petsfort.layouts.ZoomableImageView;

public class ImagesViewActivity extends androidx.appcompat.app.AppCompatActivity {

    public static final String EXTRA_IMAGE_URLS = "image_urls";

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagesviewactivity);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);


        java.util.ArrayList<String> urls = getIntent()
                .getStringArrayListExtra(EXTRA_IMAGE_URLS);
        if (urls == null) urls = new java.util.ArrayList<>();

        androidx.recyclerview.widget.RecyclerView rv =
                findViewById(R.id.recyclerView);
        androidx.recyclerview.widget.LinearLayoutManager lm =
                new androidx.recyclerview.widget.LinearLayoutManager(
                        this,
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                        false
                );
        rv.setLayoutManager(lm);
        System.out.println(urls);
        rv.setAdapter(new ImagesAdapter(this, urls));
    }

    static class ImagesAdapter extends
            androidx.recyclerview.widget.RecyclerView.Adapter<ImagesAdapter.ViewHolder> {

        private final android.content.Context ctx;
        private final java.util.List<String> urls;

        ImagesAdapter(android.content.Context context, java.util.List<String> urls) {
            this.ctx = context;
            this.urls = urls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(ctx)
                    .inflate(R.layout.lay_for_imagesviewactivity, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ImagesAdapter.ViewHolder holder, int position) {
            String url = urls.get(position);
            com.bumptech.glide.Glide
                    .with(ctx)
                    .load(url)
                    .placeholder(R.drawable.logo) // optional
                    .error(R.drawable.logo)             // optional
                    .into(holder.photoView);
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView photoView;
            ViewHolder(android.view.View itemView) {
                super(itemView);
                photoView = itemView.findViewById(R.id.photoView);
            }
        }
    }
}
