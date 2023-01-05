package com.vkas.easylinkapp.view.result

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import com.vkas.easylinkapp.BR
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.app.App
import com.vkas.easylinkapp.base.BaseActivity
import com.vkas.easylinkapp.base.BaseViewModel
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.databinding.ActivityResultElBinding
import com.vkas.easylinkapp.elad.ElLoadResultAd
import com.vkas.easylinkapp.elad.ElLoadVpnAd
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.EasyUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ResultElActivity : BaseActivity<ActivityResultElBinding, BaseViewModel>() {
    private var isConnectionEl: Boolean = false

    //当前服务器
    private lateinit var currentServerBeanEl: ElVpnBean
    private var jobResultEl: Job? = null
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_result_el
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        isConnectionEl = bundle?.getBoolean(Constant.CONNECTION_EL_STATUS) == true
        currentServerBeanEl = JsonUtil.fromJson(
            bundle?.getString(Constant.SERVER_EL_INFORMATION),
            object : TypeToken<ElVpnBean?>() {}.type
        )

    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = ElClick()
        binding.resultTitle.imgBack.setImageResource(R.drawable.ic_title_back)
        binding.resultTitle.tvTitle.text = if (isConnectionEl) {
            getString(R.string.vpn_connect)
        } else {
            getString(
                R.string.vpn_disconnect
            )
        }
        binding.resultTitle.tvRight.visibility = View.GONE
        binding.resultTitle.imgBack.setOnClickListener {
            finish()
        }
    }

    override fun initData() {
        super.initData()
        if (isConnectionEl) {
            binding.tvConnected.text = getString(R.string.connection_succeed)
            binding.tvConnected.setTextColor(getColor(R.color.tv_vpn_success))
        } else {
            binding.tvConnected.text = getString(R.string.disconnection_succeed)
            binding.tvConnected.setTextColor(getColor(R.color.tv_vpn_dis))
        }
        binding.imgCountry.setImageResource(EasyUtils.getFlagThroughCountryEl(currentServerBeanEl.el_country.toString()))
        binding.txtCountry.text = currentServerBeanEl.el_country.toString()
        ElLoadResultAd.getInstance().whetherToShowEl =false
        initResultAds()
    }

    inner class ElClick {

    }

    private fun initResultAds() {
        jobResultEl= lifecycleScope.launch {
            while (isActive) {
                ElLoadResultAd.getInstance().setDisplayResultNativeAd(this@ResultElActivity,binding)
                if (ElLoadVpnAd.getInstance().whetherToShowEl) {
                    jobResultEl?.cancel()
                    jobResultEl = null
                }
                delay(1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if(lifecycle.currentState != Lifecycle.State.RESUMED){return@launch}
            if(App.nativeAdRefreshEl){
                ElLoadResultAd.getInstance().whetherToShowEl =false
                if(ElLoadResultAd.getInstance().appAdDataEl !=null){
                    ElLoadResultAd.getInstance().setDisplayResultNativeAd(this@ResultElActivity,binding)
                }else{
                    ElLoadResultAd.getInstance().advertisementLoadingEl(this@ResultElActivity)
                    initResultAds()
                }
            }

        }
    }
}