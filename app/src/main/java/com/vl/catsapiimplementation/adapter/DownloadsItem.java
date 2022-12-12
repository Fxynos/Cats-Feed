package com.vl.catsapiimplementation.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DownloadsItem extends Adapter.Item {
    final private File file;

    public DownloadsItem(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    protected Bitmap load() {
        try {
            return BitmapFactory.decodeStream(new FileInputStream(file));
        } catch (IOException e) {
            return null;
        }
    }
}
