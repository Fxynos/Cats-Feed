package com.vl.catsapiimplementation.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.vl.catsapiimplementation.R;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    final static private Handler mainThread = new Handler(Looper.getMainLooper());

    final static private int ANIMATION_DURATION = 250;
    final static private Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    final private ArrayList<Adapter.Item> items;
    private Adapter.OnClickListener clickListener = null;
    final private LayoutInflater inflater;
    @DrawableRes
    private Integer icon = null;

    public Adapter(Context context, ArrayList<Adapter.Item> items) {
        this.items = items;
        inflater = LayoutInflater.from(context);
    }

    public ArrayList<Adapter.Item> getItems() {
        return items;
    }

    public void setButtonIconResource(@DrawableRes int icon) {
        this.icon = icon;
    }

    /**
     * ImageButton click event is available only after icon is set
     */
    public void setOnClickListener(Adapter.OnClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Adapter.ViewHolder(inflater.inflate(R.layout.item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        if (icon != null)
            holder.button.setImageResource(icon);
        holder.interceptAnimation();
        holder.img.setImageBitmap(null);
        holder.shimmer.showShimmer(true);
        items.get(position).setOnLoadListener((bitmap) -> {
            if (holder.getAdapterPosition() == position) {
                if (bitmap == null)
                    holder.img.setImageResource(R.drawable.placeholder_image);
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

    protected final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView img;
        final ShimmerFrameLayout shimmer;
        final ImageButton button;
        final LinearLayout shadow;
        final ObjectAnimator animator;

        public ViewHolder(View view) {
            super(view);
            button = view.findViewById(R.id.save);
            shadow = view.findViewById(R.id.shadow);
            img = view.findViewById(R.id.image);
            shimmer = view.findViewById(R.id.shimmer);
            view.setOnClickListener(this);
            button.setOnClickListener(this);
            animator = initAnimator();
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.card) {
                if (isAnimationAvailable() && items.get(getAdapterPosition()).bitmap != null)
                    startAnimationAppear();
            } else if (clickListener != null)
                clickListener.onClick(view, getAdapterPosition());
        }

        private boolean isAnimationAvailable() {
            return animator != null && !animator.isRunning() && shimmer != null && !shimmer.isShimmerVisible();
        }

        private void startAnimationAppear() {
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

    public static abstract class Item {
        public enum LoadingState {
            LOADING,
            SUCCESS,
            ERROR
        }

        private Bitmap bitmap = null;
        private Adapter.Item.OnLoadListener listener = null;
        private Adapter.Item.LoadingState state = Adapter.Item.LoadingState.LOADING;

        private void setOnLoadListener(Adapter.Item.OnLoadListener listener) {
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

        public Item() {
            startLoading();
        }

        private void setState(Adapter.Item.LoadingState state) {
            this.state = state;
            if (listener != null && (state == Adapter.Item.LoadingState.ERROR || state == Adapter.Item.LoadingState.SUCCESS))
                mainThread.post(() -> listener.onDone(bitmap));
        }

        private void startLoading() {
            CompletableFuture.runAsync(() -> {
                bitmap = load();
                setState(bitmap == null ? LoadingState.ERROR : LoadingState.SUCCESS);
            });
        }

        @Nullable
        protected abstract Bitmap load();

        public Bitmap getBitmap() {
            return bitmap;
        }

        public Adapter.Item.LoadingState getState() {
            return state;
        }

        public interface OnLoadListener {
            void onDone(Bitmap bitmap);
        }
    }
}
