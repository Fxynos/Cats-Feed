package com.vl.catsapiimplementation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    final static private Handler mainThread = new Handler(Looper.getMainLooper());

    final private ArrayList<Item> items;
    private OnClickListener listener = null;
    final private Context context;
    final private LayoutInflater inflater;

    public RecyclerViewAdapter(Context context, ArrayList<Item> items) {
        this.items = items;
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public RecyclerViewAdapter(Context context) {
        this(context, new ArrayList<>());
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        holder.img.setImageBitmap(null);
        holder.shimmer.showShimmer(true);
        items.get(position).setOnLoadListener((bitmap) -> {
            if (bitmap == null)
                holder.img.setImageResource(R.drawable.ic_baseline_signal_wifi_off_24);
            else
                holder.img.setImageBitmap(bitmap);
            holder.shimmer.stopShimmer();
            holder.shimmer.hideShimmer();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final private ImageView img;
        final private ShimmerFrameLayout shimmer;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            img = view.findViewById(R.id.image);
            shimmer = view.findViewById(R.id.shimmer);
        }

        @Override
        public void onClick(View view) {
            if (listener != null)
                listener.onClick(view, getAdapterPosition());
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }

    public static class Item {
        public enum LoadingState {
            LOADING,
            SUCCESS,
            FUCKED_UP
        }

        private Bitmap bitmap = null;
        private OnLoadListener listener = null;
        private LoadingState state = LoadingState.LOADING;
        private final String url;

        public Item(String url) {
            this.url = url;
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
                case FUCKED_UP:
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
                    setState(LoadingState.FUCKED_UP);
                }
            });
        }

        public String getUrl() {
            return url;
        }

        public LoadingState getState() {
            return state;
        }

        public void setState(LoadingState state) {
            this.state = state;
            if (listener != null && (state == LoadingState.FUCKED_UP || state == LoadingState.SUCCESS))
                mainThread.post(() -> listener.onDone(bitmap));
        }

        public interface OnLoadListener {
            void onDone(Bitmap bitmap);
        }
    }
}
