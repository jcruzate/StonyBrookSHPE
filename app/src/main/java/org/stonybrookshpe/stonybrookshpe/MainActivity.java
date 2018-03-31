package org.stonybrookshpe.stonybrookshpe;


import android.Manifest;

import android.annotation.SuppressLint;

import android.app.Activity;

import android.content.Context;

import android.graphics.Bitmap;

import android.content.Intent;

import android.content.pm.PackageManager;

import android.net.ConnectivityManager;

import android.net.Uri;

import android.os.Build;

import android.os.Bundle;

import android.os.Environment;

import android.provider.MediaStore;

import android.support.annotation.NonNull;

import android.support.annotation.Nullable;

import android.support.design.widget.CoordinatorLayout;

import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;

import android.support.v7.app.AppCompatActivity;

import android.util.Log;

import android.view.KeyEvent;

import android.view.View;

import android.webkit.ValueCallback;

import android.webkit.WebChromeClient;

import android.webkit.WebSettings;

import android.webkit.WebView;

import android.webkit.WebViewClient;

import java.io.File;

import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;

import com.onesignal.OneSignal;

import android.support.v4.widget.SwipeRefreshLayout;

import static java.security.AccessController.getContext;


public class MainActivity extends AppCompatActivity {


    ConnectivityManager cM;

    private WebView webView;

    private CoordinatorLayout coordinatorLayout;

    private static final String TAG = MainActivity.class.getSimpleName();

    private String mCM;

    private ValueCallback<Uri> mUM;

    private ValueCallback<Uri[]> mUMA;

    private final static int FCR = 1;


    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (Build.VERSION.SDK_INT >= 21) {

            Uri[] results = null;

            //Check if response is positive

            if (resultCode == Activity.RESULT_OK) {

                if (requestCode == FCR) {

                    if (null == mUMA) {

                        return;

                    }

                    if (intent == null || intent.getData() == null) {

                        //Capture Photo if no image available

                        if (mCM != null) {

                            results = new Uri[]{Uri.parse(mCM)};

                        }

                    } else {

                        String dataString = intent.getDataString();

                        if (dataString != null) {

                            results = new Uri[]{Uri.parse(dataString)};

                        }

                    }

                }

            }

            mUMA.onReceiveValue(results);

            mUMA = null;

        } else {

            if (requestCode == FCR) {

                if (null == mUM) return;

                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();

                mUM.onReceiveValue(result);

                mUM = null;

            }

        }

    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast"})

    @Override

    protected void onCreate(@Nullable Bundle savedInstanceState) {


        // OneSignal Initialization

        OneSignal.startInit(this)

                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)

                .unsubscribeWhenNotificationsAreDisabled(true)

                .init();

        OneSignal.setEmail("sbshpe@gmail.com");


        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 1);

        }


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.ifView);

        assert webView != null;

        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);

        webSettings.setAllowFileAccess(true);

        if (Build.VERSION.SDK_INT >= 21) {

            webSettings.setMixedContentMode(0);

            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        } else if (Build.VERSION.SDK_INT >= 19) {

            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        } else if (Build.VERSION.SDK_INT < 19) {

            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        }

        webView.setWebViewClient(new Callback());

        cM = (ConnectivityManager) this.getSystemService(Activity.CONNECTIVITY_SERVICE);

        if (cM != null && cM.getActiveNetworkInfo() != null && cM.getActiveNetworkInfo().isConnectedOrConnecting()) {

            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

            webView = (WebView) findViewById(R.id.ifView);

            if (savedInstanceState == null) {

                webView.loadUrl("http://stonybrookshpe.org");

            }

        } else {

            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

            if (savedInstanceState == null) {

                webView.loadUrl("http://stonybrookshpe.org");

            }

        }

        webView.setWebChromeClient(new WebChromeClient() {

            //For Android 3.0+

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {

                mUM = uploadMsg;

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                i.addCategory(Intent.CATEGORY_OPENABLE);

                i.setType("*/*");

                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);

            }

            // For Android 3.0+, above method not supported in some android 3+ versions, in such case we use this

            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {

                mUM = uploadMsg;

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                i.addCategory(Intent.CATEGORY_OPENABLE);

                i.setType("*/*");

                MainActivity.this.startActivityForResult(

                        Intent.createChooser(i, "File Browser"),

                        FCR);

            }

            //For Android 4.1+

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {

                mUM = uploadMsg;

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                i.addCategory(Intent.CATEGORY_OPENABLE);

                i.setType("*/*");

                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), MainActivity.FCR);

            }

            //For Android 5.0+

            public boolean onShowFileChooser(

                    WebView webView, ValueCallback<Uri[]> filePathCallback,

                    WebChromeClient.FileChooserParams fileChooserParams) {

                if (mUMA != null) {

                    mUMA.onReceiveValue(null);

                }

                mUMA = filePathCallback;

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {

                    File photoFile = null;

                    try {

                        photoFile = createImageFile();

                        takePictureIntent.putExtra("PhotoPath", mCM);

                    } catch (IOException ex) {

                        Log.e(TAG, "Image file creation failed", ex);

                    }

                    if (photoFile != null) {

                        mCM = "file:" + photoFile.getAbsolutePath();

                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));

                    } else {

                        takePictureIntent = null;

                    }

                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);

                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);

                contentSelectionIntent.setType("*/*");

                Intent[] intentArray;

                if (takePictureIntent != null) {

                    intentArray = new Intent[]{takePictureIntent};

                } else {

                    intentArray = new Intent[0];

                }


                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);

                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);

                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");

                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                startActivityForResult(chooserIntent, FCR);

                return true;

            }

        });

    }

    public class Callback extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith("mailto:")) {

                // We use `ACTION_SENDTO` instead of `ACTION_SEND` so that only email programs are launched.
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);

                // Parse the url and set it as the data for the `Intent`.
                emailIntent.setData(Uri.parse(url));

                // `FLAG_ACTIVITY_NEW_TASK` opens the email program in a new task instead as part of this application.
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Make it so.
                startActivity(emailIntent);

                return true;

            } else {

                if (Uri.parse(url).getHost().equals("stonybrookshpe.org")) {

                    return false;

                }

            }


            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            startActivity(intent);

            return true;


        }


        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            String html = "";

            String mime = "text/html";

            String encoding = "utf-8";

            //webview myWebView = (WebView)this.findViewById(R.id.ifView);

            webView.loadDataWithBaseURL(null, html, mime, encoding, null);

        }

    }


    // Create an image file

    private File createImageFile() throws IOException {

        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String imageFileName = "img_" + timeStamp + "_";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(imageFileName, ".jpg", storageDir);

    }

    @Override

    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        webView.saveState(outState);

    }

    @Override

    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);

        webView.restoreState(savedInstanceState);

    }

    @Override

    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            switch (keyCode) {

                case KeyEvent.KEYCODE_BACK:

                    if (webView.canGoBack()) {

                        webView.goBack();

                    } else {

                        finish();

                    }

                    return true;
            }

        }

        return super.onKeyDown(keyCode, event);

    }


}