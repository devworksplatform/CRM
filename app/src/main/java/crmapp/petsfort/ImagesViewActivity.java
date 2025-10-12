package crmapp.petsfort;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import crmapp.petsfort.layouts.ZoomableImageView;

public class ImagesViewActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URLS = "image_urls";
    public static final String EXTRA_INITIAL_POSITION = "initial_position";
    private static final String TAG = "ImagesViewActivity";

    private ViewPager2 viewPager;
    private TextView counterTextView;
    private View controlsContainer;
    private boolean areControlsVisible = true;
    private ArrayList<String> imageUrls;
    private ImageAdapter adapter;
    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagesviewactivity);

        // Setup immersive mode
        setupImmersiveMode();

        // Get images from intent
        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        int initialPosition = getIntent().getIntExtra(EXTRA_INITIAL_POSITION, 0);

        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
            Log.e(TAG, "No image URLs provided");
        }

        if (imageUrls.isEmpty()) {
            Toast.makeText(this, "No images to display", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Debug log image URLs
        for (int i = 0; i < imageUrls.size(); i++) {
            Log.d(TAG, "Image URL " + i + ": " + imageUrls.get(i));
        }

        // Initialize UI components
        viewPager = findViewById(R.id.viewPager);
        counterTextView = findViewById(R.id.counterTextView);
        controlsContainer = findViewById(R.id.controlsContainer);
        ImageButton backButton = findViewById(R.id.backButton);

        // Setup back button
        backButton.setOnClickListener(v -> finish());

        // Setup ViewPager and adapter
        adapter = new ImageAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);

        // Set initial position if provided
        if (initialPosition >= 0 && initialPosition < imageUrls.size()) {
            viewPager.setCurrentItem(initialPosition, false);
        }

        // Setup counter
        updateCounter(viewPager.getCurrentItem());

        // Page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCounter(position);
            }
        });

        // Setup gesture detector for tap to toggle controls
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }
        });

        // Apply gesture detector to the ViewPager
        viewPager.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void setupImmersiveMode() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        View decorView = getWindow().getDecorView();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(flags);
    }

    private void toggleControls() {
        if (areControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void hideControls() {
        controlsContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        controlsContainer.setVisibility(View.GONE);
                    }
                });
        areControlsVisible = false;
    }

    private void showControls() {
        controlsContainer.setAlpha(0f);
        controlsContainer.setVisibility(View.VISIBLE);
        controlsContainer.animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null);
        areControlsVisible = true;
    }

    private void updateCounter(int position) {
        counterTextView.setText(String.format("%d/%d", position + 1, imageUrls.size()));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupImmersiveMode();
        }
    }

    static class ImageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final android.content.Context ctx;
        private final List<String> urls;

        ImageAdapter(android.content.Context context, List<String> urls) {
            this.ctx = context;
            this.urls = urls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ZoomableImageView imageView = new ZoomableImageView(ctx);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = urls.get(position);
            Log.d(TAG, "Loading image at position " + position + ": " + url);

            // Reset zoom state if needed
            if (holder.imageView instanceof ZoomableImageView) {
                ((ZoomableImageView) holder.imageView).setImageMatrix(new android.graphics.Matrix());
            }

            // Load image with Glide
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo);

            Glide.with(ctx)
                    .load(url)
                    .apply(options)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                this.imageView = (ImageView) itemView;
            }
        }
    }

    // Static method to start this activity
    public static void start(android.content.Context context, ArrayList<String> imageUrls, int initialPosition) {
        Intent intent = new Intent(context, ImagesViewActivity.class);
        intent.putStringArrayListExtra(EXTRA_IMAGE_URLS, imageUrls);
        intent.putExtra(EXTRA_INITIAL_POSITION, initialPosition);
        context.startActivity(intent);
    }
}