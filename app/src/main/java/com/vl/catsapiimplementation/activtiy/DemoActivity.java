package com.vl.catsapiimplementation.activtiy;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vl.catsapiimplementation.R;
import com.vl.catsapiimplementation.RecyclerViewAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DemoActivity extends AppCompatActivity {
    final private static ExecutorService executor = Executors.newSingleThreadExecutor();
    final private static Gson g = new Gson();
    final private static String
            url = "https://api.thecatapi.com/v1/images/search";
    final private static int CHUNK = 10;

    private RecyclerView list;
    private RecyclerViewAdapter adapter;
    volatile private boolean loading = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_layout);
        list = findViewById(R.id.list);
        adapter = new RecyclerViewAdapter(this);
        list.setAdapter(adapter);
        asyncUpdate(CHUNK);
        list.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                if (!loading && list.computeVerticalScrollExtent() * 1.5 + list.computeVerticalScrollOffset() >= list.computeVerticalScrollRange())
                    asyncUpdate(CHUNK);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    public void asyncUpdate(int count) {
        loading = true;
        final int s = adapter.getItemCount();
        executor.execute(() -> {
            final Collection<RecyclerViewAdapter.Item> items;
            try {
                items = load(count);
            } catch (IOException exception) {
                exception.printStackTrace();
                return;
            }
            runOnUiThread(() -> {
                adapter.getItems().addAll(items);
                adapter.notifyItemRangeInserted(s, items.size());
                loading = false;
            });
        });
    }

    private Collection<RecyclerViewAdapter.Item> load(int count) throws IOException {
        if (count > 10 || count <= 0)
            throw new IllegalArgumentException("Illegal count");
        HttpURLConnection connection = (HttpURLConnection) new URL(url.concat("?limit=").concat(Integer.toString(count))).openConnection();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        final StringBuilder builder = new StringBuilder();
        String s;
        while ((s = reader.readLine()) != null)
            builder.append(s).append("\n");
        final String response = builder.toString();
        ArrayList<Object> list = g.fromJson(response, new TypeToken<ArrayList<Object>>(){}.getType());
        final ArrayList<RecyclerViewAdapter.Item> items = new ArrayList<>();
        for (Object o : list)
            items.add(new RecyclerViewAdapter.Item((String) ((HashMap<String, Object>) g.fromJson(g.toJson(o), new TypeToken<HashMap<String, Object>>(){}.getType())).get("url")));
        for (RecyclerViewAdapter.Item item : items)
            Log.d("api", item.getUrl());
        return items;
    }
}
