package com.vkas.easylinkapp.view.main

import android.app.AlertDialog
import android.app.Application
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.google.gson.reflect.TypeToken
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.app.App.Companion.mmkvEl
import com.vkas.easylinkapp.base.BaseViewModel
import com.vkas.easylinkapp.bean.ElIpBean
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.EasyUtils
import com.vkas.easylinkapp.utils.KLog
import com.vkas.easylinkapp.utils.MmkvUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.net.JsonUtil

class MainViewModel (application: Application) : BaseViewModel(application){
    //初始化服务器数据
    val liveInitializeServerData: MutableLiveData<ElVpnBean> by lazy {
        MutableLiveData<ElVpnBean>()
    }
    //更新服务器数据(未连接)
    val liveNoUpdateServerData: MutableLiveData<ElVpnBean> by lazy {
        MutableLiveData<ElVpnBean>()
    }
    //更新服务器数据(已连接)
    val liveUpdateServerData: MutableLiveData<ElVpnBean> by lazy {
        MutableLiveData<ElVpnBean>()
    }

    //当前服务器
    var currentServerData: ElVpnBean = ElVpnBean()
    //断开后选中服务器
    var afterDisconnectionServerData: ElVpnBean = ElVpnBean()
    //跳转结果页
    val liveJumpResultsPage: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }
    fun initializeServerData() {
        val bestData = EasyUtils.getFastIpEl()
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                ProfileManager.updateProfile(setSkServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setSkServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
        currentServerData = bestData
        val serviceData = JsonUtil.toJson(currentServerData)
        MmkvUtils.set("currentServerData",serviceData)
        liveInitializeServerData.postValue(bestData)
    }

    fun updateSkServer(skServiceBean: ElVpnBean,isConnect:Boolean) {
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setSkServerData(it, skServiceBean)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        if(isConnect){
            afterDisconnectionServerData = skServiceBean
            liveUpdateServerData.postValue(skServiceBean)
        }else{
            currentServerData = skServiceBean
            val serviceData = JsonUtil.toJson(currentServerData)
            MmkvUtils.set("currentServerData",serviceData)
            liveNoUpdateServerData.postValue(skServiceBean)
        }
    }

    /**
     * 设置服务器数据
     */
    private fun setSkServerData(profile: Profile, bestData: ElVpnBean): Profile {
        profile.name = bestData.el_country + "-" + bestData.el_city
        profile.host = bestData.el_ip.toString()
        profile.password = bestData.el_pwd!!
        profile.method = bestData.el_method!!
        profile.remotePort = bestData.el_port!!
        return profile
    }
    /**
     * 跳转连接结果页
     */
    fun jumpConnectionResultsPage(isConnection: Boolean){
        val bundle = Bundle()
        val serviceData = mmkvEl.decodeString("currentServerData", "").toString()
        bundle.putBoolean(Constant.CONNECTION_EL_STATUS, isConnection)
        bundle.putString(Constant.SERVER_EL_INFORMATION, serviceData)
        liveJumpResultsPage.postValue(bundle)
    }

    /**
     * 解析是否是非法ip；中国大陆ip、伊朗ip
     */
    fun whetherParsingIsIllegalIp(): Boolean {
        val data = mmkvEl.decodeString(Constant.IP_INFORMATION)
        KLog.e("state","data=${data}===isNullOrEmpty=${Utils.isNullOrEmpty(data)}")
        return if (Utils.isNullOrEmpty(data)) {
            false
        } else {
            val ptIpBean: ElIpBean = JsonUtil.fromJson(
                mmkvEl.decodeString(Constant.IP_INFORMATION),
                object : TypeToken<ElIpBean?>() {}.type
            )
            return ptIpBean.country_code == "IR"
        }
    }

    /**
     * 是否显示不能使用弹框
     */
    fun whetherTheBulletBoxCannotBeUsed(context: AppCompatActivity) {
        val dialogVpn: AlertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.vpn))
            .setMessage(context.getString(R.string.cant_user_vpn))
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                XUtil.exitApp()
            }.create()
        dialogVpn.setCancelable(false)
        dialogVpn.show()
        dialogVpn.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialogVpn.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }
}