package com.vkas.easylinkapp.view.vpnlist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.easylinkapp.app.App.Companion.mmkvEl
import com.vkas.easylinkapp.base.BaseViewModel
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.EasyUtils.getFastIpEl
import com.vkas.easylinkapp.utils.EasyUtils.getLocalServerData
import com.vkas.easylinkapp.utils.KLog
import com.xuexiang.xui.utils.Utils.isNullOrEmpty
import com.xuexiang.xutil.net.JsonUtil

class VpnListViewModel (application: Application) : BaseViewModel(application) {
    private lateinit var skServiceBean : ElVpnBean
    private lateinit var skServiceBeanList :MutableList<ElVpnBean>

    // 服务器列表数据
    val liveServerListData: MutableLiveData<MutableList<ElVpnBean>> by lazy {
        MutableLiveData<MutableList<ElVpnBean>>()
    }

    /**
     * 获取服务器列表
     */
    fun getServerListData(){
        skServiceBeanList = ArrayList()
        skServiceBean = ElVpnBean()
        skServiceBeanList = if (isNullOrEmpty(mmkvEl.decodeString(Constant.PROFILE_EL_DATA))) {
            KLog.e("TAG","skServiceBeanList--1--->")

            getLocalServerData()
        } else {
            KLog.e("TAG","skServiceBeanList--2--->")

            JsonUtil.fromJson(
                mmkvEl.decodeString(Constant.PROFILE_EL_DATA),
                object : TypeToken<MutableList<ElVpnBean>?>() {}.type
            )
        }
        skServiceBeanList.add(0, getFastIpEl())
        KLog.e("LOG","skServiceBeanList---->${JsonUtil.toJson(skServiceBeanList)}")

        liveServerListData.postValue(skServiceBeanList)
    }
}