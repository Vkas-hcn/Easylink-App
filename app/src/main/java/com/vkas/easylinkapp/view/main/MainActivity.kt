package com.vkas.easylinkapp.view.main

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easylinkapp.BR
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.app.App
import com.vkas.easylinkapp.app.App.Companion.mmkvEl
import com.vkas.easylinkapp.base.BaseActivity
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.databinding.ActivityMainBinding
import com.vkas.easylinkapp.elad.ElLoadConnectAd
import com.vkas.easylinkapp.elad.ElLoadResultAd
import com.vkas.easylinkapp.elad.ElLoadVpnAd
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.enevt.Constant.logTagEl
import com.vkas.easylinkapp.utils.EasyUtils
import com.vkas.easylinkapp.utils.EasyUtils.getFlagThroughCountryEl
import com.vkas.easylinkapp.utils.EasyUtils.isThresholdReached
import com.vkas.easylinkapp.utils.ElTimerThread
import com.vkas.easylinkapp.utils.KLog
import com.vkas.easylinkapp.utils.MmkvUtils
import com.vkas.easylinkapp.view.result.ResultElActivity
import com.vkas.easylinkapp.view.vpnlist.VpnList
import com.vkas.easylinkapp.view.webel.WebElActivity
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    var state = BaseService.State.Idle

    //重复点击
    var repeatClick = false
    private var jobRepeatClick: Job? = null

    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    private val connection = ShadowsocksConnection(true)

    // 是否返回刷新服务器
    var whetherRefreshServer = false
    private var jobNativeAdsEl: Job? = null
    private var jobStartEl: Job? = null

    //当前执行连接操作
    private var performConnectionOperations: Boolean = false

    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = ElClick()
        liveEventBusReceive()
        binding.mainTitleEl.imgBack.setImageResource(R.drawable.ic_title_setting)
        binding.mainTitleEl.tvTitle.visibility = View.GONE
        binding.mainTitleEl.tvRight.visibility = View.GONE
        binding.mainTitleEl.imgBack.setOnClickListener {
            finish()
        }
        binding.mainTitleEl.imgBack.setOnClickListener {
            binding.sidebarShowsEl = binding.sidebarShowsEl != true
        }
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Constant.TIMER_EL_DATA, String::class.java)
            .observeForever {
                binding.txtTimerEl.text = it
            }
        //更新服务器(未连接)
        LiveEventBus
            .get(Constant.NOT_CONNECTED_EL_RETURN, ElVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, false)
            }
        //更新服务器(已连接)
        LiveEventBus
            .get(Constant.CONNECTED_EL_RETURN, ElVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, true)
            }
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_EL_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observeForever {
                KLog.e("state", "插屏关闭接收=${it}")

                //重复点击
                jobRepeatClick = lifecycleScope.launch {
                    if (!repeatClick) {
                        KLog.e("state", "插屏关闭后跳转=${it}")
                        connectOrDisconnectEl(it)
                        repeatClick = true
                    }
                    delay(1000)
                    repeatClick = false
                }
            }
    }

    override fun initData() {
        super.initData()
        if(viewModel.whetherParsingIsIllegalIp()){
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        if (ElTimerThread.isStopThread) {
            viewModel.initializeServerData()
        } else {
            val serviceData = mmkvEl.decodeString("currentServerData", "").toString()
            val currentServerData: ElVpnBean = JsonUtil.fromJson(
                serviceData,
                object : TypeToken<ElVpnBean?>() {}.type
            )
            setFastInformation(currentServerData)
        }
//        ElLoadVpnAd.getInstance().whetherToShowEl = false
        initHomeAd()
    }

    private fun initHomeAd() {
//        jobNativeAdsEl = lifecycleScope.launch {
//            while (isActive) {
//                ElLoadVpnAd.getInstance().setDisplayHomeNativeAdEl(this@MainActivity, binding)
//                if (ElLoadVpnAd.getInstance().whetherToShowEl) {
//                    jobNativeAdsEl?.cancel()
//                    jobNativeAdsEl = null
//                }
//                delay(1000L)
//            }
//        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
        // 跳转结果页
        jumpResultsPageData()
        setServiceData()
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    startActivityForResult(ResultElActivity::class.java, 0x11, it)
                }
            }
        })
        viewModel.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)
        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            connect.launch(null)
        })
    }

    inner class ElClick {
        fun linkService() {
            if (binding.vpnState != 1) {
                connect.launch(null)
            }
        }

        fun clickService() {
            if (binding.vpnState != 1) {
                jumpToServerList()
            }
        }
        fun clickMain(){
            if (binding.sidebarShowsEl == true) {
                binding.sidebarShowsEl = false
            }
        }
        fun clickMainMenu(){

        }
        fun toContactUs() {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_EL_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            runCatching {
                startActivity(intent)
            }.onFailure {
                ToastUtils.toast("Please set up a Mail account")
            }
        }

        fun toPrivacyPolicy() {
            startActivity(WebElActivity::class.java)
        }

        fun toShare() {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(
                Intent.EXTRA_TEXT,
                Constant.SHARE_EL_ADDRESS + this@MainActivity.packageName
            )
            intent.type = "text/plain"
            startActivity(intent)
        }
    }

    /**
     * 跳转服务器列表
     */
    fun jumpToServerList() {
        lifecycleScope.launch {
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            val bundle = Bundle()
            if (state.name == "Connected") {
                bundle.putBoolean(Constant.WHETHER_EL_CONNECTED, true)
            } else {
                bundle.putBoolean(Constant.WHETHER_EL_CONNECTED, false)
            }
            val serviceData = mmkvEl.decodeString("currentServerData", "").toString()
            bundle.putString(Constant.CURRENT_EL_SERVICE, serviceData)
            startActivity(VpnList::class.java, bundle)
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(elVpnBean: ElVpnBean) {
        if (elVpnBean.el_best == true) {
            binding.txtCountry.text = Constant.FASTER_EL_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountryEl(Constant.FASTER_EL_SERVER))
        } else {
            binding.txtCountry.text =
                String.format(elVpnBean.el_country + "-" + elVpnBean.el_city)
            binding.imgCountry.setImageResource(getFlagThroughCountryEl(elVpnBean.el_country.toString()))
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
            if (isNetworkAvailable()) {
                startVpn()
            } else {
                ToastUtils.toast("The current device has no network")
            }
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        binding.vpnState = 1
        changeOfVpnStatus()
//        App.isAppOpenSameDayEl()
//        if (isThresholdReached()) {
//            KLog.d(logTagEl, "广告达到上线")
//            connectOrDisconnectEl(false)
//            return
//        }
//        ElLoadConnectAd.getInstance().advertisementLoadingEl(this)
//        ElLoadResultAd.getInstance().advertisementLoadingEl(this)

        jobStartEl = lifecycleScope.launch {
            delay(1000L)
            connectOrDisconnectEl(false)
//            try {
//                withTimeout(10000L) {
//                    delay(2000L)
//                    KLog.e(logTagEl, "jobStartEl?.isActive=${jobStartEl?.isActive}")
//                    while (jobStartEl?.isActive == true) {
//                        val showState =
//                            ElLoadConnectAd.getInstance()
//                                .displayConnectAdvertisementEl(this@MainActivity)
//                        if (showState) {
//                            jobStartEl?.cancel()
//                            jobStartEl = null
//                        }
//                        delay(1000L)
//                    }
//                }
//            } catch (e: TimeoutCancellationException) {
//                KLog.d(logTagEl, "connect---插屏超时")
//                if (jobStartEl != null) {
//                    connectOrDisconnectEl(false)
//                }
//            }
        }
    }

    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnectEl(isBackgroundClosed: Boolean) {
        KLog.e("state", "连接或断开")
        if (state.canStop) {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(false)
            }
            Core.stopService()
            performConnectionOperations = false
        } else {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(true)
            }
            Core.startService()
            performConnectionOperations = true
        }
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLog.e("TAG", "connectionStatusJudgment=${state}")
        if (performConnectionOperations && state != "Connected") {
            //vpn连接失败
            KLog.d(logTagEl, "vpn连接失败")
            ToastUtils.toast(getString(R.string.connected_failed))
        }
        when (state) {
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
            }
            "Stopped" -> {
                disconnectServerSuccessful()
            }
        }
    }

    /**
     * 连接服务器成功
     */
    private fun connectionServerSuccessful() {
        binding.vpnState = 2
        changeOfVpnStatus()
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        KLog.e("TAG", "断开服务器")
        binding.vpnState = 0
        changeOfVpnStatus()
    }

    /**
     * vpn状态变化
     * 是否连接
     */
    private fun changeOfVpnStatus() {
        when (binding.vpnState) {
            0 -> {
                binding.imgState.setImageResource(R.drawable.ic_connect)
                binding.imgConnectionStatus.setImageResource(R.mipmap.bg_connect)
                binding.txtTimerEl.text = getString(R.string._00_00_00)
                binding.txtTimerEl.setTextColor(getColor(R.color.tv_time_dis))
                ElTimerThread.endTiming()
                binding.lavViewEl.pauseAnimation()
                binding.lavViewEl.visibility = View.GONE
            }
            1 -> {
                binding.imgState.visibility = View.GONE
                if (performConnectionOperations) {
                    binding.imgConnectionStatus.setImageResource(R.mipmap.bg_disconnecting)
                    binding.lavViewEl.setAnimation("data_dis.json")
                    binding.lavViewEl.imageAssetsFolder = "imagesDis"
                } else {
                    binding.imgConnectionStatus.setImageResource(R.mipmap.bg_connecting)
                    binding.lavViewEl.setAnimation("data_connect.json")
                    binding.lavViewEl.imageAssetsFolder = "imagesConnect"
                }
                binding.lavViewEl.visibility = View.VISIBLE
                binding.lavViewEl.playAnimation()
            }
            2 -> {
                binding.imgState.setImageResource(R.mipmap.ic_vpn_connect_success)
                binding.imgConnectionStatus.setImageResource(R.mipmap.bg_connected)
                binding.txtTimerEl.setTextColor(getColor(R.color.tv_time_connect))
                ElTimerThread.startTiming()
                binding.lavViewEl.pauseAnimation()
                binding.lavViewEl.visibility = View.GONE
            }
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state)
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
//            if (App.nativeAdRefreshEl) {
//                if (viewModel.afterDisconnectionServerData.el_ip == null) {
//                    setFastInformation(viewModel.currentServerData)
//                } else {
//                    setFastInformation(viewModel.afterDisconnectionServerData)
//                }
//                ElLoadVpnAd.getInstance().whetherToShowEl = false
//                if (ElLoadVpnAd.getInstance().appAdDataEl != null) {
//                    KLog.d(logTagEl, "onResume------>1")
//                    ElLoadVpnAd.getInstance().setDisplayHomeNativeAdEl(this@VpnActivity, binding)
//                } else {
//                    binding.vpnAdEl = false
//                    KLog.d(logTagEl, "onResume------>2")
//                    ElLoadVpnAd.getInstance().advertisementLoadingEl(this@VpnActivity)
//                    initHomeAd()
//                }
//            }
        }

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveEventBus
            .get(Constant.PLUG_EL_ADVERTISEMENT_SHOW, Boolean::class.java)
            .removeObserver {}
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStartEl?.cancel()
        jobStartEl = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            val serviceData = toJson(viewModel.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return true
    }
}