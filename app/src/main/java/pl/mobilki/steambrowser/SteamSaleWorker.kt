package pl.mobilki.steambrowser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

class SteamSaleWorker(
    context: Context,
    params: WorkerParameters,
    private val repositoryOverride: FavoritesRepository? = null,
    private val apiServiceOverride: SteamApiService? = null
) : CoroutineWorker(context, params) {

    private val repository = repositoryOverride ?: FavoritesRepository(context)
    private val apiService = apiServiceOverride ?: DefaultSteamApiService()

    companion object {
        const val CHANNEL_ID = "steam_sales"
        const val CHANNEL_NAME = "Promocje Steam"
    }

    override suspend fun doWork(): Result {
        val favoriteIds = repository.favoritesFlow.first()
        if (favoriteIds.isEmpty()) return Result.success()

        for (appId in favoriteIds) {
            try {
                val response = apiService.getAppDetails(appId)
                checkAndNotify(appId, response)
                delay(2000)
            } catch (e: Exception) {
            }
        }

        return Result.success()
    }

    private fun checkAndNotify(appId: Int, response: JsonObject) {
        val gameData = (response[appId.toString()] as? JsonObject) ?: return
        val success = (gameData["success"] as? JsonPrimitive)?.content == "true"
        if (!success) return

        val data = (gameData["data"] as? JsonObject) ?: return
        val name = (data["name"] as? JsonPrimitive)?.contentOrNull ?: "Gra #$appId"
        val priceOverview = (data["price_overview"] as? JsonObject) ?: return

        val discountPercent = (priceOverview["discount_percent"] as? JsonPrimitive)?.intOrNull ?: 0
        val finalPriceFormatted = (priceOverview["final_formatted"] as? JsonPrimitive)?.contentOrNull ?: ""

        if (discountPercent > 0) {
            showNotification(appId, name, discountPercent, finalPriceFormatted)
        }
    }

    private fun showNotification(appId: Int, name: String, discount: Int, price: String) {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Promocja na Steam!")
            .setContentText("$name jest teraz o $discount% taniej ($price)!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(appId, notification)
        } catch (e: SecurityException) {
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
