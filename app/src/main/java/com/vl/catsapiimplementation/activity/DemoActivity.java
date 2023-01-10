package com.vl.catsapiimplementation.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vl.catsapiimplementation.R;
import com.vl.catsapiimplementation.fragment.DownloadsFragment;
import com.vl.catsapiimplementation.fragment.FeedFragment;

import java.io.File;
import java.util.Map;

public class DemoActivity extends AppCompatActivity {
    final private static String BUNDLE_PAGE_KEY = "page";
    final private static int LANDSCAPE_GRID_COLUMNS = 3;
    final private static File DOWNLOADS = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Cats Feed");
    final private static Map<Integer, Fragment> fragments = Map.ofEntries(
            Map.entry(R.id.feed, new FeedFragment()),
            Map.entry(R.id.downloads, new DownloadsFragment())
    );
    private BottomNavigationView bottomBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_layout);
        if ((!DOWNLOADS.exists() || !DOWNLOADS.isDirectory()) && !DOWNLOADS.mkdir())
            Toast.makeText(this, String.format("Couldn't create %s directory", DOWNLOADS.getAbsolutePath()), Toast.LENGTH_SHORT).show();
        navigate(savedInstanceState == null ? R.id.feed : savedInstanceState.getInt(BUNDLE_PAGE_KEY));
        (bottomBar = findViewById(R.id.navigation)).setOnItemSelectedListener((item) -> {
            navigate(item.getItemId());
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_PAGE_KEY, bottomBar.getSelectedItemId());
    }

    public void navigate(@IdRes int menuItem) {
        final Fragment fragment = fragments.get(menuItem);
        if (fragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.placeholder, fragment).commit();
    }

    public static File getDownloads() {
        return DOWNLOADS;
    }

    public RecyclerView.LayoutManager obtainRecyclerLayoutManagerForSpecificOrientation() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ?
                new GridLayoutManager(this, LANDSCAPE_GRID_COLUMNS) :
                new LinearLayoutManager(this);
    }
}
