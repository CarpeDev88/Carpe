package app.carpe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import app.carpe.core.ScreenAuditAnalyzer
import app.carpe.core.ScreenAuditStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object ScreenAuditIntents {
    const val EXTRA_RESULT_CODE = "result_code"
    const val EXTRA_RESULT_DATA = "result_data"
    const val ACTION_STOP = "app.carpe.screen_audit.stop"
}

class ScreenAuditService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val store by lazy { ScreenAuditStore(this) }
    private val analyzer = ScreenAuditAnalyzer()
    private val recognizerLazy = lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val recognizer by recognizerLazy
    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var processing = false
    private var stopped = false
    private var sampleCount = 0
    private var stopAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ScreenAuditIntents.ACTION_STOP) {
            stopAudit()
            return START_NOT_STICKY
        }
        if (projection != null) return START_NOT_STICKY
        val resultCode = intent?.getIntExtra(ScreenAuditIntents.EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.let { readProjectionData(it) }
        if (data == null || resultCode != android.app.Activity.RESULT_OK) {
            fail("Screen capture permission was not granted.")
            return START_NOT_STICKY
        }
        try {
            startForegroundCompat()
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            val currentProjection = projection ?: error("Screen capture could not start.")
            currentProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() = stopAudit()
            }, handler)

            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            val density = metrics.densityDpi.coerceAtLeast(1)
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = currentProjection.createVirtualDisplay(
                "CarpeFeedAudit", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, handler
            ) ?: error("Screen capture display could not start.")

            stopped = false
            sampleCount = 0
            stopAt = SystemClock.elapsedRealtime() + MAX_SESSION_MILLIS
            store.setActive(true)
            store.setError(null)
            handler.postDelayed(sample, FIRST_SAMPLE_DELAY)
            handler.postDelayed(sessionTimeout, MAX_SESSION_MILLIS)
        } catch (_: Exception) {
            fail("Screen audit could not start. Try again from the Mirror screen.")
        }
        return START_NOT_STICKY
    }

    private val sample = object : Runnable {
        override fun run() {
            if (stopped || SystemClock.elapsedRealtime() >= stopAt || sampleCount >= MAX_SAMPLES) {
                stopAudit()
                return
            }
            if (!processing) imageReader?.acquireLatestImage()?.let(::analyzeImage)
            if (!stopped) handler.postDelayed(this, SAMPLE_INTERVAL_MILLIS)
        }
    }

    private val sessionTimeout = Runnable { stopAudit() }

    private fun analyzeImage(image: Image) {
        val bitmap = try {
            imageToBitmap(image)
        } catch (_: Exception) {
            image.close()
            return
        }
        image.close()
        processing = true
        sampleCount++
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                if (!stopped) {
                    analyzer.observe(result.text)
                    store.save(analyzer.report())
                }
            }
            .addOnFailureListener {
                if (!stopped) store.setError("Some screen samples could not be read. The audit will keep running.")
            }
            .addOnCompleteListener {
                bitmap.recycle()
                processing = false
            }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val pixelStride = plane.pixelStride
        val rowPadding = (plane.rowStride - pixelStride * image.width) / pixelStride
        val padded = Bitmap.createBitmap(image.width + rowPadding, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }

    @Suppress("DEPRECATION")
    private fun readProjectionData(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(ScreenAuditIntents.EXTRA_RESULT_DATA, Intent::class.java)
        else intent.getParcelableExtra(ScreenAuditIntents.EXTRA_RESULT_DATA)

    private fun startForegroundCompat() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Screen insight session", NotificationManager.IMPORTANCE_LOW))
        }
        val stopIntent = PendingIntent.getService(
            this, 9,
            Intent(this, ScreenAuditService::class.java).setAction(ScreenAuditIntents.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("CARPE screen audit is active")
            .setContentText("Screen samples are analyzed on this device. Tap Stop to end now.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    private fun fail(message: String) {
        store.setError(message)
        store.setActive(false)
        stopped = true
        handler.removeCallbacks(sample)
        handler.removeCallbacks(sessionTimeout)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.stop()
        projection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAudit() {
        if (stopped) return
        stopped = true
        handler.removeCallbacks(sample)
        handler.removeCallbacks(sessionTimeout)
        store.save(analyzer.report())
        store.setActive(false)
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        val currentProjection = projection
        projection = null
        currentProjection?.stop()
        if (recognizerLazy.isInitialized()) recognizer.close()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (!stopped) stopAudit()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "carpe_screen_audit"
        const val NOTIFICATION_ID = 9181
        const val FIRST_SAMPLE_DELAY = 1500L
        const val SAMPLE_INTERVAL_MILLIS = 3000L
        const val MAX_SESSION_MILLIS = 120_000L
        const val MAX_SAMPLES = 40
    }

}
