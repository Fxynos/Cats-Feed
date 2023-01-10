package com.vl.catsapiimplementation.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vl.catsapiimplementation.adapter.Adapter;
import com.vl.catsapiimplementation.R;
import com.vl.catsapiimplementation.activity.DemoActivity;
import com.vl.catsapiimplementation.adapter.FeedItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedFragment extends Fragment implements Adapter.OnClickListener {
    private ExecutorService executor;
    final private static Handler mainThreadLooper = new Handler(Looper.getMainLooper());
    final private static Gson g = new Gson();
    final private static String
            url = "https://api.thecatapi.com/v1/images/search";
    final private static int CHUNK = 10;

    private RecyclerView list;
    private Adapter adapter;
    private ImageView loadingIcon;
    private AnimatedVectorDrawable loadingAnimation = null;
    volatile private boolean loading = true;

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    /* Demo without API
    private Collection<Adapter.Item> getFewTemplates(int count) {
        final Collection<Adapter.Item> items = new ArrayList<>();
        for (int i = 0; i < count; i++)
            items.add(new Adapter.Item("https://malformed-url"));
        return items;
    }*/

    public void asyncUpdate(int count) {
        assert getContext() != null;
        loading = true;
        loadingAnimation.start();
        loadingIcon.setVisibility(View.VISIBLE);
        final int s = adapter.getItemCount();
        executor.execute(() -> {
            final Collection<FeedItem> items;
            try {
                items = load(count);
            } catch (IOException exception) { // TODO more attempts
                exception.printStackTrace();
                mainThreadLooper.post(() -> Toast.makeText(getContext(), String.format("Exception: %s", exception.getMessage()), Toast.LENGTH_LONG).show());
                return;
            }
            mainThreadLooper.post(() -> {
                adapter.getItems().addAll(items);
                adapter.notifyItemRangeInserted(s, items.size());
                loading = false;
                loadingAnimation.stop();
                loadingIcon.setVisibility(View.GONE);
            });
        });
    }

    private Collection<FeedItem> load(int count) throws IOException {
        if (count > 10 || count <= 0)
            throw new IllegalArgumentException("Illegal count");
        HttpURLConnection connection = (HttpURLConnection) new URL(url.concat("?limit=").concat(Integer.toString(count))).openConnection();
        connection.setConnectTimeout(10_000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder builder = new StringBuilder();
        String s;
        while ((s = reader.readLine()) != null)
            builder.append(s).append("\n");
        final String response = builder.toString();
        ArrayList<Object> list = g.fromJson(response, new TypeToken<ArrayList<Object>>(){}.getType());
        final ArrayList<FeedItem> items = new ArrayList<>();
        for (Object o : list) {
            final HashMap<String, Object> map = g.fromJson(g.toJson(o), new TypeToken<HashMap<String, Object>>(){}.getType());
            items.add(new FeedItem((String) map.get("url"), (String) map.get("id")));
        }
        return items;
    }

    private void save(Bitmap bitmap, String name) {
        final File file = new File(DemoActivity.getDownloads(), name.concat(".jpg"));
        if (!file.exists())
            CompletableFuture.runAsync(() -> {
                assert(getContext() != null);
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                        getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        mainThreadLooper.post(() -> Toast.makeText(getContext(), "Saved ".concat(file.getAbsolutePath()), Toast.LENGTH_SHORT).show());
                    } else mainThreadLooper.post(() -> Toast.makeText(getContext(), "Couldn't save", Toast.LENGTH_LONG).show());
                } catch (IOException exception) {
                    mainThreadLooper.post(() -> Toast.makeText(getContext(), String.format("Exception: %s", exception.getMessage()), Toast.LENGTH_LONG).show());
                }
            });
    }

    private boolean isPictureDownloaded(FeedItem item) {
        return new File(DemoActivity.getDownloads(), item.getId().concat(".jpg")).exists();
    }

    private boolean isTimeToUpdate() {
        return !loading && list.computeVerticalScrollExtent() * 4 + list.computeVerticalScrollOffset() >= list.computeVerticalScrollRange();
    }

    @Override
    public void onClick(View view, int position) {
        final FeedItem item = (FeedItem) adapter.getItems().get(position);
        if (item.getState() == Adapter.Item.LoadingState.SUCCESS)
            if (!isPictureDownloaded(item))
                save(item.getBitmap(), item.getId());
            else Toast.makeText(getContext(), "Saved earlier", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         executor = Executors.newSingleThreadExecutor();
        final View root = inflater.inflate(R.layout.fragment_feed, container, false);
        loadingIcon = root.findViewById(R.id.loadingIcon);
        loadingIcon.setBackgroundResource(R.drawable.loading_animated);
        loadingAnimation = (AnimatedVectorDrawable) loadingIcon.getBackground();
        list = root.findViewById(R.id.list);
        adapter = new Adapter(getContext(), new ViewModelProvider(getActivity()).get(FeedModel.class).getFeedItems());
        list.setAdapter(adapter);
        if (adapter.getItemCount() == 0) asyncUpdate(CHUNK);
        list.setOnScrollChangeListener((View view, int i, int i1, int i2, int i3) -> {
            if (isTimeToUpdate())
                asyncUpdate(CHUNK);
        });
        adapter.setOnClickListener(this);
        adapter.setButtonIconResource(R.drawable.ic_download);
        return root;
    }
}
