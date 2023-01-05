package com.vkas.easylinkapp.view.vpnlist

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chad.library.adapter.base.BaseDelegateMultiAdapter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.delegate.BaseMultiTypeDelegate
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.elad.ElLoadBackAd
import com.vkas.easylinkapp.elad.ElLoadResultAd
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.EasyUtils.getFlagThroughCountryEl
import kotlinx.coroutines.*

class VpnListAdapter (activity: AppCompatActivity, data: MutableList<ElVpnBean>?) :
    BaseDelegateMultiAdapter<ElVpnBean?, BaseViewHolder>() {
    init {
        setMultiTypeDelegate(object : BaseMultiTypeDelegate<ElVpnBean?>() {
            override fun getItemType(data: List<ElVpnBean?>, position: Int): Int {
                // 根据数据，自己判断应该返回的类型
                return if(data[position]?.isAd ==true){
                    1
                }else{
                    0
                }
            }
        })
        // 第二部，绑定 item 类型
        getMultiTypeDelegate()
            ?.addItemType(0, R.layout.item_vpn)
            ?.addItemType(1, R.layout.item_ad_el)
    }
    private var jobBackEl: Job? = null
    var activityList= activity

    @SuppressLint("ResourceType")
    override fun convert(holder: BaseViewHolder, item: ElVpnBean?) {
        if(holder.itemViewType == 0){
            if (item?.el_best == true) {
                holder.setText(R.id.txt_country, Constant.FASTER_EL_SERVER)
                holder.setImageResource(
                    R.id.img_flag,
                    getFlagThroughCountryEl(Constant.FASTER_EL_SERVER)
                )
            } else {
                holder.setText(R.id.txt_country, item?.el_country + "-" + item?.el_city)
                holder.setImageResource(
                    R.id.img_flag,
                    getFlagThroughCountryEl(item?.el_country.toString())
                )
            }

            if (item?.el_check == true) {
                holder.setBackgroundResource(R.id.con_item, R.drawable.ic_item_frame)
                holder.setTextColor(R.id.txt_country,context.resources.getColor(R.color.white))
            } else {
                holder.setBackgroundResource(R.id.con_item, R.drawable.bg_item)
                holder.setTextColor(R.id.txt_country,context.resources.getColor(R.color.tv_ff_333333))
            }
        }else{
            val adView = holder.getView<FrameLayout>(R.id.el_item_ad)
            val adViewImg = holder.getView<ImageView>(R.id.img_el_item_ad)
            ElLoadBackAd.getInstance().advertisementLoadingEl(activityList)
            initBackAds(adView,adViewImg)
        }

    }
    private fun initBackAds(adView: ViewGroup, adViewImg:ImageView) {
        jobBackEl = activityList.lifecycleScope.launch {
                while (isActive) {
                    ElLoadBackAd.getInstance().setDisplayBackNativeAdEl(activityList, adView,adViewImg)
                    if (ElLoadResultAd.getInstance().whetherToShowEl) {
                        jobBackEl?.cancel()
                        jobBackEl = null
                    }
                    delay(1000L)
                }
        }
    }
}