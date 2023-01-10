package com.vl.catsapiimplementation.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.vl.catsapiimplementation.R;
import com.vl.catsapiimplementation.activity.DemoActivity;
import com.vl.catsapiimplementation.adapter.Adapter;
import com.vl.catsapiimplementation.adapter.DownloadsItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DownloadsFragment extends Fragment implements Adapter.OnClickListener {
    private Adapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        assert(getActivity() != null);
        final View root = inflater.inflate(R.layout.fragment_downloads, parent, false);
        final File[] downloads = DemoActivity.getDownloads().listFiles();
        adapter = new Adapter(
                getContext(),
                new ArrayList<>(downloads == null ? List.of() : Arrays.stream(downloads).map(DownloadsItem::new).collect(Collectors.toList()))
        );
        final RecyclerView list = root.findViewById(R.id.downloadsList);
        list.setLayoutManager(((DemoActivity) getActivity()).obtainRecyclerLayoutManagerForSpecificOrientation());
        list.setAdapter(adapter);
        adapter.notifyItemRangeInserted(0, adapter.getItemCount());
        adapter.setOnClickListener(this);
        adapter.setButtonIconResource(R.drawable.ic_delete);
        return root;
    }

    @Override
    public void onClick(View view, int position) {
        final DownloadsItem item = (DownloadsItem) adapter.getItems().remove(position);
        adapter.notifyItemRemoved(position);
        if (!item.getFile().delete())
            Toast.makeText(getContext(), String.format("Couldn't delete %s", item.getFile().getAbsolutePath()), Toast.LENGTH_SHORT).show();
    }
}
