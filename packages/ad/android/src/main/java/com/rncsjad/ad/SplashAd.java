package com.rncsjad.ad;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.CSJAdError;
import com.bytedance.sdk.openadsdk.CSJSplashAd;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.rncsjad.R;
import com.rncsjad.options.CsjLoadSplashAdOption;
import com.rncsjad.utils.EventHelper;
import com.rncsjad.utils.LogUtils;
import com.rncsjad.utils.UIUtils;

import java.lang.ref.WeakReference;

public class SplashAd {
  private Dialog mSplashDialog;
  private WeakReference<Activity> mActivity;
  private static final String TAG = LogUtils.createLogTag("SplashAd");
  private final ReactApplicationContext mContext;
  private final EventHelper mEventHelper;

  public SplashAd(ReactApplicationContext reactContext) {
    this.mContext = reactContext;
    this.mEventHelper = new EventHelper(reactContext, "SplashAd");
  }

  public void show(View splashView) {
    Activity activity = mContext.getCurrentActivity();
    if (activity == null) return;
    mActivity = new WeakReference<Activity>(activity);
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!activity.isFinishing()) {
          mSplashDialog = new Dialog(activity, R.style.FullScreenDialogTheme);
          mSplashDialog.setContentView(splashView);
          mSplashDialog.setCancelable(false);
          Window window = mSplashDialog.getWindow();
          window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
          window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
          View decorView = window.getDecorView();
          decorView.setPadding(0, 0, 0, 0);
          decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
          WindowManager.LayoutParams layoutParams = window.getAttributes();
          layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
          layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
          window.setAttributes(layoutParams);
          setActivityAndroidP(mSplashDialog);
          if (!mSplashDialog.isShowing()) {
            mSplashDialog.show();
          }
        }
      }
    });
  }

  public void hide() {
    Activity activity = mContext.getCurrentActivity();
    if (activity == null) {
      if (mActivity == null) {
        return;
      }
      activity = mActivity.get();
    }

    if (activity == null) return;

    final Activity _activity = activity;

    _activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (mSplashDialog != null && mSplashDialog.isShowing()) {
          boolean isDestroyed = false;

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isDestroyed = _activity.isDestroyed();
          }

          if (!_activity.isFinishing() && !isDestroyed) {
            mSplashDialog.dismiss();
          }
          mSplashDialog = null;
        }
      }
    });
  }

  public void loadSplashAd(CsjLoadSplashAdOption option, Promise promise) {
    TTAdNative adNativeLoader = TTAdSdk.getAdManager().createAdNative(mContext.getCurrentActivity());
    adNativeLoader.loadSplashAd(buildSplashAdslot(option.code), new NativeSplashAdListener(promise), option.timeout);
  }

  //展示开屏广告
  private void showSplashAd(CSJSplashAd splashAd) {
    if (splashAd == null) {
      return;
    }

    splashAd.setSplashAdListener(new SplashAdListener());
    show(splashAd.getSplashView());
  }

  // 构造开屏广告的Adslot
  private AdSlot buildSplashAdslot(String code) {
    return new AdSlot.Builder()
      .setCodeId(code) //广告位ID
      .setImageAcceptedSize(UIUtils.getScreenWidthInPx(mContext), UIUtils.getScreenHeight(mContext))  //设置广告宽高 单位px
      .setExpressViewAcceptedSize(UIUtils.getScreenWidthDp(mContext), UIUtils.px2dip(mContext, UIUtils.getScreenHeight(mContext)))
      .build();
  }

  private void setActivityAndroidP(Dialog dialog) {
    //设置全屏展示
    if (Build.VERSION.SDK_INT >= 28) {
      if (dialog != null && dialog.getWindow() != null) {
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);//全屏显示
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        dialog.getWindow().setAttributes(lp);
      }
    }
  }

  private class NativeSplashAdListener implements TTAdNative.CSJSplashAdListener {
    private final Promise mPromise;

    public NativeSplashAdListener(Promise promise) {
      this.mPromise = promise;
    }

    @Override
    public void onSplashLoadSuccess() {
      Log.d(TAG, "广告加载成功");
      mEventHelper.sendEvent("onSplashLoadSuccess", null);
    }

    @Override
    public void onSplashLoadFail(CSJAdError csjAdError) {
      //广告加载失败
      Log.d(TAG, "广告加载失败: " + csjAdError.getCode() + csjAdError.getMsg());
      mPromise.reject(String.valueOf(csjAdError.getCode()), csjAdError.getMsg());

      WritableMap params = Arguments.createMap();
      params.putInt("code", csjAdError.getCode());
      params.putString("message", csjAdError.getMsg());
      mEventHelper.sendEvent("onSplashLoadFail", params);
    }

    @Override
    public void onSplashRenderSuccess(CSJSplashAd csjSplashAd) {
      //广告渲染成功，在此展示广告
      Log.d(TAG, "广告渲染成功");
      showSplashAd(csjSplashAd);
      mPromise.resolve(null);
      mEventHelper.sendEvent("onSplashRenderSuccess", null);
    }

    @Override
    public void onSplashRenderFail(CSJSplashAd csjSplashAd, CSJAdError csjAdError) {
      //广告渲染失败
      Log.d(TAG, "广告渲染失败");
      mPromise.reject(String.valueOf(csjAdError.getCode()), csjAdError.getMsg());

      WritableMap params = Arguments.createMap();
      params.putInt("code", csjAdError.getCode());
      params.putString("message", csjAdError.getMsg());
      mEventHelper.sendEvent("onSplashRenderFail", params);
    }
  }

  private class SplashAdListener implements CSJSplashAd.SplashAdListener {
    @Override
    public void onSplashAdShow(CSJSplashAd csjSplashAd) {
      mEventHelper.sendEvent("onSplashAdShow", null);
    }

    @Override
    public void onSplashAdClick(CSJSplashAd csjSplashAd) {
      //广告点击
      mEventHelper.sendEvent("onSplashAdClick", null);
    }

    /**
     * 广告关闭
     * @param csjSplashAd
     * @param closeType
     */
    @Override
    public void onSplashAdClose(CSJSplashAd csjSplashAd, int closeType) {
      hide();

      WritableMap params = Arguments.createMap();
      params.putInt("closeType", closeType);
      mEventHelper.sendEvent("onSplashAdClose", params);
    }
  }
}
