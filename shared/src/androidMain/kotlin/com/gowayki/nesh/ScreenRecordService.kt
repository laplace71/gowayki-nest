package com.gowayki.nesh

import android.app.*
import android.content.Context
import android.content.pm.ServiceInfo
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

/**
 * Servicio foreground para grabación de pantalla.
 * Mantiene activa la MediaProjection mientras se graba.
 */
class ScreenRecordService : Service() {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    
    companion object {
        private const val NOTIFICATION_ID = 9003
        private const val CHANNEL_ID = "screen_record_channel"
        private const val CHANNEL_NAME = "Grabación de Pantalla"
        
        const val ACTION_START = "com.gowayki.nesh.ACTION_START_RECORDING"
        const val ACTION_STOP = "com.gowayki.nesh.ACTION_STOP_RECORDING"
        const val ACTION_PAUSE = "com.gowayki.nesh.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME = "com.gowayki.nesh.ACTION_RESUME_RECORDING"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        const val EXTRA_OUTPUT_FILE = "output_file"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_DPI = "dpi"
        
        // Callback estático para notificar a la implementación
        var onRecordingStopped: ((filePath: String?) -> Unit)? = null
        var onRecordingError: ((error: String) -> Unit)? = null
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }
                val outputFile = intent.getStringExtra(EXTRA_OUTPUT_FILE) ?: return START_NOT_STICKY
                val width = intent.getIntExtra(EXTRA_WIDTH, 1280)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 720)
                val dpi = intent.getIntExtra(EXTRA_DPI, 320)
                
                startRecording(resultCode, data, outputFile, width, height, dpi)
            }
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indica que se está grabando la pantalla"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Grabando pantalla")
            .setContentText("Wayki está grabando tu pantalla")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Detener", stopPendingIntent)
            .build()
    }
    
    @Suppress("DEPRECATION")
    private fun startRecording(
        resultCode: Int,
        data: Intent?,
        outputFile: String,
        width: Int,
        height: Int,
        dpi: Int
    ) {
        try {
            // Iniciar foreground notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            
            // Configurar MediaProjection
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data!!)
            
            // Configurar MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile)
                setVideoSize(width, height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(5 * 1024 * 1024) // 5 Mbps
                setVideoFrameRate(30)
                prepare()
            }
            
            // Crear VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "WaykiScreenRecord",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null, null
            )
            
            mediaRecorder?.start()
            
        } catch (e: Exception) {
            e.printStackTrace()
            onRecordingError?.invoke(e.message ?: "Error al iniciar grabación")
            stopSelf()
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            virtualDisplay?.release()
            mediaProjection?.stop()
            
            onRecordingStopped?.invoke(null)
        } catch (e: Exception) {
            e.printStackTrace()
            onRecordingError?.invoke(e.message ?: "Error al detener grabación")
        } finally {
            mediaRecorder = null
            virtualDisplay = null
            mediaProjection = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
    
    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
