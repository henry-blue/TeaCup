package com.app.teacup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.app.bean.fanju.FanjuVideoInfo;
import com.app.ui.MoreTextView;
import com.app.util.OkHttpUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.squareup.okhttp.Request;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import hb.xvideoplayer.MxVideoPlayer;
import hb.xvideoplayer.MxVideoPlayerWidget;


public class FanjuVideoActivity extends BaseActivity {

    private List<FanjuVideoInfo> mDatas;
    private LinearLayout mVideoContainer;
    private SwipeRefreshLayout mRefreshLayout;
    private MxVideoPlayerWidget mxVideoPlayerWidget;
    private String mVideoPlayUrl;
    private TextView mXiangGuanText;
    private MoreTextView mContentView;
    private String mVideoContent = "";
    private TextView mVideoIntroduce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_fanjuvideo_view);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        initView();
        setupRefreshLayout();
    }

    @Override
    protected void onLoadDataError() {
        Toast.makeText(FanjuVideoActivity.this, getString(R.string.not_have_more_data),
                Toast.LENGTH_SHORT).show();
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onLoadDataFinish() {
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
        String videoUrl = getIntent().getStringExtra("fanjuVideoUrl");
        if (!TextUtils.isEmpty(videoUrl)) {
            OkHttpUtils.getAsyn(videoUrl, new OkHttpUtils.ResultCallback<String>() {

                @Override
                public void onError(Request request, Exception e) {
                    sendParseDataMessage(LOAD_DATA_ERROR);
                }

                @Override
                public void onResponse(String response) {
                    parseData(response);
                    sendParseDataMessage(LOAD_DATA_FINISH);
                }
            });
        }

    }

    private void parseData(String response) {
        Document document = Jsoup.parse(response);
        if (document != null) {
            Element userVideo = document.getElementsByClass("user_video").get(0);
            Element videoFrame = userVideo.getElementsByClass("video_frame").get(0);
            Element danMu = videoFrame.getElementsByClass("danmu-div").get(0);
            mVideoPlayUrl = danMu.getElementsByTag("video").get(0).attr("src");

            Element videoSection = document.getElementsByClass("video_section").get(0);
            Element contentVideo = videoSection.getElementsByClass("content_video").get(0);
            Element shareVideo = contentVideo.getElementsByClass("share_video").get(0);
            Element intrVideo = shareVideo.getElementsByClass("intr_video").get(0);
            Elements pElements = intrVideo.getElementsByTag("p");
            for (Element p : pElements) {
                mVideoContent += p.text() + "\n";
            }

            Element recVideo = contentVideo.getElementById("rec_video");
            Element replyVideo = recVideo.getElementsByClass("reply_video").get(0);
            Elements lis = replyVideo.getElementsByTag("ul").get(0).getElementsByTag("li");
            for (Element li : lis) {
                FanjuVideoInfo info = new FanjuVideoInfo();
                Element a = li.getElementsByTag("a").get(0);
                String nextUrl = "http://www.diyidan.com" + a.attr("href");
                Element img = a.getElementsByTag("img").get(0);
                String title = img.attr("alt");
                String imgUrl = img.attr("src");
                info.setNextUrl(nextUrl);
                info.setVideoName(title);
                info.setImgeUrl(imgUrl);
                mDatas.add(info);
            }
        }
    }

    private void initData() {
        if (mDatas.isEmpty() && TextUtils.isEmpty(mVideoPlayUrl)) {
            Toast.makeText(FanjuVideoActivity.this, getString(R.string.refresh_net_error),
                    Toast.LENGTH_SHORT).show();
        } else {
            mRefreshLayout.setEnabled(false);
        }
        if (!TextUtils.isEmpty(mVideoPlayUrl)) {
            mxVideoPlayerWidget.startPlay(mVideoPlayUrl, MxVideoPlayer.SCREEN_LAYOUT_NORMAL,
                    getIntent().getStringExtra("fanjuVideoName"));
            mxVideoPlayerWidget.mStartButton.performClick();
            mxVideoPlayerWidget.setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,
                    View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
        }

        if (!TextUtils.isEmpty(mVideoContent)) {
            mVideoIntroduce.setVisibility(View.VISIBLE);
            mContentView.setVisibility(View.VISIBLE);
            mContentView.setContent(mVideoContent);
        }

        if (!mDatas.isEmpty()) {
            mXiangGuanText.setVisibility(View.VISIBLE);
            addVideoInfoToContainer();
        }
    }

    private void addVideoInfoToContainer() {
        for (final FanjuVideoInfo info : mDatas) {
            View view = View.inflate(FanjuVideoActivity.this, R.layout.item_fanju_video_view, null);
            TextView videoName = (TextView) view.findViewById(R.id.fanju_video_name);
            videoName.setText(info.getVideoName());
            ImageView videoImg = (ImageView) view.findViewById(R.id.fanju_video_img);
            loadImageResource(videoImg, info.getImgeUrl());
            mVideoContainer.addView(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(FanjuVideoActivity.this, FanjuVideoActivity.class);
                        intent.putExtra("fanjuVideoUrl", info.getNextUrl());
                        intent.putExtra("fanjuVideoName", info.getVideoName());
                        startActivity(intent);
                }
            });
        }
    }

    private void loadImageResource(ImageView videoImg, String imgUrl) {
        if (!MainActivity.mIsLoadPhoto) {
            Glide.with(this).load(imgUrl).asBitmap()
                    .error(R.drawable.photo_loaderror)
                    .placeholder(R.drawable.main_load_bg)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .into(videoImg);
        } else {
            if (MainActivity.mIsWIFIState) {
                Glide.with(this).load(imgUrl).asBitmap()
                        .error(R.drawable.photo_loaderror)
                        .placeholder(R.drawable.main_load_bg)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .into(videoImg);
            } else {
                videoImg.setImageResource(R.drawable.main_load_bg);
            }
        }
    }

    private void initView() {
        mDatas = new ArrayList<>();
        mVideoIntroduce = (TextView) findViewById(R.id.tv_video_intr);
        mContentView = (MoreTextView) findViewById(R.id.fanjuvideo_content);
        mVideoContainer = (LinearLayout) findViewById(R.id.fanju_video_container);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_refresh);
        mxVideoPlayerWidget = (MxVideoPlayerWidget) findViewById(R.id.fanju_video_player);
        mxVideoPlayerWidget.setAllControlsVisible(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                View.INVISIBLE, View.VISIBLE, View.INVISIBLE);

        mXiangGuanText = (TextView) findViewById(R.id.xiangguan_textview);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MxVideoPlayer.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if (MxVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }
}