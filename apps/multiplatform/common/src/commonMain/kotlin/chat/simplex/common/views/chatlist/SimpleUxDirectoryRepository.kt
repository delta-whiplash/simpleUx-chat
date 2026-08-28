package chat.simplex.common.views.chatlist

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import chat.simplex.common.platform.Log
import chat.simplex.common.views.helpers.generalGetString
import chat.simplex.res.MR

data class SimpleUxDirectoryGroup(
  val name: String,
  val description: String,
  val link: String,
  val category: String,
  val members: String,
  val imageUrl: String? = null,
  // Marks the synthetic directory-bot entry: its display strings live in the UI layer (localized at
  // render time) because this repository is not a Composable and must not hard-code user-visible text.
  val isDirectoryBot: Boolean = false
)

object SimpleUxDirectoryRepository {
  private const val TAG = "DirectoryRepo"
  private const val DIRECTORY_LISTING_URL = "https://directory.simplex.chat/data/listing.json"
  private const val DIRECTORY_PROMOTED_URL = "https://directory.simplex.chat/data/promoted.json"
  private const val DATA_BASE_URL = "https://directory.simplex.chat/data/"

  val directoryBot = SimpleUxDirectoryGroup(
    name = "SimpleX Directory Bot",
    description = "",
    link = "https://smp4.simplex.im/a#lXUjJW5vHYQzoLYgmi8GbxkGP41_kjefFvBrdwg-0Ok",
    category = "",
    members = "",
    isDirectoryBot = true
  )

  private val _groups = MutableStateFlow<List<SimpleUxDirectoryGroup>>(listOf(directoryBot))
  val groups: StateFlow<List<SimpleUxDirectoryGroup>> = _groups.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private var lastFetchTime = 0L
  private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes cache

  private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  fun fetchDirectoryIfNeeded(scope: CoroutineScope, force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (!force && _groups.value.size > 1 && (now - lastFetchTime < CACHE_TTL_MS)) {
      return
    }
    scope.launch(Dispatchers.IO) {
      fetchDirectoryInternal()
    }
  }

  private fun fetchDirectoryInternal() {
    _isLoading.value = true
    try {
      val jsonContent = fetchUrlContent(DIRECTORY_LISTING_URL) ?: fetchUrlContent(DIRECTORY_PROMOTED_URL)
      if (jsonContent != null) {
        val parsedGroups = parseDirectoryJson(jsonContent)
        if (parsedGroups.isNotEmpty()) {
          _groups.value = listOf(directoryBot) + parsedGroups
          lastFetchTime = System.currentTimeMillis()
          Log.d(TAG, "Successfully loaded ${parsedGroups.size} groups dynamically from SimpleX directory")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error fetching SimpleX directory: ${e.message}")
    } finally {
      _isLoading.value = false
    }
  }

  private fun fetchUrlContent(urlString: String): String? {
    return try {
      val url = URL(urlString)
      val conn = url.openConnection() as HttpURLConnection
      conn.connectTimeout = 8000
      conn.readTimeout = 10000
      conn.requestMethod = "GET"
      conn.setRequestProperty("User-Agent", "SimpleUX-Chat/1.0")
      conn.setRequestProperty("Accept", "application/json")

      if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        conn.inputStream.bufferedReader().use { it.readText() }
      } else {
        Log.w(TAG, "HTTP error ${conn.responseCode} fetching $urlString")
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to connect to $urlString: ${e.message}")
      null
    }
  }

  private fun parseDirectoryJson(rawJson: String): List<SimpleUxDirectoryGroup> {
    val result = mutableListOf<SimpleUxDirectoryGroup>()
    try {
      val root = jsonParser.parseToJsonElement(rawJson).jsonObject
      val entries = root["entries"]?.jsonArray ?: return emptyList()

      for (element in entries) {
        val entry = element.jsonObject
        val name = entry["displayName"]?.jsonPrimitive?.content ?: continue

        // Extract link
        val groupLinkObj = entry["groupLink"]?.jsonObject
        val shortLink = groupLinkObj?.get("connShortLink")?.jsonPrimitive?.content
        val fullLink = groupLinkObj?.get("connFullLink")?.jsonPrimitive?.content
        val link = shortLink ?: fullLink ?: continue

        // Extract description
        var desc = ""
        val welcomeArray = entry["welcomeMessage"]?.jsonArray
        if (welcomeArray != null && welcomeArray.isNotEmpty()) {
          for (part in welcomeArray) {
            val textPart = part.jsonObject["text"]?.jsonPrimitive?.content
            if (!textPart.isNullOrBlank()) {
              desc += textPart
            }
          }
        }
        if (desc.isBlank()) {
          val shortDescArray = entry["shortDescr"]?.jsonArray
          if (shortDescArray != null) {
            for (part in shortDescArray) {
              val textPart = part.jsonObject["text"]?.jsonPrimitive?.content
              if (!textPart.isNullOrBlank()) {
                desc += textPart
              }
            }
          }
        }
        if (desc.isBlank()) {
          desc = generalGetString(MR.strings.directory_group_default_desc)
        } else {
          // Clean up first line or rules for concise display
          desc = desc.lines().firstOrNull { it.isNotBlank() && !it.startsWith("Link to join") } ?: desc.trim()
        }

        // Extract category / type
        val entryTypeObj = entry["entryType"]?.jsonObject
        val typeStr = entryTypeObj?.get("groupType")?.jsonPrimitive?.content
          ?: entryTypeObj?.get("type")?.jsonPrimitive?.content
          ?: "group"
        val category = when (typeStr.lowercase()) {
          "channel" -> generalGetString(MR.strings.directory_category_channel)
          else -> generalGetString(MR.strings.directory_category_group)
        }

        // Extract members
        val summaryObj = entryTypeObj?.get("summary")?.jsonObject
        val membersCount = summaryObj?.get("currentMembers")?.jsonPrimitive?.content?.toIntOrNull()
          ?: summaryObj?.get("members")?.jsonPrimitive?.content?.toIntOrNull()
        val membersStr = if (membersCount != null && membersCount > 0) generalGetString(MR.strings.directory_members_count).format(membersCount) else generalGetString(MR.strings.directory_members_public_group)

        val imgFile = entry["imageFile"]?.jsonPrimitive?.content
        val imgUrl = if (!imgFile.isNullOrBlank()) "$DATA_BASE_URL$imgFile" else null

        result.add(
          SimpleUxDirectoryGroup(
            name = name,
            description = desc,
            link = link,
            category = category,
            members = membersStr,
            imageUrl = imgUrl
          )
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing directory JSON: ${e.message}")
    }
    return result
  }
}
