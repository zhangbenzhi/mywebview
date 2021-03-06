package cn.mkblog.www.mkbrowser.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.mkblog.www.mkbrowser.R;
import cn.mkblog.www.mkbrowser.entity.Weather;
import cn.mkblog.www.mkbrowser.utils.BrowseUtil;
import cn.mkblog.www.mkbrowser.utils.CollectionUtil;
import cn.mkblog.www.mkbrowser.utils.HttpDownloader;
import cn.mkblog.www.mkbrowser.utils.ImageUtil;
import cn.mkblog.www.mkbrowser.utils.RequestUtil;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 参考资料：
 * - https://www.zhihu.com/question/31316646
 * - https://github.com/Justson/AgentWeb
 * - https://juejin.im/post/58a037df86b599006b3fade4
 * - 视频播放问题 https://www.jianshu.com/p/d6d379e3f41d
 */
public class WebActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webView;
    private ProgressBar progressBar;
    private EditText textUrl;
    private ImageView webIcon, goBack, goForward, goHome, btnStart, collection, btnSign, browseIcon, download, btnUpdate,bg_iv,iv_weather;
    private TextView tv_wendu,baidu,jingdong,taobao;
    private int[] backResouce = new int[]{R.drawable.bg01, R.drawable.bg03, R.drawable.bg04, R.drawable.bg02};

    private long exitTime = 0;

    private Context mContext;
    private InputMethodManager manager;
    private String nowUrl;

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final int PRESS_BACK_EXIT_GAP = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 防止底部按钮上移
        getWindow().setSoftInputMode
                (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_web);

        mContext = WebActivity.this;
        manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        nowUrl = getResources().getString(R.string.home_url);
        // 绑定控件
        initView();
        // 初始化 WebView
        initWeb();
        getPemmsion();//请求权限
        requestWeather();//獲取天氣
    }

    private void requestWeather() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = RequestUtil.getWeater("北京");
                final List<Weather> list = RequestUtil.parseWeather(json);
                // 运行在主线程：
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (list == null) {
                            Toast.makeText(WebActivity.this, "获取网络数据异常！", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        if (list.size() > 0) {
                            ImageUtil.setImg(iv_weather, list.get(0).getState());
                            tv_wendu.setText(list.get(0).getTemperature());
                        }
                    }
                });
            }
        }).start();
    }

    private void getPemmsion() {
        new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Boolean aBoolean) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
    }

    /**
     * 绑定控件
     */
    private void initView() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        textUrl = findViewById(R.id.textUrl);
        webIcon = findViewById(R.id.webIcon);
        btnStart = findViewById(R.id.btnStart);
        goBack = findViewById(R.id.goBack);
        goForward = findViewById(R.id.goForward);
        goHome = findViewById(R.id.goHome);
        collection = findViewById(R.id.collection);
        btnSign = findViewById(R.id.btnSign);
        browseIcon = findViewById(R.id.browseIcon);
        download = findViewById(R.id.download);
        btnUpdate = findViewById(R.id.btnUpdate);
        bg_iv=findViewById(R.id.bg_iv);
        iv_weather=findViewById(R.id.iv_weather);
        tv_wendu=findViewById(R.id.tv_wendu);
        baidu=findViewById(R.id.baidu);
        jingdong=findViewById(R.id.jingdong);
        taobao=findViewById(R.id.taobao);

        // 绑定按钮点击事件
        btnStart.setOnClickListener(this);
        goBack.setOnClickListener(this);
        goForward.setOnClickListener(this);
        goHome.setOnClickListener(this);
        collection.setOnClickListener(this);
        btnSign.setOnClickListener(this);
        browseIcon.setOnClickListener(this);
        download.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
        baidu.setOnClickListener(this);
        jingdong.setOnClickListener(this);
        taobao.setOnClickListener(this);

        // 地址输入栏获取与失去焦点处理
        textUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    // 显示当前网址链接 TODO:搜索页面显示搜索词
                    textUrl.setText(webView.getUrl());
                    // 光标置于末尾
                    textUrl.setSelection(textUrl.getText().length());
                    // 显示因特网图标
                    webIcon.setImageResource(R.drawable.internet);
                    // 显示跳转按钮
                    btnStart.setImageResource(R.drawable.go);
                } else {
                    // 显示网站名
                    textUrl.setText(webView.getTitle());
                    // 显示网站图标
                    webIcon.setImageBitmap(webView.getFavicon());
                    // 显示刷新按钮
                    btnStart.setImageResource(R.drawable.refresh);
                }
            }
        });

        // 监听键盘回车搜索
        textUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    // 执行搜索
                    btnStart.callOnClick();
                    textUrl.clearFocus();
                }
                return false;
            }
        });
    }


    /**
     * 初始化 web
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWeb() {
        // 重写 WebViewClient
        webView.setWebViewClient(new MkWebViewClient());
        // 重写 WebChromeClient
        webView.setWebChromeClient(new MkWebChromeClient());

        WebSettings settings = webView.getSettings();
        // 启用 js 功能
        settings.setJavaScriptEnabled(true);
        // 设置浏览器 UserAgent
        settings.setUserAgentString(settings.getUserAgentString() + " mkBrowser/" + getVerName(mContext));

        // 将图片调整到适合 WebView 的大小
        settings.setUseWideViewPort(true);
        // 缩放至屏幕的大小
        settings.setLoadWithOverviewMode(true);

        // 支持缩放，默认为true。是下面那个的前提。
        settings.setSupportZoom(true);
        // 设置内置的缩放控件。若为false，则该 WebView 不可缩放
        settings.setBuiltInZoomControls(true);
        // 隐藏原生的缩放控件
        settings.setDisplayZoomControls(false);

        // 缓存
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 设置可以访问文件
        settings.setAllowFileAccess(true);
        // 支持通过JS打开新窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 支持自动加载图片
        settings.setLoadsImagesAutomatically(true);
        // 设置默认编码格式
        settings.setDefaultTextEncodingName("utf-8");
        // 本地存储
        settings.setDomStorageEnabled(true);
        settings.setPluginState(WebSettings.PluginState.ON);

        // 资源混合模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

    }


    /**
     * 重写 WebViewClient
     */
    private class MkWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // 设置在webView点击打开的新网页在当前界面显示,而不跳转到新的浏览器中

            if (url == null) {
                // 返回true自己处理，返回false不处理
                return true;
            }

            // 正常的内容，打开
            if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
                view.loadUrl(url);
                return true;
            }

            // 调用第三方应用，防止crash (如果手机上没有安装处理某个scheme开头的url的APP, 会导致crash)
            try {
                // TODO:弹窗提示用户，允许后再调用
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivityForResult(intent, 100);
                return true;
            } catch (Exception e) {
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // 网页开始加载，显示进度条
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);

            // 更新状态文字
            textUrl.setText("加载中...");

            // 切换默认网页图标
            webIcon.setImageResource(R.drawable.internet);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // 网页加载完毕，隐藏进度条
            progressBar.setVisibility(View.INVISIBLE);

            nowUrl = url;
            //添加浏览记录
            BrowseUtil.browse(WebActivity.this, url);
            // 改变标题
            setTitle(webView.getTitle());
            // 显示页面标题
            textUrl.setText(webView.getTitle());

            //设置收藏与取消收藏
            collection.setActivated(CollectionUtil.isCollected(WebActivity.this, url));
        }
    }


    /**
     * 重写 WebChromeClient
     */
    private class MkWebChromeClient extends WebChromeClient {
        private final static int WEB_PROGRESS_MAX = 100;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            // 加载进度变动，刷新进度条
            progressBar.setProgress(newProgress);
            if (newProgress > 0) {
                if (newProgress == WEB_PROGRESS_MAX) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);

            // 改变图标
            webIcon.setImageBitmap(icon);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // 改变标题
            setTitle(title);
            // 显示页面标题
            textUrl.setText(title);
        }
    }

    /**
     * 返回按钮处理
     */
    @Override
    public void onBackPressed() {
        // 能够返回则返回上一页
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if ((System.currentTimeMillis() - exitTime) > PRESS_BACK_EXIT_GAP) {
                // 连点两次退出程序
                Toast.makeText(mContext, "再按一次退出程序",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                super.onBackPressed();
            }

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // 跳转 或 刷新
            case R.id.btnStart:
                if (textUrl.hasFocus()) {
                    // 隐藏软键盘
                    if (manager.isActive()) {
                        manager.hideSoftInputFromWindow(textUrl.getApplicationWindowToken(), 0);
                    }

                    // 地址栏有焦点，是跳转
                    String input = textUrl.getText().toString();
                    if (!isHttpUrl(input)) {
                        // 不是网址，加载搜索引擎处理
                        try {
                            // URL 编码
                            input = URLEncoder.encode(input, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        input = "https://www.baidu.com/s?wd=" + input + "&ie=UTF-8";
                    }
                    webView.loadUrl(input);

                    // 取消掉地址栏的焦点
                    textUrl.clearFocus();
                } else {
                    // 地址栏没焦点，是刷新
                    webView.reload();
                }
                break;

            // 后退
            case R.id.goBack:
                webView.goBack();
                break;

            // 前进
            case R.id.goForward:
                webView.goForward();
                break;
             // 主页
            case R.id.goHome:
                webView.loadUrl(getResources().getString(R.string.home_url));
                break;

            //书签
            case R.id.collection:
                startActivityForResult(new Intent(this, MySignActivity.class), 100);
                break;
            case R.id.btnSign:
                CollectionUtil.collect(this, nowUrl);
                Toast.makeText(mContext, "添加书签成功", Toast.LENGTH_SHORT).show();
                break;
            //浏览历史
            case R.id.browseIcon:
                startActivityForResult(new Intent(this, MyBrowseActivity.class), 100);
                break;
            //下载
            case R.id.download:
                Toast.makeText(mContext, "正在下载中....", Toast.LENGTH_SHORT).show();
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                int code = new HttpDownloader().downloadFiles(nowUrl, System.currentTimeMillis() + "");
                                if (code == 1) {
                                    toast("文件已存在");
                                } else if (code == -1) {
                                    toast("文件下载失败");
                                } else if (code == 0) {
                                    toast("文件下载成功");
                                }
                            }
                        }
                ).start();
                break;
            case R.id.btnUpdate:
                bg_iv.setImageResource(backResouce[new Random().nextInt(4)]);
                break;
            case R.id.baidu:
                webView.loadUrl(v.getTag()+"");
                break;
            case R.id.jingdong:
                webView.loadUrl(v.getTag()+"");
                break;
            case R.id.taobao:
                webView.loadUrl(v.getTag()+"");
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            webView.getClass().getMethod("onPause").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            webView.getClass().getMethod("onResume").invoke(webView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 判断字符串是否为URL（https://blog.csdn.net/bronna/article/details/77529145）
     *
     * @param urls 要勘定的字符串
     * @return true:是URL、false:不是URL
     */
    public static boolean isHttpUrl(String urls) {
        boolean isUrl;
        // 判断是否是网址的正则表达式
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";

        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        isUrl = mat.matches();
        return isUrl;
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     * @return 当前版本名称
     */
    private static String getVerName(Context context) {
        String verName = "unKnow";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 100 && resultCode == 200) {
            String url = data.getStringExtra("url");
            if (!TextUtils.isEmpty(url)) {
                webView.loadUrl(url);
            }
        }
    }
}
