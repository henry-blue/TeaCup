package com.app.fragment;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.app.adapter.PhotoRecyclerAdapter;
import com.app.teacup.R;
import com.app.teacup.ShowPhotoActivity;
import com.app.util.HttpUtils;
import com.app.util.OkHttpUtils;
import com.jcodecraeer.xrecyclerview.XRecyclerView;
import com.squareup.okhttp.Request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class JiandanMeiziFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final int REFRESH_START = 0;
    private static final int REFRESH_FINISH = 1;
    private static final int REFRESH_ERROR = 2;
    private static final int LOAD_DATA_FINISH = 3;
    private static final int LOAD_DATA_ERROR = 4;

    private List<String> mImgUrl;
    private SwipeRefreshLayout mRefreshLayout;
    private XRecyclerView mRecyclerView;
    private int mainPageId = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_START:
                    startRefreshData();
                    break;
                case REFRESH_FINISH:
                    mRefreshLayout.setRefreshing(false);
                    mPhotoRecyclerAdapter.reSetData(mImgUrl);
                    break;
                case REFRESH_ERROR:
                    mRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "刷新失败, 请检查网络", Toast.LENGTH_SHORT).show();
                    break;
                case LOAD_DATA_FINISH:
                    mRecyclerView.loadMoreComplete();
                    mPhotoRecyclerAdapter.reSetData(mImgUrl);
                    break;
                case LOAD_DATA_ERROR:
                    Toast.makeText(getContext(), "刷新失败, 请检查网络", Toast.LENGTH_SHORT).show();
                    mRecyclerView.loadMoreComplete();
                    break;
                default:
                    break;
            }
        }
    };
    private PhotoRecyclerAdapter mPhotoRecyclerAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mImgUrl = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_fragment, container, false);
        initView(view);
        setupRecycleView();
        setupRefreshLayout();
        return view;
    }

    private void setupRefreshLayout() {
        mRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        mRefreshLayout.setProgressViewEndTarget(true, 100);
        mRefreshLayout.setOnRefreshListener(this);
        StartRefreshPage();
    }

    private void StartRefreshPage() {
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(true);
                startRefreshData();
            }
        });
    }

    private void setupRecycleView() {
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setPullRefreshEnabled(false);

        mRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
            }

            @Override
            public void onLoadMore() {
                startLoadData();
            }
        });
        mPhotoRecyclerAdapter = new PhotoRecyclerAdapter(getContext(),
                mImgUrl);
        mRecyclerView.setAdapter(mPhotoRecyclerAdapter);

        mPhotoRecyclerAdapter.setOnItemClickListener(new PhotoRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String url = mImgUrl.get(position);
                if (!TextUtils.isEmpty(url)) {
                    Intent intent = new Intent(getContext(), ShowPhotoActivity.class);
                    intent.putExtra("ImageUrl", url);
                    startActivity(intent);
                }
            }
        });
    }

    private void initView(View view) {
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_refresh);
        mRecyclerView = (XRecyclerView) view.findViewById(R.id.base_recycler_view);
    }

    private void startRefreshData() {
        mImgUrl.clear();
        String url = "http://jandan.net/ooxx";
//        HttpUtils.sendHttpRequest(url, new HttpUtils.HttpCallBackListener() {
//            @Override
//            public void onFinish(String response) {
//                parsePhotoData(response);
//                sendParseDataMessage(REFRESH_FINISH);
//            }
//
//            @Override
//            public void onError(Exception e) {
//                sendParseDataMessage(REFRESH_ERROR);
//            }
//        });

        OkHttpUtils.getAsyn(url, new OkHttpUtils.ResultCallback<String>() {

            @Override
            public void onError(Request request, Exception e) {
                sendParseDataMessage(REFRESH_ERROR);
            }

            @Override
            public void onResponse(String response) {
                parsePhotoData(response);
                sendParseDataMessage(REFRESH_FINISH);
            }
        });
    }

    private void startLoadData() {
        mainPageId--;
        String url = "http://jandan.net/ooxx/page-/" + mainPageId + "#comments";
        HttpUtils.sendHttpRequest(url, new HttpUtils.HttpCallBackListener() {
            @Override
            public void onFinish(String response) {
                parsePhotoData(response);
                sendParseDataMessage(LOAD_DATA_FINISH);
            }

            @Override
            public void onError(Exception e) {
                sendParseDataMessage(LOAD_DATA_ERROR);
            }
        });
    }

    public void sendParseDataMessage(int message) {
        Message msg = Message.obtain();
        msg.what = message;
        mHandler.sendMessage(msg);
    }

    private void parsePhotoData(String response) {
        Document document = Jsoup.parse(response);
        Elements commentlist = document.getElementsByClass("commentlist");
        for (Element element : commentlist) {
            Elements li = element.getElementsByTag("li");
            for (Element li1 : li) {
                Elements a = li1.getElementsByTag("a");
                for (Element a1 : a) {
                    String imgUrl = a1.attr("href");
                    if (imgUrl.contains(".jpg")) {
                        mImgUrl.add(imgUrl);
                    }
                }
            }
        }
        if (mainPageId == -1) {
            Elements comments = document.getElementsByClass("comments");
            for (Element current : comments) {
                Elements currentPage = current.getElementsByClass("current-comment-page");
                String text = currentPage.text();
                if (!TextUtils.isEmpty(text)) {
                    String subStr = text.substring(1, text.length() - 1);
                    Log.i("Jiandan", "====subStr===");
                    mainPageId = Integer.valueOf(subStr);
                }
            }
        }
    }

    @Override
    public void onRefresh() {
        mainPageId = -1;
        Message msg = Message.obtain();
        msg.what = REFRESH_START;
        mHandler.sendMessage(msg);
    }
}