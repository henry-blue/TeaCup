package com.app.teacup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.app.adapter.ReadTopicRecyclerAdapter;
import com.app.bean.Read.ReadTopicInfo;
import com.app.util.HttpUtils;
import com.jcodecraeer.xrecyclerview.ProgressStyle;
import com.jcodecraeer.xrecyclerview.XRecyclerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;


public class ReadTopicActivity extends BaseActivity {

    private List<ReadTopicInfo> mDatas;
    private XRecyclerView mRecyclerView;
    private ReadTopicRecyclerAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_read_topic);
        initView();
        initToolBar();
        setupRefreshLayout();
    }

    @Override
    protected void onLoadDataError() {
        Toast.makeText(ReadTopicActivity.this, getString(R.string.not_have_more_data),
                Toast.LENGTH_SHORT).show();
        mRecyclerView.refreshComplete();
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onLoadDataFinish() {
        mRecyclerView.refreshComplete();
        mRefreshLayout.setRefreshing(false);
        initData();
    }

    @Override
    protected void onRefreshError() {

    }

    @Override
    protected void onRefreshFinish() {

    }

    @Override
    protected void onRefreshStart() {

    }

    private void setupRefreshLayout() {
        mRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        mRefreshLayout.setProgressViewEndTarget(true, 100);
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

    private void startRefreshData() {
        mDatas.clear();
        String readUrl = getIntent().getStringExtra("readTopicUrl");
        if (!TextUtils.isEmpty(readUrl)) {
            HttpUtils.sendHttpRequest(readUrl, new HttpUtils.HttpCallBackListener() {
                @Override
                public void onFinish(String response) {
                    parseData(response);
                    sendParseDataMessage(LOAD_DATA_FINISH);
                }

                @Override
                public void onError(Exception e) {
                    sendParseDataMessage(LOAD_DATA_ERROR);
                }
            });
        }

    }

    private void parseData(String response) {
        Document document = Jsoup.parse(response);
        if (document != null) {
            Element main = document.getElementsByClass("main").get(0);
            Element sDetailLeft = main.getElementsByClass("s_detail_left").get(0);
            Element mainCont = sDetailLeft.getElementsByClass("main_cont").get(0);
            Elements content = mainCont.getElementsByClass("create_content");
            for (Element e : content) {
                ReadTopicInfo info = new ReadTopicInfo();
                Element dCreateTop = e.getElementsByClass("d_create_top").get(0);
                Element heading = dCreateTop.getElementsByClass("heading").get(0).getElementsByTag("a").get(0);
                info.setTitle(heading.text());
                info.setNextUrl(heading.attr("href"));

                String fromNews = e.getElementsByClass("from_news").get(0).text();
                info.setDetail(fromNews);

                Element article = e.getElementsByClass("word_article").get(0);
                Element clearfix = article.getElementsByClass("clearfix").get(0);
                Elements byTag = clearfix.getElementsByTag("a");
                for (Element tag : byTag) {
                    String text = tag.text();
                    if (!TextUtils.isEmpty(text)) {
                        info.setContent(text + "\n");
                    } else {
                        Element img = tag.getElementsByTag("img").get(0);
                        if (img != null) {
                            String imgUrl = img.attr("src");
                            String[] split = imgUrl.split("!");
                            info.setImgUrl(split[0]);
                        }
                    }
                }
                mDatas.add(info);
            }
        }
    }

    private void initData() {
        if (mDatas.isEmpty()) {
            Toast.makeText(ReadTopicActivity.this, getString(R.string.screen_shield),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (mAdapter == null) {
                mAdapter = new ReadTopicRecyclerAdapter(ReadTopicActivity.this,
                        mDatas, 0);
                mRecyclerView.setAdapter(mAdapter);
                mAdapter.setOnItemClickListener(new ReadTopicRecyclerAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Intent intent = new Intent(ReadTopicActivity.this, ReadDetailActivity.class);
                        intent.putExtra("readTitle", mDatas.get(position).getTitle());
                        intent.putExtra("readDetailUrl", mDatas.get(position).getNextUrl());
                        startActivity(intent);
                    }
                });
            } else {
                mAdapter.reSetData(mDatas);
                mAdapter.notifyDataSetChanged();
            }

        }
    }

    private void initView() {
        mDatas = new ArrayList<>();
        mRecyclerView = (XRecyclerView) findViewById(R.id.base_recycler_view);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_refresh);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLoadingMoreEnabled(false);
        mRecyclerView.setRefreshProgressStyle(ProgressStyle.BallSpinFadeLoader);
        mRecyclerView.setLoadingMoreProgressStyle(ProgressStyle.BallSpinFadeLoader);
        mRecyclerView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                startRefreshData();
            }

            @Override
            public void onLoadMore() {
            }
        });
    }

    private void initToolBar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.activity_navigation_toolbar);
        if (mToolbar != null) {
            mToolbar.setTitle(getIntent().getStringExtra("readTitle"));
        }
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
