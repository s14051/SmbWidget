package com.example.smbwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.io.IOException


/**
 * Implementation of App Widget functionality.
 */
private const val IMG_LEFT_ACTION = "imgLeftAction"
private const val IMG_RIGHT_ACTION = "imgRightAction"
private const val SONG_PREV_ACTION = "songPrevAction"
private const val SONG_NEXT_ACTION = "songNextAction"
private const val SONG_START_ACTION = "songStartAction"
private const val SONG_STOP_ACTION = "songStopAction"
private const val SONG_PAUSE_ACTION = "songPauseAction"

private val images = intArrayOf(R.raw.img1, R.raw.img2, R.raw.img3, R.raw.img4)
private var imgIdx = 0
private val songs = intArrayOf(R.raw.song1, R.raw.song2, R.raw.song3)
private val songTitles = arrayOf("song1", "song2", "song3")
private var songIdx = 0
private var mediaPlayer: MediaPlayer? = null

class SmbWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val views = RemoteViews(context.packageName, R.layout.smb_widget)

        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer.create(context, songs[songIdx])

        when (intent.action){
            IMG_LEFT_ACTION -> swipeImage(-1, views, context)
            IMG_RIGHT_ACTION -> swipeImage(1, views, context)
            SONG_PREV_ACTION, SONG_NEXT_ACTION, SONG_START_ACTION, SONG_STOP_ACTION, SONG_PAUSE_ACTION -> {
                manageMusicPlayerAction(intent.action!!, context)
                views.setTextViewText(R.id.tv_song_title, songTitles[songIdx])
                refreshWidget(context, views)
            }
        }

        // buttons visibility management
        when (intent.action) {
            SONG_START_ACTION -> {
                views.setViewVisibility(R.id.btn_song_start, View.GONE)
                views.setViewVisibility(R.id.btn_song_stop, View.VISIBLE)
                views.setViewVisibility(R.id.btn_song_pause, View.VISIBLE)
            }
            SONG_STOP_ACTION -> {
                views.setViewVisibility(R.id.btn_song_start, View.VISIBLE)
                views.setViewVisibility(R.id.btn_song_stop, View.GONE)
                views.setViewVisibility(R.id.btn_song_pause, View.GONE)
            }
            SONG_PAUSE_ACTION -> {
                views.setViewVisibility(R.id.btn_song_start, View.VISIBLE)
                views.setViewVisibility(R.id.btn_song_stop, View.VISIBLE)
                views.setViewVisibility(R.id.btn_song_pause, View.GONE)
            }
        }
    }

    private fun swipeImage(swipeBy: Int, views: RemoteViews, context: Context){
        imgIdx += swipeBy

        // manage index overflow
        if (imgIdx >= images.size) {
            imgIdx = 0
        }
        else if (imgIdx < 0) {
            imgIdx = images.size - 1
        }

        views.setImageViewResource(R.id.imageView, images[imgIdx])

        refreshWidget(context, views)
    }

    private fun manageMusicPlayerAction(action: String, context: Context){
        when (action){
            SONG_PREV_ACTION -> changeSong(-1, context)
            SONG_NEXT_ACTION -> changeSong(1, context)
            SONG_START_ACTION -> mediaPlayer?.start()
            SONG_STOP_ACTION -> {
                mediaPlayer?.stop()
                try {
                    mediaPlayer?.prepare()
                }
                catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            SONG_PAUSE_ACTION -> mediaPlayer?.pause()
        }
    }

    private fun changeSong(swipeBy: Int, context: Context) {
        songIdx += swipeBy

        // manage index overflow
        if (songIdx >= songs.size) {
            songIdx = 0
        }
        else if (songIdx < 0) {
            songIdx = songs.size - 1
        }

        val wasPlaying = mediaPlayer?.isPlaying!!

        mediaPlayer?.reset()
        val afd: AssetFileDescriptor = context.resources.openRawResourceFd(songs[songIdx])
        try {
            mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mediaPlayer?.prepare()

            if (wasPlaying)
                mediaPlayer?.start()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun refreshWidget(context: Context, views: RemoteViews) {
        val manager = AppWidgetManager.getInstance(context)
        manager.updateAppWidget(ComponentName(context, SmbWidget::class.java), views)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.smb_widget)

    // website
    val goToWebsiteIntent = Intent(Intent.ACTION_VIEW)
    goToWebsiteIntent.data = Uri.parse("http://www.pja.edu.pl")
    val goToWebsitePendingIntent = PendingIntent.getActivity(context, 0, goToWebsiteIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_website, goToWebsitePendingIntent)

    // img initial
    views.setImageViewResource(R.id.imageView, images[imgIdx])

    // img left
    val imgLeftIntent = Intent(context, SmbWidget::class.java)
    imgLeftIntent.action = IMG_LEFT_ACTION
    val imgLeftPendingIntent = PendingIntent.getBroadcast(context, 0, imgLeftIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_img_left, imgLeftPendingIntent)

    // img right
    val imgRightIntent = Intent(context, SmbWidget::class.java)
    imgRightIntent.action = IMG_RIGHT_ACTION
    val imgRightPendingIntent = PendingIntent.getBroadcast(context, 0, imgRightIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_img_right, imgRightPendingIntent)

    // set current song title
    views.setTextViewText(R.id.tv_song_title, songTitles[songIdx])

    // song prev
    val songPrevIntent = Intent(context, SmbWidget::class.java)
    songPrevIntent.action = SONG_PREV_ACTION
    val songPrevPendingIntent = PendingIntent.getBroadcast(context, 0, songPrevIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_song_prev, songPrevPendingIntent)

    // song next
    val songNextIntent = Intent(context, SmbWidget::class.java)
    songNextIntent.action = SONG_NEXT_ACTION
    val songNextPendingIntent = PendingIntent.getBroadcast(context, 0, songNextIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_song_next, songNextPendingIntent)

    // song start
    val songStartIntent = Intent(context, SmbWidget::class.java)
    songStartIntent.action = SONG_START_ACTION
    val songStartPendingIntent = PendingIntent.getBroadcast(context, 0, songStartIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_song_start, songStartPendingIntent)

    // song stop
    val songStopIntent = Intent(context, SmbWidget::class.java)
    songStopIntent.action = SONG_STOP_ACTION
    val songStopPendingIntent = PendingIntent.getBroadcast(context, 0, songStopIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_song_stop, songStopPendingIntent)

    // song pause
    val songPauseIntent = Intent(context, SmbWidget::class.java)
    songPauseIntent.action = SONG_PAUSE_ACTION
    val songPausePendingIntent = PendingIntent.getBroadcast(context, 0, songPauseIntent, 0)
    views.setOnClickPendingIntent(R.id.btn_song_pause, songPausePendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}