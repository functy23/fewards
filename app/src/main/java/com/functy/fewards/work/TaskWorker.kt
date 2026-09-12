package com.functy.fewards.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.functy.fewards.ui.viewmodel.TaskRunner

/**
 * WorkManager 包装：手动执行经由这里入队，保证进程被杀后任务仍能执行。
 */
class TaskWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val runWb = inputData.getBoolean(KEY_RUN_WB, false)
        val runMhy = inputData.getBoolean(KEY_RUN_MHY, false)
        return try {
            TaskRunner.execute(runWb, runMhy)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_RUN_WB = "run_wb"
        const val KEY_RUN_MHY = "run_mhy"

        fun enqueue(context: Context, runWb: Boolean, runMhy: Boolean) {
            val request = OneTimeWorkRequestBuilder<TaskWorker>()
                .setInputData(
                    workDataOf(
                        KEY_RUN_WB to runWb,
                        KEY_RUN_MHY to runMhy,
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
