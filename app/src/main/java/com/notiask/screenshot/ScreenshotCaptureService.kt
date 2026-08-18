package com.notiask.screenshot

import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.notiask.R
import com.notiask.appContainer
import com.notiask.notification.NotificationController
import java.util.concurrent.atomic.AtomicBoolean

class ScreenshotCaptureService : Service() {
    private lateinit var workerThread: HandlerThread
    private lateinit var handler: Handler
    private val started = AtomicBoolean(false)
    private val captured = AtomicBoolean(false)
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("notiask-screenshot").also { it.start() }
        handler = Handler(workerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = capturingNotification()
        startForeground(NotificationController.SCREENSHOT_FGS_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        if (!started.compareAndSet(false, true)) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.projectionData()
        if (resultCode != Activity.RESULT_OK || data == null) {
            appContainer().notifications.showError("截屏授权无效")
            stopSelf()
            return START_NOT_STICKY
        }

        handler.postDelayed({ capture(resultCode, data) }, SHADE_SETTLE_MS)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releaseCapture()
        workerThread.quitSafely()
        super.onDestroy()
    }

    private fun capture(resultCode: Int, data: Intent) {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            manager.getMediaProjection(resultCode, data)
        } catch (e: SecurityException) {
            failAndStop(e.message ?: "无法开始截屏")
            return
        }
        if (projection == null) {
            failAndStop("无法开始截屏")
            return
        }
        mediaProjection = projection
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                handler.post { releaseCapture() }
            }
        }, handler)

        val (width, height, density) = displaySize()
        val reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ incoming ->
            val image = incoming.acquireLatestImage() ?: return@setOnImageAvailableListener
            if (!captured.compareAndSet(false, true)) {
                image.close()
                return@setOnImageAvailableListener
            }
            finishWithImage(image)
        }, handler)

        virtualDisplay = projection.createVirtualDisplay(
            "notiask-screenshot",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )

        handler.postDelayed({
            if (captured.get()) return@postDelayed
            val image = reader.acquireLatestImage()
            if (image != null && captured.compareAndSet(false, true)) {
                finishWithImage(image)
            }
        }, FIRST_FRAME_MS)

        handler.postDelayed({
            if (captured.compareAndSet(false, true)) {
                failAndStop("截屏超时，请重试")
            }
        }, TIMEOUT_MS)
    }

    private fun finishWithImage(image: android.media.Image) {
        try {
            val bitmap = ScreenshotBitmap.fromImage(image)
            val jpeg = ScreenshotBitmap.toJpeg(bitmap)
            bitmap.recycle()
            val container = appContainer()
            container.screenshotSession.setPendingAskImage(jpeg)
            container.notifications.showScreenshotReady()
        } catch (e: Exception) {
            appContainer().notifications.showError(e.message ?: "截屏处理失败")
        } finally {
            image.close()
            releaseCapture()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun failAndStop(message: String) {
        appContainer().notifications.showError(message)
        releaseCapture()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseCapture() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {
        }
        imageReader = null
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
    }

    private fun displaySize(): Triple<Int, Int, Int> {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            val bounds = wm.maximumWindowMetrics.bounds
            Triple(bounds.width(), bounds.height(), resources.displayMetrics.densityDpi)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        }
    }

    private fun capturingNotification(): Notification =
        androidx.core.app.NotificationCompat.Builder(this, NotificationController.CHANNEL_ASK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NotiAsk 正在截屏")
            .setContentText("正在捕获当前屏幕")
            .setOngoing(true)
            .setSilent(true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
            .build()

    companion object {
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "data"
        private const val SHADE_SETTLE_MS = 450L
        private const val FIRST_FRAME_MS = 280L
        private const val TIMEOUT_MS = 3000L

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenshotCaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.projectionData(): Intent? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else getParcelableExtra(EXTRA_DATA)
}
