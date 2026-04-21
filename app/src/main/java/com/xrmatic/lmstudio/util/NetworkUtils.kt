package com.xrmatic.lmstudio.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Helpers for inspecting the device's current network state.
 */
object NetworkUtils {

    /**
     * Returns `true` if the device has an active network connection.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Returns `true` if the device is currently connected via WiFi.
     */
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Returns `true` if the device is connected via mobile (cellular) data.
     */
    fun isMobileDataConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Validates that sending a request is permitted given the current network
     * state and the user's WiFi-only setting.
     *
     * @return `null` if sending is allowed, or an error message string otherwise.
     */
    fun checkNetworkAllowed(context: Context, wifiOnly: Boolean): String? {
        if (!isNetworkAvailable(context)) {
            return "No network connection available."
        }
        if (wifiOnly && !isWifiConnected(context)) {
            return "WiFi-only mode is enabled. Please connect to a WiFi network."
        }
        return null
    }
}
