package com.vkas.easylinkapp.app

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.tencent.mmkv.MMKV
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.CalendarUtils
import com.vkas.easylinkapp.utils.MmkvUtils
import kotlinx.coroutines.Job
import com.blankj.utilcode.util.ProcessUtils
import com.github.shadowsocks.Core
import com.google.android.gms.ads.AdActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easylinkapp.BuildConfig
import com.vkas.easylinkapp.view.main.MainActivity
import com.vkas.easylinkapp.base.AppManagerElMVVM
import com.vkas.easylinkapp.utils.ActivityUtils
import com.vkas.easylinkapp.utils.ElTimerThread.sendTimerInformation
import com.vkas.easylinkapp.utils.KLog
import com.vkas.easylinkapp.view.start.StartActivity
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class App : Application(), LifecycleObserver {
    private var flag = 0
    private var job_pt : Job? =null
    private var ad_activity_pt: Activity? = null
    private var top_activity_pt: Activity? = null
    companion object {
        // app当前是否在后台
        var isBackDataEl = false

        // 是否进入后台（三秒后）
        var whetherBackgroundEl = false
        // 原生广告刷新
        var nativeAdRefreshEl = false
        val mmkvEl by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("EasyLink", MMKV.MULTI_PROCESS_MODE)
        }
        //当日日期
        var adDateEl = ""
        /**
         * 判断是否是当天打开
         */
        fun isAppOpenSameDayEl() {
            adDateEl = mmkvEl.decodeString(Constant.CURRENT_EL_DATE, "").toString()
            if (adDateEl == "") {
                MmkvUtils.set(Constant.CURRENT_EL_DATE, CalendarUtils.formatDateNow())
            } else {
                if (CalendarUtils.dateAfterDate(adDateEl, CalendarUtils.formatDateNow())) {
                    MmkvUtils.set(Constant.CURRENT_EL_DATE, CalendarUtils.formatDateNow())
                    MmkvUtils.set(Constant.CLICKS_EL_COUNT, 0)
                    MmkvUtils.set(Constant.SHOW_EL_COUNT, 0)
                }
            }
        }

    }
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
//        initCrash()
        setActivityLifecycleEl(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
            XUI.init(this) //初始化UI框架
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)
        sendTimerInformation()
        isAppOpenSameDayEl()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        nativeAdRefreshEl =true
        job_pt?.cancel()
        job_pt = null
        //从后台切过来，跳转启动页
        if (whetherBackgroundEl&& !isBackDataEl) {
            jumpGuidePage()
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState(){
        job_pt = GlobalScope.launch {
            whetherBackgroundEl = false
            delay(3000L)
            whetherBackgroundEl = true
            ad_activity_pt?.finish()
            ActivityUtils.getActivity(StartActivity::class.java)?.finish()
        }
    }
    /**
     * 跳转引导页
     */
    private fun jumpGuidePage(){
        whetherBackgroundEl = false
        val intent = Intent(top_activity_pt, StartActivity::class.java)
        intent.putExtra(Constant.RETURN_EL_CURRENT_PAGE, true)
        MmkvUtils.set(Constant.RETURN_EL_CURRENT_PAGE,true)
        top_activity_pt?.startActivity(intent)
    }
    fun setActivityLifecycleEl(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppManagerElMVVM.get().addActivity(activity)
                if (activity !is AdActivity) {
                    top_activity_pt = activity
                } else {
                    ad_activity_pt = activity
                }
                KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
            }

            override fun onActivityStarted(activity: Activity) {
                KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_pt = activity
                } else {
                    ad_activity_pt = activity
                }
                flag++
                isBackDataEl = false
            }

            override fun onActivityResumed(activity: Activity) {
                KLog.v("Lifecycle", "onActivityResumed=" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_pt = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is AdActivity) {
                    ad_activity_pt = activity
                } else {
                    top_activity_pt = activity
                }
                KLog.v("Lifecycle", "onActivityPaused=" + activity.javaClass.name)
            }

            override fun onActivityStopped(activity: Activity) {
                flag--
                if (flag == 0) {
                    isBackDataEl = true
                }
                KLog.v("Lifecycle", "onActivityStopped=" + activity.javaClass.name)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                KLog.v("Lifecycle", "onActivitySaveInstanceState=" + activity.javaClass.name)

            }

            override fun onActivityDestroyed(activity: Activity) {
                AppManagerElMVVM.get().removeActivity(activity)
                KLog.v("Lifecycle", "onActivityDestroyed" + activity.javaClass.name)
                ad_activity_pt = null
                top_activity_pt = null
            }
        })
    }
}