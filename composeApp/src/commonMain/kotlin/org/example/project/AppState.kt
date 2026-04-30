package org.example.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

sealed class Screen {
    data object Home : Screen()
    data object ArticleList : Screen()
    data class ArticleForm(val articleId: UInt) : Screen()
    data object LocationList : Screen()
    data class LocationForm(val locationId: UInt) : Screen()
    data class LocationAssignments(val locationId: UInt, val locationName: String) : Screen()
    data class ArticleAssignments(val articleId: UInt, val articleName: String) : Screen()
    data object Info : Screen()
    data object HowToHelp : Screen()
    data object Statistics : Screen()
    data object Settings : Screen()
    data class LicenseDetail(val licenseType: LicenseType) : Screen()
}

class AppState(
    private val jsonDataManager: JsonDataManager,
    private val imageManager: ImageManager,
    private val settingsManager: JsonSettingsManager,
    private val fileHandler: FileHandler,
    private val fileDownloader: FileDownloader,
    val scope: CoroutineScope
) {
    private val _navStack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val navStack: StateFlow<List<Screen>> = _navStack.asStateFlow()
    val currentScreen get() = _navStack.value.last()

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    private val _assignments = MutableStateFlow<List<Assignment>>(emptyList())
    val assignments: StateFlow<List<Assignment>> = _assignments.asStateFlow()

    private val _showNotImplementedDialog = MutableStateFlow(false)
    val showNotImplementedDialog: StateFlow<Boolean> = _showNotImplementedDialog.asStateFlow()

    private val _errorDialogMessage = MutableStateFlow<String?>(null)
    val errorDialogMessage: StateFlow<String?> = _errorDialogMessage.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _showExportSuccessDialog = MutableStateFlow(false)
    val showExportSuccessDialog: StateFlow<Boolean> = _showExportSuccessDialog.asStateFlow()

    private val _showImportSuccessDialog = MutableStateFlow(false)
    val showImportSuccessDialog: StateFlow<Boolean> = _showImportSuccessDialog.asStateFlow()

    private val _showDirtyBuildWarning = MutableStateFlow(false)
    val showDirtyBuildWarning: StateFlow<Boolean> = _showDirtyBuildWarning.asStateFlow()

    private val _showFutureVersionWarning = MutableStateFlow(false)
    val showFutureVersionWarning: StateFlow<Boolean> = _showFutureVersionWarning.asStateFlow()

    private val _showAppExpiredWarning = MutableStateFlow(false)
    val showAppExpiredWarning: StateFlow<Boolean> = _showAppExpiredWarning.asStateFlow()

    private val _showRootedDeviceWarning = MutableStateFlow(false)
    val showRootedDeviceWarning: StateFlow<Boolean> = _showRootedDeviceWarning.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    // --- Navigation ---

    fun navigateTo(screen: Screen) {
        _navStack.value = _navStack.value + screen
    }

    fun navigateBack() {
        if (_navStack.value.size > 1) {
            _navStack.value = _navStack.value.dropLast(1)
        }
    }

    fun removeScreensFromStack(predicate: (Screen) -> Boolean) {
        _navStack.value = _navStack.value.filterNot(predicate)
    }

    // --- Data Loading ---

    suspend fun reloadAllData() {
        try {
            val data = withContext(Dispatchers.Default) { jsonDataManager.load() }
            _articles.value = data.articles
            _locations.value = data.locations
            _assignments.value = data.assignments
        } catch (e: Exception) {
            val errorMessage = "Failed to load data: ${e.message}. The data file might be corrupt."
            _errorDialogMessage.value = errorMessage
            Logger.log(LogLevel.ERROR, "Failed to load data in reloadAllData: ${e.message}", e)
        }
        Logger.log(LogLevel.INFO, "Data reloaded")
        Logger.logImportantFiles(LogLevel.TRACE)
    }

    // --- Initialization ---

    @OptIn(ExperimentalTime::class)
    suspend fun initialize() {
        val loadedSettings = withContext(Dispatchers.Default) { settingsManager.loadSettings() }
        _settings.value = loadedSettings
        Logger.updateSettings(loadedSettings)
        setAppLanguage(loadedSettings.language)

        withContext(Dispatchers.Default) { settingsManager.saveSettings(loadedSettings) }
        _settings.value = settingsManager.settings

        reloadAllData()

        if (isDeviceRooted()) {
            _showRootedDeviceWarning.value = true
            return
        }

        val expirationDays = GeneratedVersionInfo.EXPIRATION_DAYS
        if (expirationDays > 0) {
            try {
                val buildInstant = Instant.parse(GeneratedVersionInfo.BUILD_DATE)
                val nowInstantVal = nowInstant()
                val expirationSeconds = expirationDays.toLong() * 24 * 60 * 60
                if ((nowInstantVal - buildInstant).inWholeSeconds > expirationSeconds) {
                    _showAppExpiredWarning.value = true
                    return
                }
            } catch (_: Exception) { }
        }

        if (GeneratedVersionInfo.IS_DIRTY == "dirty") {
            _showDirtyBuildWarning.value = true
        }
        val savedVersionInfo = loadedSettings.versionInfo
        if (savedVersionInfo.commitDate.isNotEmpty()) {
            try {
                val savedBuildInstant = Instant.parse(savedVersionInfo.commitDate)
                val currentBuildInstant = Instant.parse(GeneratedVersionInfo.COMMIT_DATE)
                if (savedBuildInstant > currentBuildInstant) {
                    _showFutureVersionWarning.value = true
                }
            } catch (_: Exception) { }
        }
    }

    // --- Article operations ---

    fun addArticle(defaultName: String) {
        scope.launch {
            val newArticle = jsonDataManager.createNewArticle(defaultName)
            withContext(Dispatchers.Default) { jsonDataManager.addOrUpdateArticle(newArticle) }
            reloadAllData()
            navigateTo(Screen.ArticleForm(newArticle.id))
        }
    }

    fun saveArticle(editedArticle: Article, newImages: Map<UInt, ByteArray>, onDone: (() -> Unit)? = null) {
        scope.launch {
            val existingArticle = try {
                jsonDataManager.getArticleById(editedArticle.id)
            } catch (e: Exception) {
                Logger.log(LogLevel.ERROR, "Failed to get article ${editedArticle.id} for save: ${e.message}", e)
                null
            }
            val existingImageIds = existingArticle?.imageIds ?: emptyList()
            val newImagesToUpload = newImages.filter { it.key !in existingImageIds }
            val idsToDelete = existingImageIds.filter { it !in newImages.keys }

            withContext(Dispatchers.Default) {
                idsToDelete.forEach { imageId ->
                    imageManager.deleteArticleImage(editedArticle.id, imageId)
                }
                newImagesToUpload.entries.forEach { (imageId, imageData) ->
                    imageManager.saveArticleImage(editedArticle.id, imageId, imageData)
                }
                jsonDataManager.addOrUpdateArticle(editedArticle)
            }
            reloadAllData()
            onDone?.invoke()
        }
    }

    fun deleteArticle(articleId: UInt) {
        scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    val articleToDelete = jsonDataManager.getArticleById(articleId)
                    articleToDelete!!.imageIds.forEach { imageId ->
                        imageManager.deleteArticleImage(articleId, imageId)
                    }
                    jsonDataManager.deleteArticle(articleId)
                }
                reloadAllData()
                removeScreensFromStack { it is Screen.ArticleForm && it.articleId == articleId }
            } catch (e: Exception) {
                Logger.log(LogLevel.ERROR, "Failed to delete article with id $articleId: ${e.message}", e)
                _errorDialogMessage.value = "Failed to delete article: ${e.message}"
            }
        }
    }

    fun addArticleVariant(articleToCopy: Article) {
        scope.launch {
            val newArticleWithNewId = jsonDataManager.createNewArticle(articleToCopy.name)
            val newArticle = newArticleWithNewId.copy(
                brand = articleToCopy.brand,
                abbreviation = articleToCopy.abbreviation,
                minimumAmount = articleToCopy.minimumAmount,
                defaultExpirationDays = articleToCopy.defaultExpirationDays,
                notes = articleToCopy.notes
            )
            withContext(Dispatchers.Default) { jsonDataManager.addOrUpdateArticle(newArticle) }
            reloadAllData()
            navigateTo(Screen.ArticleForm(newArticle.id))
        }
    }

    fun setArticleAssignments(articleId: UInt, updatedAssignments: List<Assignment>) {
        scope.launch {
            withContext(Dispatchers.Default) {
                jsonDataManager.setArticleAssignments(articleId, updatedAssignments)
            }
            reloadAllData()
        }
    }

    // --- Location operations ---

    fun addLocation(defaultName: String) {
        scope.launch {
            val newLocation = jsonDataManager.createNewLocation(defaultName)
            withContext(Dispatchers.Default) { jsonDataManager.addOrUpdateLocation(newLocation) }
            reloadAllData()
            navigateTo(Screen.LocationForm(newLocation.id))
        }
    }

    fun saveLocation(editedLocation: Location, newImages: Map<UInt, ByteArray>, onDone: (() -> Unit)? = null) {
        scope.launch {
            val existingLocation = try {
                jsonDataManager.getLocationById(editedLocation.id)
            } catch (e: Exception) {
                Logger.log(LogLevel.ERROR, "Failed to get location ${editedLocation.id} for save: ${e.message}", e)
                null
            }
            val existingImageIds = existingLocation?.imageIds ?: emptyList()
            val newImagesToUpload = newImages.filter { it.key !in existingImageIds }
            val idsToDelete = existingImageIds.filter { it !in newImages.keys }

            withContext(Dispatchers.Default) {
                idsToDelete.forEach { imageId ->
                    imageManager.deleteLocationImage(editedLocation.id, imageId)
                }
                newImagesToUpload.entries.sortedBy { it.key }.forEach { (imageId, imageData) ->
                    imageManager.saveLocationImage(editedLocation.id, imageId, imageData)
                }
                jsonDataManager.addOrUpdateLocation(editedLocation)
            }
            reloadAllData()
            onDone?.invoke()
        }
    }

    fun deleteLocation(locationId: UInt) {
        scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    val locationToDelete = jsonDataManager.getLocationById(locationId)
                    locationToDelete!!.imageIds.forEach { imageId ->
                        imageManager.deleteLocationImage(locationId, imageId)
                    }
                    jsonDataManager.deleteLocation(locationId)
                }
                reloadAllData()
                removeScreensFromStack {
                    (it is Screen.LocationForm && it.locationId == locationId) ||
                    (it is Screen.LocationAssignments && it.locationId == locationId)
                }
            } catch (e: Exception) {
                Logger.log(LogLevel.ERROR, "Failed to delete location with id $locationId: ${e.message}", e)
                _errorDialogMessage.value = "Failed to delete location: ${e.message}"
            }
        }
    }

    fun setLocationAssignments(locationId: UInt, updatedAssignments: List<Assignment>) {
        scope.launch {
            withContext(Dispatchers.Default) {
                jsonDataManager.setLocationAssignments(locationId, updatedAssignments)
            }
            reloadAllData()
        }
    }

    // --- Settings operations ---

    fun changeSettings(newSettings: Settings) {
        scope.launch {
            withContext(Dispatchers.Default) { settingsManager.saveSettings(newSettings) }
            Logger.updateSettings(newSettings)
            _settings.value = newSettings
        }
    }

    fun changeLocale(newLocale: String) {
        val newSettings = _settings.value.copy(language = newLocale)
        scope.launch {
            withContext(Dispatchers.Default) { settingsManager.saveSettings(newSettings) }
            setAppLanguage(newLocale)
            Logger.updateSettings(newSettings)
            _settings.value = newSettings
        }
    }

    fun changeLogLevel(newLogLevel: LogLevel) {
        val newSettings = _settings.value.copy(logLevel = newLogLevel)
        scope.launch {
            withContext(Dispatchers.Default) { settingsManager.saveSettings(newSettings) }
            Logger.updateSettings(newSettings)
            _settings.value = newSettings
        }
    }

    fun changeEnableExpirationDates(enabled: Boolean) {
        val newSettings = _settings.value.copy(enableExpirationDates = enabled)
        scope.launch {
            withContext(Dispatchers.Default) { settingsManager.saveSettings(newSettings) }
            _settings.value = newSettings
        }
    }

    // --- Export / Import ---

    fun exportZip() {
        scope.launch {
            try {
                _isExporting.value = true
                val exportFileName = fileHandler.createTimestampedFileName("mistermanager", "zip")
                withContext(Dispatchers.Default) {
                    fileDownloader.download(exportFileName, fileHandler.zipFiles(), getContext())
                }
                _isExporting.value = false
                _showExportSuccessDialog.value = true
            } catch (e: Exception) {
                _isExporting.value = false
                _errorDialogMessage.value = "Failed to export: ${e.message}"
                Logger.log(LogLevel.ERROR, "Failed to export ZIP: ${e.message}", e)
            }
        }
    }

    fun importJson(fileContent: String) {
        scope.launch {
            try {
                withContext(Dispatchers.Default) { jsonDataManager.importData(fileContent) }
                reloadAllData()
                _snackbarEvents.emit("Import successful")
            } catch (e: Exception) {
                val errorMessage = "Failed to import data: ${e.message}. The data file might be corrupt."
                _errorDialogMessage.value = errorMessage
                Logger.log(LogLevel.ERROR, "Failed to import data in importJson: ${e.message}", e)
            }
        }
    }

    fun importZip(zipInputStream: Any) {
        scope.launch {
            try {
                _isImporting.value = true

                val backupFolderName = "backup"
                try { fileHandler.deleteBackupDirectory(backupFolderName) } catch (_: Exception) { }
                fileHandler.renameFilesDirectory(backupFolderName)

                withContext(Dispatchers.Default) { fileHandler.unzipAndReplaceFiles(zipInputStream) }

                try { fileHandler.deleteBackupDirectory(backupFolderName) } catch (_: Exception) { }

                reloadAllData()
                _isImporting.value = false
                _showImportSuccessDialog.value = true
            } catch (e: Exception) {
                _isImporting.value = false
                _errorDialogMessage.value = "Failed to import ZIP: ${e.message}"
                Logger.log(LogLevel.ERROR, "Failed to import ZIP in importZip: ${e.message}", e)
            }
        }
    }

    // --- Dialog dismissers ---

    fun dismissNotImplementedDialog() { _showNotImplementedDialog.value = false }
    fun dismissErrorDialog() { _errorDialogMessage.value = null }
    fun dismissDirtyBuildWarning() { _showDirtyBuildWarning.value = false }
    fun dismissFutureVersionWarning() { _showFutureVersionWarning.value = false }
    fun dismissExportSuccessDialog() { _showExportSuccessDialog.value = false }
    fun dismissImportSuccessDialog() { _showImportSuccessDialog.value = false }
}
