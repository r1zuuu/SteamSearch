package pl.mobilki.steambrowser

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class SteamSaleWorkerTest {

    private lateinit var context: Context
    private val repository = mockk<FavoritesRepository>()
    private val apiService = mockk<SteamApiService>()
    private val notificationManager = mockk<NotificationManager>(relaxed = true)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        mockkStatic(NotificationManagerCompat::class)
        val nmc = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(any()) } returns nmc
    }

    @Test
    fun `doWork triggers notification for discounted games with Polish characters`() = runBlocking {
        val appIdDiscounted = 12345
        val appIdNotDiscounted = 67890
        val gameName = "Wiedźmin 3: Dziki Gon"

        coEvery { repository.favoritesFlow } returns flowOf(setOf(appIdDiscounted, appIdNotDiscounted))
        
        val discountedJson = """
            {
                "$appIdDiscounted": {
                    "success": true,
                    "data": {
                        "name": "$gameName",
                        "price_overview": {
                            "discount_percent": 50,
                            "final_formatted": "50,00 zł"
                        }
                    }
                }
            }
        """.trimIndent()
        
        val notDiscountedJson = """
            {
                "$appIdNotDiscounted": {
                    "success": true,
                    "data": {
                        "name": "Gra bez zniżki",
                        "price_overview": {
                            "discount_percent": 0,
                            "final_formatted": "100,00 zł"
                        }
                    }
                }
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        
        coEvery { apiService.getAppDetails(appIdDiscounted) } returns 
                json.parseToJsonElement(discountedJson).jsonObject
        coEvery { apiService.getAppDetails(appIdNotDiscounted) } returns 
                json.parseToJsonElement(notDiscountedJson).jsonObject

        val factory = object : androidx.work.WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: androidx.work.WorkerParameters
            ): ListenableWorker? {
                return SteamSaleWorker(appContext, workerParameters, repository, apiService)
            }
        }

        val worker = TestListenableWorkerBuilder<SteamSaleWorker>(context)
            .setWorkerFactory(factory)
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        val nmc = NotificationManagerCompat.from(context)
        verify(exactly = 1) { 
            nmc.notify(appIdDiscounted, any())
        }
        verify(exactly = 0) {
            nmc.notify(appIdNotDiscounted, any())
        }
    }
}
