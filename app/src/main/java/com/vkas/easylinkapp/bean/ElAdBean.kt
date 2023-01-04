package com.vkas.easylinkapp.bean

import androidx.annotation.Keep

data class ElAdBean(
    var el_open: MutableList<ElDetailBean> = ArrayList(),
    var el_home: MutableList<ElDetailBean> = ArrayList(),
    var el_translation: MutableList<ElDetailBean> = ArrayList(),
    var el_back: MutableList<ElDetailBean> = ArrayList(),
    var el_vpn: MutableList<ElDetailBean> = ArrayList(),
    var el_result: MutableList<ElDetailBean> = ArrayList(),
    var el_connect: MutableList<ElDetailBean> = ArrayList(),

    var el_click_num: Int = 0,
    var el_show_num: Int = 0
)

@Keep
data class ElDetailBean(
    val el_id: String,
    val el_platform: String,
    val el_type: String,
    val el_weight: Int
)