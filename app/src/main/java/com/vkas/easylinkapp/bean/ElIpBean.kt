package com.vkas.easylinkapp.bean

import androidx.annotation.Keep

@Keep
data class ElIpBean (
    val country: String,
    val country_code: String,
    val country_code3: String,
    val ip: String
    )
