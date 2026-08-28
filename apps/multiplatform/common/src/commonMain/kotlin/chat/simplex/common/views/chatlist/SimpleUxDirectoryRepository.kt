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

/**
 * Language-neutral result of parsing one directory listing entry — pure data with no localized
 * strings, so it can be unit-tested offline.
 *
 * @param description  cleaned listing text, or null when the directory provided no usable text
 *   (the localized default description is substituted at render time).
 * @param isChannel    true when the entry type is a channel (vs. the default group).
 * @param membersCount positive member count, or null when absent/zero/negative/unparsable
 *   (the localized "public group" string is substituted at render time).
 */
internal data class SimpleUxDirectoryEntry(
  val name: String,
  val link: String,
  val description: String?,
  val isChannel: Boolean,
  val membersCount: Int?,
  val imageUrl: String?
)

object SimpleUxDirectoryRepository {
  private const val DIRECTORY_LISTING_URL = "https://directory.simplex.chat/data/listing.json"
  private const val DIRECTORY_PROMOTED_URL = "https://directory.simplex.chat/data/promoted.json"

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

  private fun parseDirectoryJson(rawJson: String): List<SimpleUxDirectoryGroup> =
    parseDirectoryEntries(rawJson).map { e ->
      SimpleUxDirectoryGroup(
        name = e.name,
        description = e.description ?: generalGetString(MR.strings.directory_group_default_desc),
        link = e.link,
        category = if (e.isChannel) generalGetString(MR.strings.directory_category_channel) else generalGetString(MR.strings.directory_category_group),
        members = if (e.membersCount != null) generalGetString(MR.strings.directory_members_count).format(e.membersCount) else generalGetString(MR.strings.directory_members_public_group),
        imageUrl = e.imageUrl
      )
    }
}

private const val TAG = "DirectoryRepo"
private const val DATA_BASE_URL = "https://directory.simplex.chat/data/"

private val directoryJsonParser = Json {
  ignoreUnknownKeys = true
  isLenient = true
}

/**
 * Pure structural parse of the directory listing JSON (extracted from [SimpleUxDirectoryRepository]
 * so the grammar can be unit-tested without network or localized resources).
 *
 * Behaviour is unchanged from the previous inline implementation:
 *  - entries without a displayName or without any link are skipped;
 *  - description comes from welcomeMessage parts, falling back to shortDescr parts, with the first
 *    non-blank line that is not a "Link to join" line kept (whole trimmed text when all lines filter out);
 *  - one structurally corrupt entry aborts parsing and returns the entries accumulated so far.
 */
internal fun parseDirectoryEntries(rawJson: String): List<SimpleUxDirectoryEntry> {
  val result = mutableListOf<SimpleUxDirectoryEntry>()
  try {
    val root = directoryJsonParser.parseToJsonElement(rawJson).jsonObject
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
      val description = if (desc.isBlank()) {
        null // localized default description substituted at render time
      } else {
        // Clean up first line or rules for concise display
        desc.lines().firstOrNull { it.isNotBlank() && !it.startsWith("Link to join") } ?: desc.trim()
      }

      // Extract category / type
      val entryTypeObj = entry["entryType"]?.jsonObject
      val typeStr = entryTypeObj?.get("groupType")?.jsonPrimitive?.content
        ?: entryTypeObj?.get("type")?.jsonPrimitive?.content
        ?: "group"
      val isChannel = typeStr.lowercase() == "channel"

      // Extract members
      val summaryObj = entryTypeObj?.get("summary")?.jsonObject
      val parsedMembersCount = summaryObj?.get("currentMembers")?.jsonPrimitive?.content?.toIntOrNull()
        ?: summaryObj?.get("members")?.jsonPrimitive?.content?.toIntOrNull()
      val membersCount = parsedMembersCount?.takeIf { it > 0 }

      val imgFile = entry["imageFile"]?.jsonPrimitive?.content
      val imgUrl = if (!imgFile.isNullOrBlank()) "$DATA_BASE_URL$imgFile" else null

      result.add(
        SimpleUxDirectoryEntry(
          name = name,
          link = link,
          description = description,
          isChannel = isChannel,
          membersCount = membersCount,
          imageUrl = imgUrl
        )
      )
    }
  } catch (e: Exception) {
    Log.e(TAG, "Error parsing directory JSON: ${e.message}")
  }
  return result
}
