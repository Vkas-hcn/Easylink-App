package com.vkas.easylinkapp.utils

import com.google.gson.reflect.TypeToken
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.app.App.Companion.mmkvEl
import com.vkas.easylinkapp.bean.ElAdBean
import com.vkas.easylinkapp.bean.ElDetailBean
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.enevt.Constant
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResUtils.getString
import com.xuexiang.xutil.resource.ResourceUtils
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object EasyUtils {
    /**
     * 获取Fast ip
     */
    fun getFastIpEl(): ElVpnBean {
        val elVpnBean: MutableList<ElVpnBean> = getLocalServerData()
        var intersectionList = findFastAndOrdinaryIntersection(elVpnBean)
        if (intersectionList.size <= 0) {
            intersectionList = elVpnBean
        }
        intersectionList.shuffled().take(1).forEach {
            it.el_best = true
            it.el_country = getString(R.string.fast_service)
            return it
        }
        intersectionList[0].el_best = true
        return intersectionList[0]
    }

    /**
     * 获取本地服务器数据
     */
    fun getLocalServerData(): MutableList<ElVpnBean> {
        return if (Utils.isNullOrEmpty(mmkvEl.decodeString(Constant.PROFILE_EL_DATA))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert("elVpnData.json"),
                object : TypeToken<MutableList<ElVpnBean>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvEl.decodeString(Constant.PROFILE_EL_DATA),
                object : TypeToken<MutableList<ElVpnBean>?>() {}.type
            )
        }
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        return if (Utils.isNullOrEmpty(mmkvEl.decodeString(Constant.PROFILE_EL_DATA_FAST))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert("elVpnFastData.json"),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvEl.decodeString(Constant.PROFILE_EL_DATA_FAST),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        }
    }
    /**
     *
     */

    /**
     * 找出fast与普通交集
     */
    private fun findFastAndOrdinaryIntersection(elVpnBeans: MutableList<ElVpnBean>): MutableList<ElVpnBean> {
        val intersectionList: MutableList<ElVpnBean> = ArrayList()
        getLocalFastServerData().forEach { fast ->
            elVpnBeans.forEach { skServiceBean ->
                if (fast == skServiceBean.el_ip) {
                    intersectionList.add(skServiceBean)
                }
            }
        }
        return intersectionList
    }

    /**
     * 广告排序
     */
    private fun adSortingEl(elAdBean: ElAdBean): ElAdBean {
        val adBean: ElAdBean = ElAdBean()
        val elOpen = elAdBean.el_open.sortedWith(compareByDescending { it.el_weight })
        val elBack = elAdBean.el_back.sortedWith(compareByDescending { it.el_weight })

        val elVpn = elAdBean.el_vpn.sortedWith(compareByDescending { it.el_weight })
        val elResult = elAdBean.el_result.sortedWith(compareByDescending { it.el_weight })
        val elConnect = elAdBean.el_connect.sortedWith(compareByDescending { it.el_weight })


        adBean.el_open = elOpen.toMutableList()
        adBean.el_back = elBack.toMutableList()

        adBean.el_vpn = elVpn.toMutableList()
        adBean.el_result = elResult.toMutableList()
        adBean.el_connect = elConnect.toMutableList()

        adBean.el_show_num = elAdBean.el_show_num
        adBean.el_click_num = elAdBean.el_click_num
        return adBean
    }

    /**
     * 取出排序后的广告ID
     */
    fun takeSortedAdIDEl(index: Int, elAdDetails: MutableList<ElDetailBean>): String {
        return elAdDetails.getOrNull(index)?.el_id ?: ""
    }

    /**
     * 获取广告服务器数据
     */
    fun getAdServerDataEl(): ElAdBean {
        val serviceData: ElAdBean =
            if (Utils.isNullOrEmpty(mmkvEl.decodeString(Constant.ADVERTISING_EL_DATA))) {
                JsonUtil.fromJson(
                    ResourceUtils.readStringFromAssert("elAdData.json"),
                    object : TypeToken<
                            ElAdBean?>() {}.type
                )
            } else {
                JsonUtil.fromJson(
                    mmkvEl.decodeString(Constant.ADVERTISING_EL_DATA),
                    object : TypeToken<ElAdBean?>() {}.type
                )
            }
        return adSortingEl(serviceData)
    }

    /**
     * 是否达到阀值
     */
    fun isThresholdReached(): Boolean {
        val clicksCount = mmkvEl.decodeInt(Constant.CLICKS_EL_COUNT, 0)
        val showCount = mmkvEl.decodeInt(Constant.SHOW_EL_COUNT, 0)
        KLog.e("TAG", "clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e(
            "TAG",
            "el_click_num=${getAdServerDataEl().el_click_num}, getAdServerData().el_show_num=${getAdServerDataEl().el_show_num}"
        )
        if (clicksCount >= getAdServerDataEl().el_click_num || showCount >= getAdServerDataEl().el_show_num) {
            return true
        }
        return false
    }

    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplaysEl() {
        var showCount = mmkvEl.decodeInt(Constant.SHOW_EL_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_EL_COUNT, showCount)
    }

    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClickEl() {
        var clicksCount = mmkvEl.decodeInt(Constant.CLICKS_EL_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_EL_COUNT, clicksCount)
    }

    /**
     * 通过国家获取国旗
     */
    fun getFlagThroughCountryEl(el_country: String): Int {
        when (el_country) {
            "Faster server" -> {
                return R.drawable.ic_fast
            }
            "Japan" -> {
                return R.drawable.ic_japan
            }
            "United Kingdom" -> {
                return R.drawable.ic_unitedkingdom
            }
            "United States" -> {
                return R.drawable.ic_unitedstates
            }
            "Australia" -> {
                return R.drawable.ic_australia
            }
            "Belgium" -> {
                return R.drawable.ic_belgium
            }
            "Brazil" -> {
                return R.drawable.ic_brazil
            }
            "Canada" -> {
                return R.drawable.ic_canada
            }
            "France" -> {
                return R.drawable.ic_france
            }
            "Germany" -> {
                return R.drawable.ic_germany
            }
            "India" -> {
                return R.drawable.ic_india
            }
            "Ireland" -> {
                return R.drawable.ic_ireland
            }
            "Italy" -> {
                return R.drawable.ic_italy
            }
            "Koreasouth" -> {
                return R.drawable.ic_koreasouth
            }
            "Netherlands" -> {
                return R.drawable.ic_netherlands
            }
            "Newzealand" -> {
                return R.drawable.ic_newzealand
            }
            "Norway" -> {
                return R.drawable.ic_norway
            }
            "Russianfederation" -> {
                return R.drawable.ic_russianfederation
            }
            "Singapore" -> {
                return R.drawable.ic_singapore
            }
            "Sweden" -> {
                return R.drawable.ic_sweden
            }
            "Switzerland" -> {
                return R.drawable.ic_switzerland
            }
        }

        return R.drawable.ic_fast
    }

    fun getIpInformation() {
        val sb = StringBuffer()
        try {
            val url = URL("https://ip.seeip.org/geoip/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val `is` = conn.inputStream
                val b = ByteArray(1024)
                var len: Int
                while (`is`.read(b).also { len = it } != -1) {
                    sb.append(String(b, 0, len, Charset.forName("UTF-8")))
                }
                `is`.close()
                conn.disconnect()
                KLog.e("state", "sb==${sb.toString()}")
                MmkvUtils.set(Constant.IP_INFORMATION, sb.toString())
            } else {
                MmkvUtils.set(Constant.IP_INFORMATION, "")
                KLog.e("state", "code==${code.toString()}")
            }
        } catch (var1: Exception) {
            MmkvUtils.set(Constant.IP_INFORMATION, "")
            KLog.e("state", "Exception==${var1.message}")
        }
    }
}