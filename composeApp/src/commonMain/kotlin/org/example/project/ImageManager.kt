package org.example.project

class ImageManager(private val fileHandler: FileHandler) {

    companion object {
        private const val THUMBNAIL_WIDTH = 256
        private const val THUMBNAIL_HEIGHT = 256
    }
    private val locationImagesDir = "img/location"
    private val locationImageThumbnailsDir = "img/location/thumbnails"
    private val articleImagesDir = "img/article"
    private val articleImageThumbnailsDir = "img/article/thumbnails"

    private fun getLocationImagePath(locationId: Int, imageId: Int) = "$locationImagesDir/${locationId}_$imageId.jpg"
    private fun getLocationImageThumbnailPath(locationId: Int, imageId: Int) = "$locationImageThumbnailsDir/${locationId}_${imageId}_${THUMBNAIL_WIDTH}x${THUMBNAIL_HEIGHT}.jpg"

    private fun getArticleImagePath(articleId: Int, imageId: Int) = "$articleImagesDir/${articleId}_$imageId.jpg"
    private fun getArticleImageThumbnailPath(articleId: Int, imageId: Int) = "$articleImageThumbnailsDir/${articleId}_${imageId}_${THUMBNAIL_WIDTH}x${THUMBNAIL_HEIGHT}.jpg"


    suspend fun saveLocationImage(locationId: Int, imageId: Int, image: ByteArray) {
        fileHandler.writeBytes(getLocationImagePath(locationId, imageId), image)
    }

    suspend fun getLocationImage(locationId: Int, imageId: Int): ByteArray? {
        return fileHandler.readBytes(getLocationImagePath(locationId, imageId))
    }

    fun getLocationImageInputStream(locationId: Int, imageId: Int): FileInputSource? {
        return fileHandler.openInputStream(getLocationImagePath(locationId, imageId))
    }

    suspend fun getLocationImageThumbnail(locationId: Int, imageId: Int): ByteArray? {
        val thumbnailPath = getLocationImageThumbnailPath(locationId, imageId)
        val cachedThumbnail = fileHandler.readBytes(thumbnailPath)
        if (cachedThumbnail != null) {
            return cachedThumbnail
        }

        val imageBytes = getLocationImage(locationId, imageId)
        return imageBytes?.let {
            val resizedImage = resizeImage(it, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            resizedImage?.let {
                fileHandler.writeBytes(thumbnailPath, it)
            }
            resizedImage
        }
    }

    suspend fun deleteLocationImage(locationId: Int, imageId: Int) {
        fileHandler.deleteFile(getLocationImagePath(locationId, imageId))
        fileHandler.deleteFile(getLocationImageThumbnailPath(locationId, imageId))
    }

    suspend fun saveArticleImage(articleId: Int, imageId: Int, image: ByteArray) {
        fileHandler.writeBytes(getArticleImagePath(articleId, imageId), image)
    }

    suspend fun getArticleImage(articleId: Int, imageId: Int): ByteArray? {
        return fileHandler.readBytes(getArticleImagePath(articleId, imageId))
    }

    fun getArticleImageInputStream(articleId: Int, imageId: Int): FileInputSource? {
        return fileHandler.openInputStream(getArticleImagePath(articleId, imageId))
    }

    suspend fun getArticleImageThumbnail(articleId: Int, imageId: Int): ByteArray? {
        val thumbnailPath = getArticleImageThumbnailPath(articleId, imageId)
        val cachedThumbnail = fileHandler.readBytes(thumbnailPath)
        if (cachedThumbnail != null) {
            return cachedThumbnail
        }

        val imageBytes = getArticleImage(articleId, imageId)
        return imageBytes?.let {
            val resizedImage = resizeImage(it, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            resizedImage?.let {
                fileHandler.writeBytes(thumbnailPath, it)
            }
            resizedImage
        }
    }

    suspend fun deleteArticleImage(articleId: Int, imageId: Int) {
        fileHandler.deleteFile(getArticleImagePath(articleId, imageId))
        fileHandler.deleteFile(getArticleImageThumbnailPath(articleId, imageId))
    }
}
