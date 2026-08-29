package chat.simplex.common.views.chatlist

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
 *  - entries without a usable name or without any usable link are skipped;
 *  - link prefers connShortLink, falling back to connFullLink;
 *  - description comes from welcomeMessage parts, falling back to shortDescr parts, with the first
 *    non-blank line that is not a "Link to join" line kept (whole trimmed text when all lines filter out);
 *  - an explicit JSON null in any field reads as absent, never as the literal string "null" (#74);
 *  - a structurally corrupt entry is skipped and parsing continues with the rest of the
 *    listing (#74) — one broken entry no longer truncates everything after it.
 */
internal fun parseDirectoryEntries(rawJson: String): List<SimpleUxDirectoryEntry> {
  val root = try {
    directoryJsonParser.parseToJsonElement(rawJson).jsonObject
  } catch (e: Exception) {
    Log.e(TAG, "Error parsing directory JSON: ${e.message}")
    return emptyList()
  }
  // `as?` instead of `.jsonArray`: a non-array "entries" member is an empty listing, not an error.
  val entries = root["entries"] as? JsonArray ?: return emptyList()

  val result = mutableListOf<SimpleUxDirectoryEntry>()
  for (element in entries) {
    // Per-entry isolation (#74): a structurally corrupt entry is logged and skipped instead of
    // aborting the loop and dropping every entry after it.
    try {
      parseDirectoryEntry(element.jsonObject)?.let { result.add(it) }
    } catch (e: Exception) {
      Log.w(TAG, "Skipping corrupt directory entry: ${e.message}")
    }
  }
  return result
}

/**
 * Parses one listing entry; null return means "skip this entry". Throws on structurally broken
 * members (container-shaped fields of the wrong type) — the caller isolates failures per entry (#74).
 *
 * Fallback decisions (#74):
 *  - name is required: an absent, JSON-null or blank displayName means the entry cannot be listed
 *    usefully and is skipped (it is never rendered named "null");
 *  - link is required: connShortLink is preferred, connFullLink is the fallback (a JSON-null short
 *    link counts as absent so the fallback applies instead of producing a broken ".../null" link);
 *    when neither yields a usable link the entry is skipped.
 */
private fun parseDirectoryEntry(entry: JsonObject): SimpleUxDirectoryEntry? {
  val name = entry.stringField("displayName")?.takeIf { it.isNotBlank() } ?: return null

  // Extract link
  val groupLinkObj = entry.objectField("groupLink")
  val link = groupLinkObj?.stringField("connShortLink")
    ?: groupLinkObj?.stringField("connFullLink")
    ?: return null

  // Extract description
  var desc = ""
  val welcomeArray = entry.arrayField("welcomeMessage")
  if (welcomeArray != null && welcomeArray.isNotEmpty()) {
    for (part in welcomeArray) {
      val textPart = part.jsonObject.stringField("text")
      if (!textPart.isNullOrBlank()) {
        desc += textPart
      }
    }
  }
  if (desc.isBlank()) {
    val shortDescArray = entry.arrayField("shortDescr")
    if (shortDescArray != null) {
      for (part in shortDescArray) {
        val textPart = part.jsonObject.stringField("text")
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
  val entryTypeObj = entry.objectField("entryType")
  val typeStr = entryTypeObj?.stringField("groupType")
    ?: entryTypeObj?.stringField("type")
    ?: "group"
  val isChannel = typeStr.lowercase() == "channel"

  // Extract members
  val summaryObj = entryTypeObj?.objectField("summary")
  val parsedMembersCount = summaryObj?.stringField("currentMembers")?.toIntOrNull()
    ?: summaryObj?.stringField("members")?.toIntOrNull()
  val membersCount = parsedMembersCount?.takeIf { it > 0 }

  val imgFile = entry.stringField("imageFile")
  val imgUrl = if (!imgFile.isNullOrBlank()) "$DATA_BASE_URL$imgFile" else null

  return SimpleUxDirectoryEntry(
    name = name,
    link = link,
    description = description,
    isChannel = isChannel,
    membersCount = membersCount,
    imageUrl = imgUrl
  )
}

/**
 * Content of a JSON primitive member, or null when the member is absent or an explicit JSON null.
 * Without the null guard, `jsonPrimitive.content` on [JsonNull] yields the literal string "null"
 * (#74): entries named "null", broken ".../null" join links and ".../data/null" image URLs.
 * A non-primitive value (object/array) reads as absent instead of corrupting the entry.
 */
private fun JsonObject.stringField(key: String): String? =
  (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

/**
 * Member as a JSON object, or null when the member is absent or an explicit JSON null (#74).
 * Any other shape throws — the caller's per-entry isolation skips the corrupt entry.
 */
private fun JsonObject.objectField(key: String): JsonObject? {
  val v = this[key] ?: return null
  if (v is JsonNull) return null
  return v.jsonObject
}

/**
 * Member as a JSON array, or null when the member is absent or an explicit JSON null (#74).
 * Any other shape throws — the caller's per-entry isolation skips the corrupt entry.
 */
private fun JsonObject.arrayField(key: String): JsonArray? {
  val v = this[key] ?: return null
  if (v is JsonNull) return null
  return v.jsonArray
}
