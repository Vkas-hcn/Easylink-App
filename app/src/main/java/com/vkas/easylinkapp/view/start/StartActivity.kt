package com.vkas.easylinkapp.view.start

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easylinkapp.BR
import com.vkas.easylinkapp.BuildConfig
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.app.App
import com.vkas.easylinkapp.base.BaseActivity
import com.vkas.easylinkapp.databinding.ActivityStartBinding
import com.vkas.easylinkapp.elad.ElLoadOpenAd
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.enevt.Constant.logTagEl
import com.vkas.easylinkapp.utils.EasyUtils
import com.vkas.easylinkapp.utils.EasyUtils.isThresholdReached
import com.vkas.easylinkapp.utils.KLog
import com.vkas.easylinkapp.utils.MmkvUtils
import com.vkas.easylinkapp.view.main.MainActivity
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import kotlinx.coroutines.*

class StartActivity : BaseActivity<ActivityStartBinding, StartViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    companion object {
        var isCurrentPage: Boolean = false
    }
    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsEl: Job? = null

    override fun initContentView(savedInstanceState: Bundle?): Int {
       return R.layout.activity_start
    }

    override fun initVariableId(): Int {
        return BR._all
    }
    override fun initToolbar() {
        super.initToolbar()
    }

    override fun initData() {
        super.initData()
        binding.pbStartEl.setProgressViewUpdateListener(this)
        binding.pbStartEl.startProgressAnimation()
        liveEventBusEl()
        lifecycleScope.launch(Dispatchers.IO) {
            EasyUtils.getIpInformation()
        }
        getFirebaseDataEl()
        jumpHomePageData()
    }
    private fun liveEventBusEl() {
        LiveEventBus
            .get(Constant.OPEN_CLOSE_JUMP, Boolean::class.java)
            .observeForever {
                KLog.d(logTagEl, "关闭开屏内容-接收==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    private fun getFirebaseDataEl() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            MmkvUtils.set(Constant.PIXEL_SET, "4")
////
//            MmkvUtils.set(Constant.PIXEL_SET_P, "1")
////
//            MmkvUtils.set(Constant.PIXEL_ABT, "100")

//            lifecycleScope.launch {
//                delay(500)
//                MmkvUtils.set(Constant.ADVERTISING_EL_DATA, ResourceUtils.readStringFromAssert("ptAdDataFireBase.json"))
//            }
            return
        } else {
            preloadedAdvertisement()
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Constant.PROFILE_EL_DATA, auth.getString("easy_servers"))
                MmkvUtils.set(Constant.PROFILE_EL_DATA_FAST, auth.getString("easy_smart"))
                MmkvUtils.set(Constant.AROUND_EL_FLOW_DATA, auth.getString("ElAroundFlow_Data"))
                MmkvUtils.set(Constant.ADVERTISING_EL_DATA, auth.getString("easy_ad"))

            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun jumpHomePageData() {
        liveJumpHomePage2.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                KLog.e("TAG", "isBackDataEl==${App.isBackDataEl}")
                delay(300)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    jumpPage()
                }
            }
        })
        liveJumpHomePage.observe(this, {
            liveJumpHomePage2.postValue(true)
        })
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (!isCurrentPage) {
            val intent = Intent(this@StartActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()

    }
//    /**
//     * 加载广告
//     */
//    private fun loadAdvertisement() {
//        //开屏
//        ElLoadOpenAd.getInstance().adIndexEl = 0
//        ElLoadOpenAd.getInstance().advertisementLoadingEl(this)
//        rotationDisplayOpeningAdEl()
//
//        ElLoadHomeAd.getInstance().adIndexEl = 0
//        ElLoadHomeAd.getInstance().advertisementLoadingEl(this)
//
//        ElLoadTranslationAd.getInstance().adIndexEl = 0
//        ElLoadTranslationAd.getInstance().advertisementLoadingEl(this)
//
//        ElLoadBackAd.getInstance().adIndexEl = 0
//        ElLoadBackAd.getInstance().advertisementLoadingEl(this)
//
//        ElLoadVpnAd.getInstance().adIndexEl = 0
//        ElLoadVpnAd.getInstance().advertisementLoadingEl(this)
//        ElLoadResultAd.getInstance().adIndexEl = 0
//        ElLoadResultAd.getInstance().advertisementLoadingEl(this)
//        ElLoadConnectAd.getInstance().adIndexEl = 0
//        ElLoadConnectAd.getInstance().advertisementLoadingEl(this)
//    }
    /**
     * 轮训展示开屏广告
     */
    private fun rotationDisplayOpeningAdEl() {
        jobOpenAdsEl = lifecycleScope.launch {
            try {
                withTimeout(8000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState = ElLoadOpenAd.getInstance()
                            .displayOpenAdvertisementEl(this@StartActivity)
                        if (showState) {
                            jobOpenAdsEl?.cancel()
                            jobOpenAdsEl =null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }
    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
//        App.isAppOpenSameDayEl()
//        if (isThresholdReached()) {
//            KLog.d(logTagEl, "广告达到上线")
            lifecycleScope.launch {
                delay(2000L)
                liveJumpHomePage.postValue(true)
            }
//        } else {
//            loadAdvertisement()
//        }
    }
    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }
}