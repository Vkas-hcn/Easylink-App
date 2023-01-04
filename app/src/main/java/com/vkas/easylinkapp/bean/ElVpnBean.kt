package com.vkas.easylinkapp.bean

import androidx.annotation.Keep

@Keep
data class ElVpnBean (
    var el_city: String? = null,
    var el_country: String? = null,
    var el_ip: String? = null,
    var el_method: String? = null,
    var el_port: Int? = null,
    var el_pwd: String? = null,
    var el_check: Boolean? = false,
    var el_best: Boolean? = false,
    var isAd:Boolean? = false
)