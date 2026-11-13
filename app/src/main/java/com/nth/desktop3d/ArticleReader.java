package com.nth.desktop3d;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;

/** Publisher pages remain on their original HTTPS origins, with no JavaScript bridge. */
@SuppressLint("ViewConstructor")
final class ArticleReader extends LinearLayout {
    final WebView web;private final TextView origin,error;private final ProgressBar progress;
    @SuppressLint("SetJavaScriptEnabled")
    ArticleReader(Activity activity,String url,Runnable close){
        super(activity);setOrientation(VERTICAL);setTag("feed-reader");
        LinearLayout tools=Ui.row(activity);tools.addView(Ui.quiet(activity,"‹ Feed",close),new LayoutParams(0,-2,1));tools.addView(Ui.quiet(activity,"Reload",()->webReload()),new LayoutParams(0,-2,1));tools.addView(Ui.quiet(activity,"Browser",()->{String current=webUrl();if(current.isEmpty())return;try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(current)));}catch(RuntimeException missing){browserMissing();}}),new LayoutParams(0,-2,1));addView(tools);
        origin=Ui.text(activity,"Publisher · "+Uri.parse(url).getHost(),12,Ui.MUTED);origin.setSingleLine(true);origin.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);addView(origin,new LayoutParams(-1,Ui.dp(activity,32)));
        progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);addView(progress,new LayoutParams(-1,Ui.dp(activity,3)));
        error=Ui.text(activity,"",13,Ui.ACCENT);error.setVisibility(GONE);addView(error);
        web=new WebView(activity);web.setTag("feed-webview");web.setBackgroundColor(Ui.PANEL);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSafeBrowsingEnabled(true);settings.setSupportMultipleWindows(false);settings.setJavaScriptCanOpenWindowsAutomatically(false);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView view,int value){progress.setProgress(value);progress.setVisibility(value==100?GONE:VISIBLE);}});
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return FeedParser.https(request.getUrl().toString(),request.getUrl().toString()).isEmpty();}
            @Override public void onPageStarted(WebView view,String address,android.graphics.Bitmap icon){origin.setText("Publisher · "+Uri.parse(address).getHost());error.setVisibility(GONE);}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError failure){if(request.isForMainFrame())failure();}
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){if(request.isForMainFrame())failure();}
            private void failure(){error.setText("This publisher page could not load. Check your connection, reload, or try Browser.");error.setVisibility(VISIBLE);progress.setVisibility(GONE);}
        });addView(web,new LayoutParams(-1,0,1));web.loadUrl(url);
    }
    private void browserMissing(){error.setText("No browser is available. You can continue reading here.");error.setVisibility(VISIBLE);}
    private void webReload(){if(web!=null)web.reload();}private String webUrl(){String u=web==null?null:web.getUrl();return u==null?"":FeedParser.https(u,u);}
    boolean back(){if(web.canGoBack()){web.goBack();return true;}return false;}
    void pause(){web.onPause();}void resume(){web.onResume();}void dispose(){web.stopLoading();removeView(web);web.destroy();}
}
