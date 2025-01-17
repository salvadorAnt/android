package org.owntracks.android.ui.preferences.load

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import java.net.URI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.greenrobot.eventbus.EventBus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.preferences.InMemoryPreferencesStore
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.PreferencesStore
import org.owntracks.android.support.Parser

@OptIn(ExperimentalCoroutinesApi::class)
class LoadViewModelTest {
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore
    private val eventBus: EventBus = mock {}

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun createMocks() {
        mockContext = mock {
            on { packageName } doReturn javaClass.canonicalName
        }
        preferencesStore = InMemoryPreferencesStore()
    }

    @Test
    fun `Given an inline OwnTracks config URL with invalid JSON, when loading it into the LoadViewModel, then the error is correctly set`() =
        runTest {
            val parser = Parser(null)
            val preferences = Preferences(preferencesStore)
            val vm = LoadViewModel(preferences, parser, InMemoryWaypointsRepo(eventBus), UnconfinedTestDispatcher())
            vm.extractPreferences(URI("owntracks:///config?inline=e30="))
            assertEquals(ImportStatus.FAILED, vm.configurationImportStatus.value)
            assertEquals("Message is not a valid configuration message", vm.importError.value)
        }

    @Test
    fun `Given an inline OwnTracks config URL with a simple MessageConfiguration JSON, when loading it into the LoadViewModel, then the correct config is displayed`() =
        runTest {
            val parser = Parser(null)
            val preferences = Preferences(preferencesStore)
            val vm = LoadViewModel(preferences, parser, InMemoryWaypointsRepo(eventBus), UnconfinedTestDispatcher())
            vm.extractPreferences(URI("owntracks:///config?inline=eyJfdHlwZSI6ImNvbmZpZ3VyYXRpb24ifQ"))
            val expectedConfig = """
            {
              "_type" : "configuration",
              "waypoints" : [ ]
            }
            """.trimIndent()
            assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
            assertEquals(expectedConfig, vm.displayedConfiguration.value)
        }

    @Test
    fun `Given an inline OwnTracks config URL with a simple MessageWaypoints JSON, when loading it into the LoadViewModel, then the correct waypoints are displayed`() =
        runTest {
            val parser = Parser(null)
            val preferences = Preferences(preferencesStore)
            val vm = LoadViewModel(preferences, parser, InMemoryWaypointsRepo(eventBus), UnconfinedTestDispatcher())
            vm.extractPreferences(
                URI(
                    "owntracks:///config?inline=eyJfdHlwZSI6IndheXBvaW50cyIsIndheXBvaW50cyI6W3siX3R5cGUiOiJ3YXlwb2ludCIsImRlc2MiOiJUZXN0IFdheXBvaW50IiwibGF0Ijo1MSwibG9uIjowLCJyYWQiOjQ1MCwidHN0IjoxNTk4NDUxMzcyfV19" // ktlint-disable max-line-length
                )
            )
            val expectedConfig = """
            {
              "_type" : "waypoints",
              "waypoints" : [ {
                "_type" : "waypoint",
                "desc" : "Test Waypoint",
                "lat" : 51.0,
                "lon" : 0.0,
                "rad" : 450,
                "tst" : 1598451372
              } ]
            }
            """.trimIndent()
            assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
            assertEquals(expectedConfig, vm.displayedConfiguration.value)
        }

    @Test
    fun `Given a configuration ByteArray with a simple configuration, when loading it into the LoadViewModel, then the correct config is displayed`() =
        runTest {
            val parser = Parser(null)
            val preferences = Preferences(preferencesStore)
            val vm = LoadViewModel(preferences, parser, InMemoryWaypointsRepo(eventBus), UnconfinedTestDispatcher())
            val expectedConfig = """
            {
              "_type" : "configuration",
              "waypoints" : [ ]
            }
            """.trimIndent()
            vm.extractPreferences(expectedConfig.toByteArray())
            assertEquals(ImportStatus.SUCCESS, vm.configurationImportStatus.value)
            assertEquals(expectedConfig, vm.displayedConfiguration.value)
        }

    @Test
    fun `Given a configuration with waypoints, when loading and then saving into the LoadViewModel, then the preferences and waypointsrepo are updated`() =
        runTest {
            val parser = Parser(null)
            val preferences = Preferences(preferencesStore)
            val waypointsRepo = InMemoryWaypointsRepo(eventBus)
            val vm = LoadViewModel(preferences, parser, waypointsRepo, UnconfinedTestDispatcher())
            val config = """
            {
              "_type":"configuration",
              "waypoints":[
                {
                  "_type":"waypoint",
                  "desc":"Test Waypoint",
                  "lat":51.0,
                  "lon":0.0,
                  "rad":450,
                  "tst":1598451372
                }
              ],
              "clientId": "testClientId"
            }
            """.trimIndent()
            vm.extractPreferences(config.toByteArray())
            vm.saveConfiguration()
            assertEquals(ImportStatus.SAVED, vm.configurationImportStatus.value)
            assertEquals(1, waypointsRepo.all.size)
            assertEquals("testClientId", preferences.clientId)
        }
}
