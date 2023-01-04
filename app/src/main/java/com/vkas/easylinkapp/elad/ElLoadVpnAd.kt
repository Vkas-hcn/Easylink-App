package com.vkas.easylinkapp.elad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.easylinkapp.app.App
import com.vkas.easylinkapp.bean.ElAdBean
import com.vkas.easylinkapp.databinding.ActivityMainBinding
import com.vkas.easylinkapp.enevt.Constant.logTagEl
import com.vkas.easylinkapp.utils.EasyUtils
import com.vkas.easylinkapp.utils.EasyUtils.getAdServerDataEl
import com.vkas.easylinkapp.utils.EasyUtils.recordNumberOfAdClickEl
import com.vkas.easylinkapp.utils.EasyUtils.takeSortedAdIDEl
import com.vkas.easylinkapp.utils.KLog
import java.util.*

class ElLoadVpnAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadEl
    }

    object InstanceHelper {
        val openLoadEl = ElLoadVpnAd()
    }
    var appAdDataEl: NativeAd? = null

    // 是否正在加载中
    var isLoadingEl = false

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
        KLog.d(logTagEl, "vpn--isLoading=${isLoadingEl}")

        if (isLoadingEl) {
            KLog.d(logTagEl, "vpn--广告加载中，不能再次加载")
            return
        }
        if(appAdDataEl == null){
            isLoadingEl = true
            loadHomeAdvertisementEl(context,getAdServerDataEl())
        }
        if (appAdDataEl != null && !whetherAdExceedsOneHour(loadTimeEl)) {
            isLoadingEl = true
            appAdDataEl =null
            loadHomeAdvertisementEl(context,getAdServerDataEl())
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
     * 加载vpn原生广告
     */
    private fun loadHomeAdvertisementEl(context: Context,adData: ElAdBean) {
        val id = takeSortedAdIDEl(adIndexEl, adData.el_vpn)
        KLog.d(logTagEl, "vpn---原生广告id=$id;权重=${adData.el_vpn.getOrNull(adIndexEl)?.el_weight}")

        val vpnNativeAds = AdLoader.Builder(
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

        vpnNativeAds.withNativeAdOptions(adOptions)
        vpnNativeAds.forNativeAd {
            appAdDataEl = it
        }
        vpnNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoadingEl = false
                appAdDataEl = null
                KLog.d(logTagEl, "vpn---加载vpn原生加载失败: $error")

                if (adIndexEl < adData.el_vpn.size - 1) {
                    adIndexEl++
                    loadHomeAdvertisementEl(context,adData)
                }else{
                    adIndexEl = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagEl, "vpn---加载vpn原生广告成功")
                loadTimeEl = Date().time
                isLoadingEl = false
                adIndexEl = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagEl, "vpn---点击vpn原生广告")
                recordNumberOfAdClickEl()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

//    /**
//     * 设置展示vpn原生广告
//     */
//    fun setDisplayHomeNativeAdEl(activity: AppCompatActivity, binding: ActivityMainBinding) {
//        activity.runOnUiThread {
//            appAdDataEl.let {
//                KLog.d(logTagEl, "whetherToShow====>${whetherToShowEl}")
//                if (it != null && !whetherToShowEl&& activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
//                    val activityDestroyed: Boolean = activity.isDestroyed
//                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
//                        it.destroy()
//                        return@let
//                    }
//                    val adView = activity.layoutInflater
//                        .inflate(R.layout.layout_vpn_native, null) as NativeAdView
//                    // 对应原生组件
//                    setCorrespondingNativeComponentEl(it, adView)
//                    binding.elAdFrame.removeAllViews()
//                    binding.elAdFrame.addView(adView)
//                    binding.vpnAdEl = true
//                    recordNumberOfAdDisplaysEl()
//                    whetherToShowEl = true
//                    App.nativeAdRefreshEl = false
//                    appAdDataEl = null
//                    KLog.d(logTagEl, "vpn--原生广告--展示")
//                    //重新缓存
//                    advertisementLoadingEl(activity)
//                }
//            }
//
//        }
//    }
//
//    private fun setCorrespondingNativeComponentEl(nativeAd: NativeAd, adView: NativeAdView) {
//        adView.mediaView = adView.findViewById(R.id.ad_media)
//        // Set other ad assets.
//        adView.headlineView = adView.findViewById(R.id.ad_headline)
//        adView.bodyView = adView.findViewById(R.id.ad_body)
//        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
//        adView.iconView = adView.findViewById(R.id.ad_app_icon)
//        (adView.headlineView as TextView).text = nativeAd.headline
//        nativeAd.mediaContent?.let {
//            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
//                ?.setMediaContent(it)
//        }
//        adView.mediaView.clipToOutline=true
//        adView.mediaView.outlineProvider= RoundCornerOutlineProvider(5f)
//        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
//        // check before trying to display them.
//        if (nativeAd.body == null) {
//            adView.bodyView?.visibility = View.INVISIBLE
//        } else {
//            adView.bodyView?.visibility = View.VISIBLE
//            (adView.bodyView as TextView).text = nativeAd.body
//        }
//
//        if (nativeAd.callToAction == null) {
//            adView.callToActionView?.visibility = View.INVISIBLE
//        } else {
//            adView.callToActionView?.visibility = View.VISIBLE
//            (adView.callToActionView as TextView).text = nativeAd.callToAction
//        }
//
//        if (nativeAd.icon == null) {
//            adView.iconView?.visibility = View.GONE
//        } else {
//            (adView.iconView as ImageView).setImageDrawable(
//                nativeAd.icon?.drawable
//            )
//            adView.iconView?.visibility = View.VISIBLE
//        }
//
//        // This method tells the Google Mobile Ads SDK that you have finished populating your
//        // native ad view with this native ad.
//        adView.setNativeAd(nativeAd)
//    }
}