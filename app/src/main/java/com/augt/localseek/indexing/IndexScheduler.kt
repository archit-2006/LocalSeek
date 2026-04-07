package com.augt.localseek.indexing

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object IndexScheduler {

    private const val TAG_ONE_TIME = "index_once"
    private const val TAG_PERIODIC = "index_periodic"

    /**
     * Run one full index scan right now.
     * We use ExistingWorkPolicy.KEEP so if the user spam-clicks "Sync",
     * it doesn't queue up 50 jobs.
     */
    fun scheduleImmediateIndex(context: Context) {
        val request = OneTimeWorkRequestBuilder<IndexWorker>()
            .addTag(TAG_ONE_TIME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                TAG_ONE_TIME,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    /**
     * Schedule a recurring background index every 6 hours to catch new files.
     */
    fun schedulePeriodicIndex(context: Context) {
        // We add constraints so it doesn't drain the user's battery when low
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<IndexWorker>(6, TimeUnit.HOURS)
            .addTag(TAG_PERIODIC)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                TAG_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}