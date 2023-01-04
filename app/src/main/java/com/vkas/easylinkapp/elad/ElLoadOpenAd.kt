package com.vkas.easylinkapp.elad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
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
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
class ElLoadOpenAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadEl
    }

    object InstanceHelper {
        val openLoadEl = ElLoadOpenAd()
    }

    var appAdDataEl: Any? = null

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
        KLog.d(logTagEl, "open--isLoading=${isLoadingEl}")

        if (isLoadingEl) {
            KLog.d(logTagEl, "open--广告加载中，不能再次加载")
            return
        }

        if (appAdDataEl == null) {
            isLoadingEl = true
            loadStartupPageAdvertisementEl(context, getAdServerDataEl())
        }
        if (appAdDataEl != null && !whetherAdExceedsOneHour(loadTimeEl)) {
            isLoadingEl = true
            appAdDataEl = null
            loadStartupPageAdvertisementEl(context, getAdServerDataEl())
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
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementEl(context: Context, adData: ElAdBean) {
        if (adData.el_open.getOrNull(adIndexEl)?.el_type == "screen") {
            loadStartInsertAdEl(context, adData)
        } else {
            loadOpenAdvertisementEl(context, adData)
        }
    }

    /**
     * 加载开屏广告
     */
    private fun loadOpenAdvertisementEl(context: Context, adData: ElAdBean) {
        KLog.e("loadOpenAdvertisementEl", "adData().el_open=${JsonUtil.toJson(adData.el_open)}")
        KLog.e(
            "loadOpenAdvertisementEl",
            "id=${JsonUtil.toJson(takeSortedAdIDEl(adIndexEl, adData.el_open))}"
        )

        val id = takeSortedAdIDEl(adIndexEl, adData.el_open)

        KLog.d(logTagEl, "open--开屏广告id=$id;权重=${adData.el_open.getOrNull(adIndexEl)?.el_weight}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loadTimeEl = Date().time
                    isLoadingEl = false
                    appAdDataEl = ad

                    KLog.d(logTagEl, "open--开屏广告加载成功")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingEl = false
                    appAdDataEl = null
                    if (adIndexEl < adData.el_open.size - 1) {
                        adIndexEl++
                        loadStartupPageAdvertisementEl(context, adData)
                    } else {
                        adIndexEl = 0
                    }
                    KLog.d(logTagEl, "open--开屏广告加载失败: " + loadAdError.message)
                }
            }
        )
    }


    /**
     * 开屏广告回调
     */
    private fun advertisingOpenCallbackEl() {
        if (appAdDataEl !is AppOpenAd) {
            return
        }
        (appAdDataEl as AppOpenAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                //取消全屏内容
                override fun onAdDismissedFullScreenContent() {
                    KLog.d(logTagEl, "open--关闭开屏内容")
                    whetherToShowEl = false
                    appAdDataEl = null
                    if (!App.whetherBackgroundEl) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                }

                //全屏内容无法显示时调用
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    whetherToShowEl = false
                    appAdDataEl = null
                    KLog.d(logTagEl, "open--全屏内容无法显示时调用")
                }

                //显示全屏内容时调用
                override fun onAdShowedFullScreenContent() {
                    appAdDataEl = null
                    whetherToShowEl = true
                    recordNumberOfAdDisplaysEl()
                    adIndexEl = 0
                    KLog.d(logTagEl, "open---开屏广告展示")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    KLog.d(logTagEl, "open---点击open广告")
                    recordNumberOfAdClickEl()
                }
            }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisementEl(activity: AppCompatActivity): Boolean {

        if (appAdDataEl == null) {
            KLog.d(logTagEl, "open---开屏广告加载中。。。")
            return false
        }
        if (whetherToShowEl || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagEl, "open---前一个开屏广告展示中或者生命周期不对")
            return false
        }
        if (appAdDataEl is AppOpenAd) {
            advertisingOpenCallbackEl()
            (appAdDataEl as AppOpenAd).show(activity)
        } else {
            startInsertScreenAdCallbackEl()
            (appAdDataEl as InterstitialAd).show(activity)
        }
        return true
    }

    /**
     * 加载启动页插屏广告
     */
    private fun loadStartInsertAdEl(context: Context, adData: ElAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDEl(adIndexEl, adData.el_open)
        KLog.d(
            logTagEl,
            "open--插屏广告id=$id;权重=${adData.el_open.getOrNull(adIndexEl)?.el_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagEl, "open---连接插屏加载失败=$it") }
                    isLoadingEl = false
                    appAdDataEl = null
                    if (adIndexEl < adData.el_open.size - 1) {
                        adIndexEl++
                        loadStartupPageAdvertisementEl(context, adData)
                    } else {
                        adIndexEl = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimeEl = Date().time
                    isLoadingEl = false
                    appAdDataEl = interstitialAd
                    KLog.d(logTagEl, "open--启动页插屏加载完成")
                }
            })
    }

    /**
     * StartInsert插屏广告回调
     */
    private fun startInsertScreenAdCallbackEl() {
        if (appAdDataEl !is InterstitialAd) {
            return
        }
        (appAdDataEl as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagEl, "open--插屏广告点击")
                    recordNumberOfAdClickEl()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagEl, "open--关闭StartInsert插屏广告${App.isBackDataEl}")
                    if (!App.whetherBackgroundEl) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
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
                    adIndexEl = 0
                    KLog.d(logTagEl, "open----插屏show")
                }
            }
    }
}