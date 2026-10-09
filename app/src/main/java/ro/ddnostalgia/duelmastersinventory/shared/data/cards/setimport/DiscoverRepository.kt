package ro.ddnostalgia.duelmastersinventory.shared.data.cards.setimport

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Expected `target` value in a Discover feed response - any other value means the URL isn't a DMInventory set feed and is rejected. */
private const val EXPECTED_TARGET = "DMInventory"

/**
 * Fetches the list of downloadable set packs from the app's hardcoded CDN
 * feed and downloads a chosen pack's zip for [CardSetImportRepository] to
 * import (see "dynamic sets" spec). The feed URL is fixed - not user
 * configurable - since the CDN repo is Duel Masters Inventory's own
 * (daydreamstalgia.github.io), not a general-purpose plugin surface.
 */
@Singleton
class DiscoverRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CATALOG_URL = "https://daydreamstalgia.github.io/static/duelmastersinventory/sets.json"
    }

    private val gson = Gson()

    suspend fun fetchCatalog(): List<DiscoverSetEntry> = withContext(Dispatchers.IO) {
        val json = httpGetString(CATALOG_URL)
        val catalog = gson.fromJson(json, DiscoverCatalog::class.java)
        if (catalog.target != EXPECTED_TARGET) {
            throw IllegalStateException("Feed did not identify as $EXPECTED_TARGET (got '${catalog.target}')")
        }
        catalog.content.orEmpty()
    }

    /** Downloads [entry]'s zip into the app's cache dir and returns the file. Caller is responsible for deleting it once imported. */
    suspend fun downloadZip(entry: DiscoverSetEntry): File = withContext(Dispatchers.IO) {
        val dest = File(context.cacheDir, "dmi_set_download_${System.currentTimeMillis()}.zip")
        val connection = URL(entry.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        dest
    }

    private fun httpGetString(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
