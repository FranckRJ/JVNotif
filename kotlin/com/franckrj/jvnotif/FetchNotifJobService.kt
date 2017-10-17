package com.franckrj.jvnotif

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.app.job.JobInfo
import android.content.ComponentName
import android.app.job.JobScheduler
import com.franckrj.jvnotif.utils.FetchNotifTool

@TargetApi(21)
class FetchNotifJobService : JobService() {
    private val toolForFetchNotif: FetchNotifTool = FetchNotifTool(this)
    private var currentJobParam: JobParameters? = null

    private val fetchNotifIsFinishedListener = object : FetchNotifTool.FetchNotifIsFinished {
        override fun onFetchNotifIsFinished() {
            if (currentJobParam != null) {
                jobFinished(currentJobParam, false)
                currentJobParam = null
            }
        }
    }

    init {
        toolForFetchNotif.showToasts = false
    }

    companion object {
        private val fetchNotifJobId: Int = 1456

        fun initJobScheduler(context: Context) {
            val jobScheduler: JobScheduler = (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler)
            val jobInfoBuilder: JobInfo.Builder = JobInfo.Builder(fetchNotifJobId, ComponentName(context, FetchNotifJobService::class.java))

            jobInfoBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                          .setPeriodic(FetchNotifTool.repeatTime)
                          .setPersisted(true)

            jobScheduler.schedule(jobInfoBuilder.build())
        }
    }

    override fun onStartJob(param: JobParameters?): Boolean {
        if (currentJobParam == null) {
            currentJobParam = param
            toolForFetchNotif.fetchNotifIsFinishedListener = fetchNotifIsFinishedListener
            toolForFetchNotif.startFetchNotif()
            return true
        }

        return false
    }

    override fun onStopJob(param: JobParameters?): Boolean {
        if (currentJobParam != null) {
            toolForFetchNotif.fetchNotifIsFinishedListener = null
            toolForFetchNotif.stopFetchNotif()
            currentJobParam = null
        }
        return false
    }
}
