package com.vl.catsapiimplementation.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class FeedItem extends Adapter.Item {
    final private String url, id;

    public FeedItem(String url, String id) {
        this.url = url;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public Bitmap load() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            return BitmapFactory.decodeStream(connection.getInputStream());
        } catch (IOException e) {
            return null;
        }
    }
}
