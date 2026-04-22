package com.tejas.grandparentguardian

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

private const val TAG = "CallBroadcastReceiver"

/**
 * Automates the vishing protection by starting/stopping the FloatingGuardianService
 * based on the phone's call state.
 */
class CallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.i(TAG, "Call State Changed: $state | Number: $incomingNumber")

        val serviceIntent = Intent(context, FloatingGuardianService::class.java)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                // Potential to pre-load models or show a "Protection Ready" hint
                Log.d(TAG, "Phone is ringing...")
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Call answered! Start the detector overlay
                Log.i(TAG, "Call answered. Launching Guardian Overlay.")
                serviceIntent.putExtra("NUMBER", incomingNumber ?: "Private Number")
                context.startForegroundService(serviceIntent)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // Call ended. Clean up.
                Log.i(TAG, "Call ended. Removing Guardian Overlay.")
                context.stopService(serviceIntent)
            }
        }
    }
}
