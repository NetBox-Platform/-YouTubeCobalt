package com.youtube.tv.utils

import android.annotation.SuppressLint
import android.os.Build
import android.util.Base64
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@SuppressLint("PrivateApi")
private fun getSystemProperty(key: String): String? {
    return try {
        val c = Class.forName("android.os.SystemProperties")
        val get = c.getMethod("get", String::class.java)
        get.invoke(null, key) as? String
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@SuppressLint("PrivateApi")
private fun getPrimarySerialNumberFromDeviceHW(): String {
    val keysToTry = listOf(
        "ro.vendor.deviceid",
        "ro.boot.deviceid",
        "ro.serialno"
    )

    val systemPropertiesSerial = keysToTry.firstNotNullOfOrNull { key ->
        getSystemProperty(key)
    }

    if (!systemPropertiesSerial.isNullOrEmpty()) {
        return systemPropertiesSerial
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return ""
    }

    var serialNumber: String? = null
    try {
        val process = Runtime.getRuntime().exec("su")
        val outputStream = DataOutputStream(process.outputStream)
        outputStream.writeBytes(
            "busybox hexdump -e '2/1 \"%c\" \"\"' " +
                "/dev/block/by-name/private | grep -o -a \"$\\`&.\\{27\\}%!\" " +
                "| head -1 | sed 's/\\$`&// ' | sed 's/%!//'i\n"
        )
        outputStream.flush()
        val bufferedReader = BufferedReader(
            InputStreamReader(process.inputStream)
        )
        val tempBuffer = CharArray(27)
        bufferedReader.read(tempBuffer)
        serialNumber = String(tempBuffer)
        outputStream.writeBytes("exit\n")
        outputStream.flush()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return serialNumber?.takeIf {
        it.isNotEmpty() && (it.startsWith("$`&") || it.startsWith("XBN"))
    }?.substring(3, 30) ?: ""
}

/**
 * takes a 27 byte serial and returns true if it is a valid one.
 * Put this function in netbox android applications
 */
private fun verifyChecksum(data: String): Boolean {
    try {
        val messageDigest = MessageDigest.getInstance("MD5")
        messageDigest.update(data.substring(0, 24).toByteArray())
        val digest = messageDigest.digest()
        val hash = Base64.encodeToString(digest, Base64.DEFAULT)
        val checksum = hash[2].toString() + hash[8].toString() + hash[16].toString()
        return checksum == data.substring(24)
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}

fun checkSerial(primarySerialNumber: String) {
    if (!verifyChecksum(primarySerialNumber)) {
        throw RuntimeException()
    }
}

private fun isAfterMarketSupportingBox(): Boolean {
    try {
        val deviceType = getSystemProperty("ro.vendor.nbx.devicetype") ?: return false
        return deviceType == "aftermarket"
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

@SuppressLint("PrivateApi")
private fun getAfterMarketSupportingBoxNumberFromDeviceHW(): String {
    val keysToTry = listOf(
        "persist.vendor.deviceid"
    )
    val systemPropertiesSerial = keysToTry.firstNotNullOfOrNull { key ->
        getSystemProperty(key)
    }
    if (!systemPropertiesSerial.isNullOrEmpty()) {
        return systemPropertiesSerial
    }
    return ""
}

fun getCompatiblePrimarySerialNumberFromDeviceHW() =
    if (isAfterMarketSupportingBox()) {
        getAfterMarketSupportingBoxNumberFromDeviceHW()
    } else {
        getPrimarySerialNumberFromDeviceHW()
    }