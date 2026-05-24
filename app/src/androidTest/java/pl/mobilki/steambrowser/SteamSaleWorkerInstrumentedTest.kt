package pl.mobilki.steambrowser

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SteamSaleWorkerInstrumentedTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = if (android.os.Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant("android.permission.POST_NOTIFICATIONS")
    } else {
        GrantPermissionRule.grant()
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testShowNotificationWithMockData() = runBlocking {
        // 1. Mock Repository
        val mockRepository = mockk<FavoritesRepository>()
        coEvery { mockRepository.favoritesFlow } returns flowOf(setOf(292030)) // The Witcher 3

        // 2. Mock API Response
        val mockApiService = mockk<SteamApiService>()
        val mockResponseJson = """
            {
              "292030": {
                "success": true,
                "data": {
                  "name": "Wiedźmin 3: Dziki Gon",
                  "price_overview": {
                    "discount_percent": 50,
                    "final_formatted": "50,00 zł"
                  }
                }
              }
            }
        """.trimIndent()
        val mockResponse = Json.parseToJsonElement(mockResponseJson).jsonObject
        coEvery { mockApiService.getAppDetails(292030) } returns mockResponse

        // 3. Setup WorkerFactory to inject mocks
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return SteamSaleWorker(appContext, workerParameters, mockRepository, mockApiService)
            }
        }

        // 4. Build and run the worker
        val worker = TestListenableWorkerBuilder<SteamSaleWorker>(context)
            .setWorkerFactory(factory)
            .build()

        val result = worker.doWork()

        // 5. Verify result
        assertEquals(ListenableWorker.Result.success(), result)
        
        // At this point, a notification should be visible on the emulator screen.
        // We sleep for a few seconds so the user can actually see it.
        kotlinx.coroutines.delay(5000)
    }
}
