package com.vkas.easylinkapp.view.vpnlist

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.easylinkapp.R
import com.vkas.easylinkapp.bean.ElVpnBean
import com.vkas.easylinkapp.enevt.Constant
import com.vkas.easylinkapp.utils.EasyUtils.getFlagThroughCountryEl

class VpnListAdapter (data: MutableList<ElVpnBean>?) :
    BaseQuickAdapter<ElVpnBean, BaseViewHolder>(
        R.layout.item_vpn,
        data
    ) {
    override fun convert(holder: BaseViewHolder, item: ElVpnBean) {
        if (item.el_best == true) {
            holder.setText(R.id.txt_country, Constant.FASTER_EL_SERVER)
            holder.setImageResource(R.id.img_flag, getFlagThroughCountryEl(Constant.FASTER_EL_SERVER))
        } else {
            holder.setText(R.id.txt_country, item.el_country + "-" + item.el_city)
            holder.setImageResource(R.id.img_flag, getFlagThroughCountryEl(item.el_country.toString()))
        }
        if (item.el_check == true) {
            holder.setBackgroundResource(R.id.con_item, R.drawable.ic_item_frame)
        } else {
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_item)
        }
    }
}