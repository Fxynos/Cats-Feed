package com.vl.catsapiimplementation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    final static private Handler mainThread = new Handler(Looper.getMainLooper());

    final static private int ANIMATION_DURATION = 250;
    final static private Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    final private ArrayList<Item> items;
    private OnClickListener clickListener = null;
    final private Context context;
    final private LayoutInflater inflater;

    public Adapter(Context context, ArrayList<Item> items) {
        this.items = items;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public Adapter(Context context) {
        this(context, new ArrayList<>());
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        holder.interceptAnimation();
        holder.img.setImageBitmap(null);
        holder.shimmer.showShimmer(true);
        items.get(position).setOnLoadListener((bitmap) -> {
            if (holder.getAdapterPosition() == position) {
                if (bitmap == null)
                    holder.img.setImageResource(R.drawable.ic_baseline_signal_wifi_off_24);
                else
                    holder.img.setImageBitmap(bitmap);
                holder.shimmer.stopShimmer();
                holder.shimmer.hideShimmer();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final private ImageView img;
        final private ShimmerFrameLayout shimmer;
        final private ImageButton save;
        final private LinearLayout shadow;
        final private ObjectAnimator animator;

        public ViewHolder(View view) {
            super(view);
            save = view.findViewById(R.id.save);
            shadow = view.findViewById(R.id.shadow);
            img = view.findViewById(R.id.image);
            shimmer = view.findViewById(R.id.shimmer);
            view.setOnClickListener(this); // R.id.card
            save.setOnClickListener(this);
            animator = initAnimator();
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null)
                clickListener.onClick(view, getAdapterPosition());
        }

        public boolean isAnimationAvailable() {
            return animator != null && !animator.isRunning() && shimmer != null && !shimmer.isShimmerVisible();
        }

        public void startAnimationAppear() {
            if (shadow.getVisibility() == View.VISIBLE)
                animator.setFloatValues(1, 0);
            else {
                animator.setFloatValues(0, 1);
                shadow.setVisibility(View.VISIBLE);
            }
            animator.start();
        }

        private void interceptAnimation() {
            animator.pause();
            shadow.setVisibility(View.INVISIBLE);
        }

        private ObjectAnimator initAnimator() {
            ObjectAnimator animator = new ObjectAnimator();
            animator.setTarget(shadow);
            animator.setDuration(ANIMATION_DURATION);
            animator.setInterpolator(ANIMATION_INTERPOLATOR);
            animator.setPropertyName("alpha");
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    shadow.setVisibility(shadow.getAlpha() == 1 ? View.VISIBLE : View.INVISIBLE);
                }
            });
            return animator;
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }

    public static class Item {
        public enum LoadingState {
            LOADING,
            SUCCESS,
            ERROR
        }

        private Bitmap bitmap = null;
        private OnLoadListener listener = null;
        private LoadingState state = LoadingState.LOADING;
        private final String url, id;

        public Item(String url, String id) {
            this.url = url;
            this.id = id;
            load(url);
        }

        public void setOnLoadListener(OnLoadListener listener) {
            switch (state) {
                case LOADING:
                    this.listener = listener;
                    break;
                case SUCCESS:
                    listener.onDone(bitmap);
                    break;
                case ERROR:
                    listener.onDone(null);
                    break;
            }
        }

        private void load(String imgUrl) {
            CompletableFuture.runAsync(() -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(imgUrl).openConnection();
                    bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                    setState(LoadingState.SUCCESS);
                } catch (IOException exception) {
                    setState(LoadingState.ERROR);
                }
            });
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public LoadingState getState() {
            return state;
        }

        public void setState(LoadingState state) {
            this.state = state;
            if (listener != null && (state == LoadingState.ERROR || state == LoadingState.SUCCESS))
                mainThread.post(() -> listener.onDone(bitmap));
        }

        public interface OnLoadListener {
            void onDone(Bitmap bitmap);
        }
    }
}
