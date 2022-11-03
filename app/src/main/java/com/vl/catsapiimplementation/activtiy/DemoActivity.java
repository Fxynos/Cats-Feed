package com.vl.catsapiimplementation.activtiy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vl.catsapiimplementation.R;
import com.vl.catsapiimplementation.Adapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DemoActivity extends AppCompatActivity implements Adapter.OnClickListener {
    final private static File DOWNLOADS = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Cats Feed");
    final private static ExecutorService executor = Executors.newSingleThreadExecutor();
    final private static Gson g = new Gson();
    final private static String
            url = "https://api.thecatapi.com/v1/images/search";
    final private static int CHUNK = 10;

    private RecyclerView list;
    private Adapter adapter;
    volatile private boolean loading = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_layout);
        if (!DOWNLOADS.exists() || !DOWNLOADS.isDirectory())
            DOWNLOADS.mkdir();
        list = findViewById(R.id.list);
        adapter = new Adapter(this);
        list.setAdapter(adapter);
        asyncUpdate(CHUNK);
        list.setOnScrollChangeListener((View view, int i, int i1, int i2, int i3) -> {
                if (!loading && list.computeVerticalScrollExtent() * 4 + list.computeVerticalScrollOffset() >= list.computeVerticalScrollRange())
                    asyncUpdate(CHUNK);
        });
        adapter.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
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
        loading = true;
        final int s = adapter.getItemCount();
        executor.execute(() -> {
            final Collection<Adapter.Item> items;
            try {
                items = load(count);
            } catch (IOException exception) {
                exception.printStackTrace();
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "E: ".concat(exception.getMessage()), Toast.LENGTH_LONG).show());
                return;
            }
            runOnUiThread(() -> {
                adapter.getItems().addAll(items);
                adapter.notifyItemRangeInserted(s, items.size());
                loading = false;
            });
        });
    }

    private Collection<Adapter.Item> load(int count) throws IOException {
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
        final ArrayList<Adapter.Item> items = new ArrayList<>();
        for (Object o : list) {
            final HashMap<String, Object> map = g.fromJson(g.toJson(o), new TypeToken<HashMap<String, Object>>(){}.getType());
            items.add(new Adapter.Item((String) map.get("url"), (String) map.get("id")));
        }
        for (Adapter.Item item : items)
            Log.d("api", item.getUrl());
        return items;
    }

    private void save(Bitmap bitmap, String name) {
        final File file = new File(DOWNLOADS, name.concat(".jpg"));
        if (!file.exists())
            CompletableFuture.runAsync(() -> {
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Saved ".concat(file.getAbsolutePath()), Toast.LENGTH_SHORT).show());
                    } else runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Couldn't save", Toast.LENGTH_LONG).show());
                } catch (IOException exception) {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error: ".concat(exception.getMessage()), Toast.LENGTH_LONG).show());
                }
            });
    }

    private boolean isPictureDownloaded(Adapter.Item item) {
        return new File(DOWNLOADS, item.getId().concat(".jpg")).exists();
    }

    @Override
    public void onClick(View view, int position) {
        switch (view.getId()) {
            case R.id.card:
                if (adapter.getItems().get(position).getState() == Adapter.Item.LoadingState.SUCCESS){
                    View shadow = ((Adapter.ViewHolder) list.getChildViewHolder(view)).getShadowMenu();
                    shadow.setVisibility(shadow.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                }
                break;
            case R.id.save:
                final Adapter.Item item = adapter.getItems().get(position);
                if (item.getState() == Adapter.Item.LoadingState.SUCCESS)
                    if (!isPictureDownloaded(item))
                        save(item.getBitmap(), item.getId());
                    else Toast.makeText(this, "Saved earlier", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
