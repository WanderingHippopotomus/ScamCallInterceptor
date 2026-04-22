package com.tejas.grandparentguardian

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

private const val CHANNEL_ID = "GuardianServiceChannel"
private const val NOTIFICATION_ID = 1

/**
 * Foreground Service that manages the floating vishing detector overlay.
 * Implements necessary owners to host Jetpack Compose in a WindowManager.
 */
class FloatingGuardianService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    // State holders for the overlay UI
    private val transcriptState = mutableStateOf("Listening to call...")
    private val riskState = mutableStateOf(0f)
    private val alertState = mutableStateOf("")
    private val numberState = mutableStateOf("Unknown Caller")

    // STT Engine
    private var voskListener: VoskListener? = null

    // --- Lifecycle & Registry Owners (Crucial for Compose) ---
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStore = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStore

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val number = intent?.getStringExtra("NUMBER") ?: "Unknown"
        numberState.value = number
        
        startForeground(NOTIFICATION_ID, createNotification(number))
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        showOverlay()
        startAnalysis()
        
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (composeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 50 // small offset from top
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingGuardianService)
            setViewTreeSavedStateRegistryOwner(this@FloatingGuardianService)
            setViewTreeViewModelStoreOwner(this@FloatingGuardianService)
            
            setContent {
                FloatingGuardianBanner(
                    callerNumber = numberState.value,
                    transcript = transcriptState.value,
                    riskLevel = riskState.value,
                    alertMessage = alertState.value,
                    onDismiss = { stopSelf() }
                )
            }
        }

        windowManager.addView(composeView, params)
    }

    private fun startAnalysis() {
        voskListener = VoskListener(
            context = this,
            onTranscript = { text ->
                transcriptState.value = text
            },
            onRiskDetected = { risk, message ->
                riskState.value = risk
                alertState.value = message
            }
        )
        voskListener?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        voskListener?.stop()
        composeView?.let { windowManager.removeView(it) }
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Guardian Overlay Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(number: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Guardian Active")
            .setContentText("Protecting call with $number")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // Replace with app icon
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
