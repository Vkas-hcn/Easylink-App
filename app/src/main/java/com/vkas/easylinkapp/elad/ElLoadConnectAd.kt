package com.vkas.easylinkapp.elad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easylinkapp.app.App
import com.vkas.easylinkapp.bean.ElAdBean
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.enevt.Constant.logTagEl
import com.vkas.easylinkapp.utils.EasyUtils
import com.vkas.easylinkapp.utils.EasyUtils.getAdServerDataEl
import com.vkas.easylinkapp.utils.EasyUtils.recordNumberOfAdClickEl
import com.vkas.easylinkapp.utils.EasyUtils.recordNumberOfAdDisplaysEl
import com.vkas.easylinkapp.utils.EasyUtils.takeSortedAdIDEl
import com.vkas.easylinkapp.utils.KLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ElLoadConnectAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadEl
    }

    object InstanceHelper {
        val openLoadEl = ElLoadConnectAd()
    }

    var appAdDataEl: InterstitialAd? = null

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
        KLog.d(logTagEl, "connect--isLoading=${isLoadingEl}")

        if (isLoadingEl) {
            KLog.d(logTagEl, "connect--广告加载中，不能再次加载")
            return
        }

        if (appAdDataEl == null) {
            isLoadingEl = true
            loadConnectAdvertisementEl(context, getAdServerDataEl())
        }
        if (appAdDataEl != null && !whetherAdExceedsOneHour(loadTimeEl)) {
            isLoadingEl = true
            appAdDataEl = null
            loadConnectAdvertisementEl(context, getAdServerDataEl())
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
     * 加载首页插屏广告
     */
    private fun loadConnectAdvertisementEl(context: Context, adData: ElAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDEl(adIndexEl, adData.el_connect)
        KLog.d(
            logTagEl,
            "connect--插屏广告id=$id;权重=${adData.el_connect.getOrNull(adIndexEl)?.el_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagEl, "connect---连接插屏加载失败=$it") }
                    isLoadingEl = false
                    appAdDataEl = null
                    if (adIndexEl < adData.el_connect.size - 1) {
                        adIndexEl++
                        loadConnectAdvertisementEl(context, adData)
                    } else {
                        adIndexEl = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimeEl = Date().time
                    isLoadingEl = false
                    appAdDataEl = interstitialAd
                    adIndexEl = 0
                    KLog.d(logTagEl, "connect---连接插屏加载成功")
                }
            })
    }

    /**
     * connect插屏广告回调
     */
    private fun connectScreenAdCallback() {
        appAdDataEl?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagEl, "connect插屏广告点击")
                    recordNumberOfAdClickEl()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagEl, "关闭connect插屏广告=${App.isBackDataEl}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_EL_ADVERTISEMENT_SHOW)
                        .post(App.isBackDataEl)

                    appAdDataEl = null
                    whetherToShowEl = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(logTagEl, "Ad failed to show fullscreen content.")
                    appAdDataEl = null
                    whetherToShowEl = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdDataEl = null
                    recordNumberOfAdDisplaysEl()
                    // Called when ad is shown.
                    whetherToShowEl = true
                    KLog.d(logTagEl, "connect----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayConnectAdvertisementEl(activity: AppCompatActivity): Boolean {
        if (appAdDataEl == null) {
            KLog.d(logTagEl, "connect--插屏广告加载中。。。")
            return false
        }
        if (whetherToShowEl || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagEl, "connect--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        connectScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdDataEl as InterstitialAd).show(activity)
        }
        return true
    }

}