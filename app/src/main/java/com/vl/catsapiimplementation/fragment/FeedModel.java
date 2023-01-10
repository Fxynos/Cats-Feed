package com.vl.catsapiimplementation.fragment;

import androidx.lifecycle.ViewModel;

import com.vl.catsapiimplementation.adapter.Adapter;

import java.util.ArrayList;

public class FeedModel extends ViewModel {
    final private ArrayList<Adapter.Item> feedItems = new ArrayList<>();

    public ArrayList<Adapter.Item> getFeedItems() {
        return feedItems;
    }
}
