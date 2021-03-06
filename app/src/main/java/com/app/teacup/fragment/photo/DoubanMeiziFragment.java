package com.app.teacup.fragment.photo;


import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.app.teacup.R;
import com.app.teacup.ShowPhotoListActivity;
import com.app.teacup.adapter.PhotoRecyclerAdapter;
import com.app.teacup.fragment.BaseFragment;
import com.app.teacup.util.urlUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class DoubanMeiziFragment extends BaseFragment {

    private ArrayList<String> mImageUrls;
    private PhotoRecyclerAdapter mPhotoRecyclerAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mImageUrls = new ArrayList<>();
        mRequestUrl = urlUtils.DOUBAN_MEINV_URL;
    }

    @Override
    protected void onRecycleViewResponseLoadMore() {
        if (mImageUrls.size() <= 0) {
            mRecyclerView.loadMoreComplete();
        } else {
            startLoadData(urlUtils.DOUBAN_MEINV_NEXT_URL, 50);
        }
    }

    @Override
    protected void setupRecycleViewAndAdapter() {
        final StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2,
                StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        mRecyclerView.setLayoutManager(layoutManager);

        int itemSpace = getResources().
                getDimensionPixelSize(R.dimen.item_photo_view_item_margin);
        mPhotoRecyclerAdapter = new PhotoRecyclerAdapter(getContext(), mImageUrls);
        mPhotoRecyclerAdapter.setHasStableIds(true);
        mRecyclerView.addItemDecoration(mPhotoRecyclerAdapter.new SpaceItemDecoration(itemSpace));
        mRecyclerView.setAdapter(mPhotoRecyclerAdapter);

        mPhotoRecyclerAdapter.setOnItemClickListener(new PhotoRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String url = mImageUrls.get(position);
                if (!TextUtils.isEmpty(url)) {
                    Intent intent = new Intent(getContext(), ShowPhotoListActivity.class);
                    intent.putStringArrayListExtra("photoList", mImageUrls);
                    intent.putExtra("photoPos", position);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void parseData(String response) {
        Document document = Jsoup.parse(response);
        try {
            Element main = document.getElementById("main");
            Elements liElements = main.getElementsByTag("li");
            for (Element element : liElements) {

                Elements aElements = element.getElementsByTag("a");
                for (Element a : aElements) {
                    Elements height_min = a.getElementsByClass("height_min");
                    for (Element height : height_min) {
                        String url = height.attr("src");
                        if (url.contains(".jpg")) {
                            mImageUrls.add(url);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "DoubanMeiziFragment::parseData: ==error==" + e.getMessage());
        }
    }

    @Override
    protected void startRefreshData() {
        mImageUrls.clear();
        super.startRefreshData();
    }

    @Override
    protected void onLoadDataFinish() {
        super.onLoadDataFinish();
        mPhotoRecyclerAdapter.reSetData(mImageUrls);
    }

    @Override
    protected void onRefreshFinish() {
        super.onRefreshFinish();
        mPhotoRecyclerAdapter.refreshData(mImageUrls);
    }
}
