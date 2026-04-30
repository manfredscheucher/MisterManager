package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mistermanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.NoSuchElementException

@Composable
fun App(jsonDataManager: JsonDataManager, imageManager: ImageManager, fileDownloader: FileDownloader, fileHandler: FileHandler, settingsManager: JsonSettingsManager) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val appState = remember { AppState(jsonDataManager, imageManager, settingsManager, fileHandler, fileDownloader, scope) }

    val navStack by appState.navStack.collectAsState()
    val screen = navStack.last()
    val settings by appState.settings.collectAsState()
    val articles by appState.articles.collectAsState()
    val locations by appState.locations.collectAsState()
    val assignments by appState.assignments.collectAsState()
    val showNotImplementedDialog by appState.showNotImplementedDialog.collectAsState()
    val errorDialogMessage by appState.errorDialogMessage.collectAsState()
    val isExporting by appState.isExporting.collectAsState()
    val isImporting by appState.isImporting.collectAsState()
    val showExportSuccessDialog by appState.showExportSuccessDialog.collectAsState()
    val showImportSuccessDialog by appState.showImportSuccessDialog.collectAsState()
    val showDirtyBuildWarning by appState.showDirtyBuildWarning.collectAsState()
    val showFutureVersionWarning by appState.showFutureVersionWarning.collectAsState()
    val showAppExpiredWarning by appState.showAppExpiredWarning.collectAsState()
    val showRootedDeviceWarning by appState.showRootedDeviceWarning.collectAsState()

    LaunchedEffect(Unit) { appState.initialize() }

    LaunchedEffect(Unit) {
        appState.snackbarEvents.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(screen) {
        val screenName = when (val s = screen) {
            is Screen.Home -> "Home"
            is Screen.ArticleList -> "ArticleList"
            is Screen.ArticleForm -> "ArticleForm(articleId=${s.articleId})"
            is Screen.LocationList -> "LocationList"
            is Screen.LocationForm -> "LocationForm(locationId=${s.locationId})"
            is Screen.LocationAssignments -> "LocationAssignments(locationId=${s.locationId}, locationName='${s.locationName}')"
            is Screen.ArticleAssignments -> "ArticleAssignments(articleId=${s.articleId}, articleName='${s.articleName}')"
            is Screen.Info -> "Info"
            is Screen.HowToHelp -> "HowToHelp"
            is Screen.Statistics -> "Statistics"
            is Screen.Settings -> "Settings"
            is Screen.LicenseDetail -> "LicenseDetail(licenseType=${s.licenseType})"
        }
        Logger.log(LogLevel.INFO, "Navigating to screen: $screenName")
        Logger.logImportantFiles(LogLevel.TRACE)
    }

    if (showNotImplementedDialog) {
        AlertDialog(
            onDismissRequest = { appState.dismissNotImplementedDialog() },
            title = { Text(stringResource(Res.string.not_implemented_title)) },
            text = { Text(stringResource(Res.string.not_implemented_message)) },
            confirmButton = {
                TextButton(onClick = { appState.dismissNotImplementedDialog() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { appState.dismissErrorDialog() },
            title = { Text("Error") },
            text = { Text(errorDialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { appState.dismissErrorDialog() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    if (showRootedDeviceWarning) {
        AlertDialog(
            onDismissRequest = { /* not dismissable */ },
            title = { Text(stringResource(Res.string.warning_rooted_device_title)) },
            text = { Text(stringResource(Res.string.warning_rooted_device_message)) },
            confirmButton = {
                TextButton(onClick = { exitApp() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    if (showAppExpiredWarning) {
        AlertDialog(
            onDismissRequest = { /* not dismissable */ },
            title = { Text(stringResource(Res.string.warning_app_expired_title)) },
            text = { Text(stringResource(Res.string.warning_app_expired_message)) },
            confirmButton = {
                TextButton(onClick = { exitApp() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    if (showDirtyBuildWarning) {
        AlertDialog(
            onDismissRequest = { appState.dismissDirtyBuildWarning() },
            title = { Text(stringResource(Res.string.warning_dirty_build_title)) },
            text = { Text(stringResource(Res.string.warning_dirty_build_message)) },
            confirmButton = {
                TextButton(onClick = { appState.dismissDirtyBuildWarning() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    if (showFutureVersionWarning) {
        AlertDialog(
            onDismissRequest = { appState.dismissFutureVersionWarning() },
            title = { Text(stringResource(Res.string.warning_future_version_title)) },
            text = { Text(stringResource(Res.string.warning_future_version_message)) },
            confirmButton = {
                TextButton(onClick = { appState.dismissFutureVersionWarning() }) {
                    Text(stringResource(Res.string.common_ok))
                }
            }
        )
    }

    MaterialTheme(
        colorScheme = LightColorScheme
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
        ) { innerPadding ->
            key(settings.language, settings.logLevel) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                ) {
                    when (val s = screen) {
                        Screen.Home -> HomeScreen(
                            onOpenArticles = { appState.navigateTo(Screen.ArticleList) },
                            onOpenLocations = { appState.navigateTo(Screen.LocationList) },
                            onOpenInfo = { appState.navigateTo(Screen.Info) },
                            onOpenStatistics = { appState.navigateTo(Screen.Statistics) },
                            onOpenSettings = { appState.navigateTo(Screen.Settings) },
                            onOpenHowToHelp = { appState.navigateTo(Screen.HowToHelp) }
                        )

                        Screen.ArticleList -> {
                            val defaultArticleName = stringResource(Res.string.article_new_default_name)
                            ArticleListScreen(
                                articles = articles.sortedByDescending { it.modified },
                                locations = locations,
                                imageManager = imageManager,
                                assignments = assignments,
                                settings = settings,
                                onAddClick = { appState.addArticle(defaultArticleName) },
                                onOpen = { id -> appState.navigateTo(Screen.ArticleForm(id)) },
                                onBack = { appState.navigateBack() },
                                onSettingsChange = { newSettings -> appState.changeSettings(newSettings) }
                            )
                        }

                        is Screen.ArticleForm -> {
                            val existingArticle = remember(s.articleId, articles) {
                                try {
                                    jsonDataManager.getArticleById(s.articleId)
                                } catch (e: NoSuchElementException) {
                                    scope.launch {
                                        Logger.log(LogLevel.ERROR, "Failed to get article by id ${s.articleId} in ArticleForm: ${e.message}", e)
                                    }
                                    null
                                }
                            }
                            var articleImagesMap by remember { mutableStateOf<Map<UInt, ByteArray>>(emptyMap()) }

                            LaunchedEffect(s.articleId, existingArticle) {
                                val imageMap = mutableMapOf<UInt, ByteArray>()
                                existingArticle?.imageIds?.forEach { imageId ->
                                    try {
                                        withContext(Dispatchers.Default) {
                                            imageManager.getArticleImage(existingArticle.id, imageId)
                                                ?.let {
                                                    imageMap[imageId] = it
                                                } ?: scope.launch {
                                                Logger.log(LogLevel.WARN, "Image not found for article ${existingArticle.id}, imageId $imageId")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            Logger.log(LogLevel.ERROR, "Failed to load image for article ${existingArticle.id}, imageId $imageId: ${e.message}", e)
                                        }
                                    }
                                }
                                articleImagesMap = imageMap
                            }

                            if (existingArticle == null) {
                                LaunchedEffect(s.articleId) { appState.navigateBack() }
                            } else {
                                val relatedAssignments = assignments.filter { it.articleId == existingArticle.id }
                                ArticleFormScreen(
                                    initial = existingArticle,
                                    initialImages = articleImagesMap,
                                    assignmentsForArticle = relatedAssignments,
                                    allLocations = locations,
                                    locationById = { pid ->
                                        locations.firstOrNull { it.id == pid }.also {
                                            if (it == null) {
                                                scope.launch {
                                                    Logger.log(LogLevel.WARN, "Location with id $pid not found, referenced by article ${existingArticle.id}")
                                                }
                                            }
                                        }
                                    },
                                    imageManager = imageManager,
                                    settings = settings,
                                    onBack = { appState.navigateBack() },
                                    onDelete = { articleIdToDelete -> appState.deleteArticle(articleIdToDelete) },
                                    onSave = { editedArticle, newImages, callback ->
                                        appState.saveArticle(editedArticle, newImages, callback)
                                    },
                                    onAddColor = { articleToCopy -> appState.addArticleVariant(articleToCopy) },
                                    onNavigateToAssignments = {
                                        appState.navigateTo(Screen.ArticleAssignments(
                                            existingArticle.id,
                                            existingArticle.name
                                        ))
                                    },
                                    onNavigateToLocation = { locationId -> appState.navigateTo(Screen.LocationForm(locationId)) }
                                )
                            }
                        }

                        Screen.LocationList -> {
                            val defaultLocationName = stringResource(Res.string.location_new_default_name)
                            LocationListScreen(
                                locations = locations,
                                assignments = assignments,
                                imageManager = imageManager,
                                settings = settings,
                                onAddClick = { appState.addLocation(defaultLocationName) },
                                onOpen = { id -> appState.navigateTo(Screen.LocationForm(id)) },
                                onBack = { appState.navigateBack() },
                                onSettingsChange = { newSettings -> appState.changeSettings(newSettings) }
                            )
                        }

                        is Screen.LocationForm -> {
                            val existingLocation = remember(s.locationId, locations) {
                                try {
                                    jsonDataManager.getLocationById(s.locationId)
                                } catch (e: NoSuchElementException) {
                                    scope.launch {
                                        Logger.log(LogLevel.ERROR, "Failed to get location by id ${s.locationId} in LocationForm: ${e.message}", e)
                                    }
                                    null
                                }
                            }
                            var locationImagesMap by remember { mutableStateOf<Map<UInt, ByteArray>>(emptyMap()) }

                            LaunchedEffect(s.locationId, existingLocation) {
                                val imageMap = mutableMapOf<UInt, ByteArray>()
                                existingLocation?.imageIds?.forEach { imageId ->
                                    try {
                                        withContext(Dispatchers.Default) {
                                            imageManager.getLocationImage(existingLocation.id, imageId)
                                                ?.let {
                                                    imageMap[imageId] = it
                                                } ?: scope.launch {
                                                Logger.log(LogLevel.WARN, "Image not found for location ${existingLocation.id}, imageId $imageId")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        scope.launch {
                                            Logger.log(LogLevel.ERROR, "Failed to load image for location ${existingLocation.id}, imageId $imageId: ${e.message}", e)
                                        }
                                    }
                                }
                                locationImagesMap = imageMap
                            }

                            if (existingLocation == null) {
                                LaunchedEffect(s.locationId) { appState.navigateBack() }
                            } else {
                                val assignmentsForCurrentLocation =
                                    assignments.filter { it.locationId == existingLocation.id }
                                LocationFormScreen(
                                    initial = existingLocation,
                                    initialImages = locationImagesMap,
                                    assignmentsForLocation = assignmentsForCurrentLocation,
                                    articleById = { articleId ->
                                        articles.firstOrNull { it.id == articleId }.also {
                                            if (it == null) {
                                                scope.launch {
                                                    Logger.log(LogLevel.WARN, "Article with id $articleId not found, referenced by location ${existingLocation.id}")
                                                }
                                            }
                                        }
                                    },
                                    imageManager = imageManager,
                                    settings = settings,
                                    onBack = { appState.navigateBack() },
                                    onDelete = { locationIdToDelete -> appState.deleteLocation(locationIdToDelete) },
                                    onSave = { editedLocation, newImages, callback ->
                                        appState.saveLocation(editedLocation, newImages, callback)
                                    },
                                    onNavigateToAssignments = {
                                        appState.navigateTo(Screen.LocationAssignments(
                                            existingLocation.id,
                                            existingLocation.name
                                        ))
                                    },
                                    onNavigateToArticle = { articleId -> appState.navigateTo(Screen.ArticleForm(articleId)) }
                                )
                            }
                        }

                        is Screen.LocationAssignments -> {
                            val initialAssignmentsForLocation = assignments.filter { it.locationId == s.locationId }

                            LocationAssignmentsScreen(
                                locationName = s.locationName,
                                locationId = s.locationId,
                                allArticles = articles,
                                allLocations = locations,
                                initialAssignments = initialAssignmentsForLocation,
                                settings = settings,
                                onCreateNewAssignment = { articleId, locationId ->
                                    jsonDataManager.createNewAssignment(articleId, locationId)
                                },
                                onSave = { updatedAssignments -> appState.setLocationAssignments(s.locationId, updatedAssignments) },
                                onBack = { appState.navigateBack() }
                            )
                        }

                        is Screen.ArticleAssignments -> {
                            val initialAssignmentsForArticle = assignments.filter { it.articleId == s.articleId }

                            ArticleAssignmentsScreen(
                                articleName = s.articleName,
                                articleId = s.articleId,
                                allLocations = locations,
                                initialAssignments = initialAssignmentsForArticle,
                                settings = settings,
                                onCreateNewAssignment = { articleId, locationId ->
                                    jsonDataManager.createNewAssignment(articleId, locationId)
                                },
                                onSave = { updatedAssignments -> appState.setArticleAssignments(s.articleId, updatedAssignments) },
                                onBack = { appState.navigateBack() }
                            )
                        }

                        Screen.Info -> {
                            InfoScreen(
                                onBack = { appState.navigateBack() },
                                onNavigateToHelp = { appState.navigateTo(Screen.HowToHelp) },
                                onNavigateToLicense = { licenseType ->
                                    appState.navigateTo(Screen.LicenseDetail(licenseType))
                                }
                            )
                        }

                        is Screen.LicenseDetail -> {
                            LicenseDetailScreen(
                                licenseType = (screen as Screen.LicenseDetail).licenseType,
                                onBack = { appState.navigateBack() }
                            )
                        }

                        Screen.HowToHelp -> {
                            HowToHelpScreen(onBack = { appState.navigateBack() })
                        }

                        Screen.Statistics -> {
                            StatisticsScreen(
                                articles = articles,
                                locations = locations,
                                assignments = assignments,
                                onBack = { appState.navigateBack() },
                                settings = settings
                            )
                        }

                        Screen.Settings -> {
                            SettingsScreen(
                                currentLocale = settings.language,
                                currentLogLevel = settings.logLevel,
                                enableExpirationDates = settings.enableExpirationDates,
                                fileHandler = fileHandler,
                                onBack = { appState.navigateBack() },
                                onExportZip = { appState.exportZip() },
                                isExporting = isExporting,
                                isImporting = isImporting,
                                showExportSuccessDialog = showExportSuccessDialog,
                                showImportSuccessDialog = showImportSuccessDialog,
                                onDismissExportSuccess = { appState.dismissExportSuccessDialog() },
                                onDismissImportSuccess = { appState.dismissImportSuccessDialog() },
                                onImport = { fileContent -> appState.importJson(fileContent) },
                                onImportZip = { zipInputStream -> appState.importZip(zipInputStream) },
                                onLocaleChange = { newLocale -> appState.changeLocale(newLocale) },
                                onLogLevelChange = { newLogLevel -> appState.changeLogLevel(newLogLevel) },
                                onEnableExpirationDatesChange = { enabled -> appState.changeEnableExpirationDates(enabled) }
                            )
                        }
                    }
                }
            }
        }
    }
}
