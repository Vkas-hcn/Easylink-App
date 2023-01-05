package com.vkas.easylinkapp.elad

import com.google.android.gms.ads.nativead.NativeAd
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.easylinkapp.app.App
import com.vkas.easylinkapp.enevt.Constant.logTagEl
import com.vkas.easylinkapp.utils.EasyUtils
import com.vkas.easylinkapp.utils.EasyUtils.recordNumberOfAdDisplaysEl
import com.vkas.easylinkapp.utils.KLog
import java.util.*
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.bean.ElAdBean
import com.vkas.easylinkapp.utils.EasyUtils.getAdServerDataEl
import com.vkas.easylinkapp.utils.EasyUtils.recordNumberOfAdClickEl
import com.vkas.easylinkapp.utils.EasyUtils.takeSortedAdIDEl
import com.vkas.easylinkapp.utils.RoundCornerOutlineProvider

class ElLoadBackAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadEl
    }

    object InstanceHelper {
        val openLoadEl = ElLoadBackAd()
    }
    var appAdDataEl: NativeAd? = null

    // 是否正在加载中
    private var isLoadingEl = false

    //加载时间
    private var loadTimeEl: Long = Date().time

    // 是否展示
    var whetherToShowEl = false

    // openIndex
    var adIndexEl = 0


    /**
     * 广告加载前判断
     */
    fun advertisementLoadingEl(context: Context) {
        App.isAppOpenSameDayEl()
        if (EasyUtils.isThresholdReached()) {
            KLog.d(logTagEl, "广告达到上线")
            return
        }
        KLog.d(logTagEl, "isLoading=${isLoadingEl}")

        if (isLoadingEl) {
            KLog.d(logTagEl, "广告加载中，不能再次加载")
            return
        }

        if(appAdDataEl == null){
            isLoadingEl = true
            loadBackAdvertisementEl(context,getAdServerDataEl())
        }
        if (appAdDataEl != null && !whetherAdExceedsOneHour(loadTimeEl)) {
            isLoadingEl = true
            appAdDataEl =null
            loadBackAdvertisementEl(context,getAdServerDataEl())
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour
    }

    /**
     * 加载back原生广告
     */
    private fun loadBackAdvertisementEl(context: Context,adData: ElAdBean) {
        val id = takeSortedAdIDEl(adIndexEl, adData.el_back)
        KLog.d(logTagEl, "back---原生广告id=$id;权重=${adData.el_back.getOrNull(adIndexEl)?.el_weight}")

        val homeNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        homeNativeAds.withNativeAdOptions(adOptions)
        homeNativeAds.forNativeAd {
            appAdDataEl = it
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoadingEl = false
                appAdDataEl = null
                KLog.d(logTagEl, "back---加载back原生加载失败: $error")

                if (adIndexEl < adData.el_back.size - 1) {
                    adIndexEl++
                    loadBackAdvertisementEl(context,adData)
                }else{
                    adIndexEl = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagEl, "back---加载back原生广告成功")
                loadTimeEl = Date().time
                isLoadingEl = false
                adIndexEl = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagEl, "back---点击back原生广告")
                recordNumberOfAdClickEl()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示back原生广告
     */
    fun setDisplayBackNativeAdEl(activity: AppCompatActivity, view: ViewGroup,img:ImageView) {
        activity.runOnUiThread {
            appAdDataEl.let {
                if (it != null && !whetherToShowEl) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        it.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_back_native_el, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponentEl(it, adView)
                    view.removeAllViews()
                    view.addView(adView)
                    view.visibility =View.VISIBLE
                    img.visibility =View.GONE
                    recordNumberOfAdDisplaysEl()
                    whetherToShowEl = true
                    App.nativeAdRefreshEl = false
                    appAdDataEl = null
                    KLog.d(logTagEl, "back--原生广告--展示")
                    //重新缓存
                    advertisementLoadingEl(activity)
                }
            }
        }
    }

    private fun setCorrespondingNativeComponentEl(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        adView.mediaView.clipToOutline=true
        adView.mediaView.outlineProvider= RoundCornerOutlineProvider(8f)

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as TextView).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}