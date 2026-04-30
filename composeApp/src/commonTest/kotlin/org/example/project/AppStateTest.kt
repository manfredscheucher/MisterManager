package org.example.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppStateTest {

    private fun makeAppState(scope: CoroutineScope): AppState {
        val fileHandler = createPlatformFileHandler()
        Logger.init(fileHandler, Settings())
        val jsonDataManager = JsonDataManager(fileHandler)
        val imageManager = ImageManager(fileHandler)
        val settingsManager = JsonSettingsManager(fileHandler, "settings.json")
        val fileDownloader = FileDownloader()
        return AppState(jsonDataManager, imageManager, settingsManager, fileHandler, fileDownloader, scope)
    }

    // Wait for all child coroutines launched via scope.launch in AppState
    // to finish (including their Dispatchers.Default sub-tasks).
    private suspend fun CoroutineScope.awaitAllJobs() {
        coroutineContext.job.children.toList().forEach { it.join() }
    }

    @Test
    fun initialScreenIsHome() {
        runBlocking {
            val appState = makeAppState(this)
            assertEquals(listOf<Screen>(Screen.Home), appState.navStack.value)
            assertEquals(Screen.Home, appState.currentScreen)
        }
    }

    @Test
    fun navigateToAddsScreenToStack() {
        runBlocking {
            val appState = makeAppState(this)
            appState.navigateTo(Screen.ArticleList)
            assertEquals(listOf(Screen.Home, Screen.ArticleList), appState.navStack.value)
        }
    }

    @Test
    fun navigateBackRemovesLastScreen() {
        runBlocking {
            val appState = makeAppState(this)
            appState.navigateTo(Screen.ArticleList)
            appState.navigateBack()
            assertEquals(listOf<Screen>(Screen.Home), appState.navStack.value)
        }
    }

    @Test
    fun navigateBackDoesNothingAtRoot() {
        runBlocking {
            val appState = makeAppState(this)
            appState.navigateBack()
            assertEquals(listOf<Screen>(Screen.Home), appState.navStack.value)
        }
    }

    @Test
    fun addArticleCreatesArticleAndNavigatesToArticleForm() {
        runBlocking {
            val appState = makeAppState(this)
            appState.addArticle("Test Article")
            awaitAllJobs()
            assertEquals(1, appState.articles.value.size)
            assertIs<Screen.ArticleForm>(appState.currentScreen)
        }
    }

    @Test
    fun saveArticlePersistsNameInArticlesList() {
        runBlocking {
            val appState = makeAppState(this)
            appState.addArticle("Initial Name")
            awaitAllJobs()
            val article = appState.articles.value.first()
            appState.saveArticle(article.copy(name = "Updated Name"), emptyMap())
            awaitAllJobs()
            assertEquals("Updated Name", appState.articles.value.first().name)
        }
    }

    @Test
    fun deleteArticleRemovesFromListAndPopsNavStack() {
        runBlocking {
            val appState = makeAppState(this)
            appState.addArticle("To Delete")
            awaitAllJobs()
            val articleId = appState.articles.value.first().id
            appState.deleteArticle(articleId)
            awaitAllJobs()
            assertTrue(appState.articles.value.isEmpty())
            assertEquals(Screen.Home, appState.currentScreen)
        }
    }

    @Test
    fun addLocationCreatesLocationAndNavigatesToLocationForm() {
        runBlocking {
            val appState = makeAppState(this)
            appState.addLocation("Test Location")
            awaitAllJobs()
            assertEquals(1, appState.locations.value.size)
            assertIs<Screen.LocationForm>(appState.currentScreen)
        }
    }

    @Test
    fun saveLocationPersistsNameInLocationsList() {
        runBlocking {
            val appState = makeAppState(this)
            appState.addLocation("Initial Location")
            awaitAllJobs()
            val location = appState.locations.value.first()
            appState.saveLocation(location.copy(name = "Renamed Location"), emptyMap())
            awaitAllJobs()
            assertEquals("Renamed Location", appState.locations.value.first().name)
        }
    }

    @Test
    fun deleteLocationRemovesFromListAndPopsNavStack() {
        runBlocking {
            val appState = makeAppState(this)
            appState.addLocation("To Delete")
            awaitAllJobs()
            val locationId = appState.locations.value.first().id
            appState.deleteLocation(locationId)
            awaitAllJobs()
            assertTrue(appState.locations.value.isEmpty())
            assertEquals(Screen.Home, appState.currentScreen)
        }
    }

    @Test
    fun changeSettingsUpdatesSettingsState() {
        runBlocking {
            val appState = makeAppState(this)
            appState.changeSettings(Settings(logLevel = LogLevel.DEBUG))
            awaitAllJobs()
            assertEquals(LogLevel.DEBUG, appState.settings.value.logLevel)
        }
    }

    @Test
    fun changeEnableExpirationDatesUpdatesSettingsState() {
        runBlocking {
            val appState = makeAppState(this)
            appState.changeEnableExpirationDates(false)
            awaitAllJobs()
            assertEquals(false, appState.settings.value.enableExpirationDates)
        }
    }
}
