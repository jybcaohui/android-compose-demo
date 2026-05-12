package com.demo.creditlimit.network.model.request2

data class CustomerWifiInfoBean(
    val localIp: String,
    val wifiName: String,
    val routerIp: String,
    val macAddress: String
)