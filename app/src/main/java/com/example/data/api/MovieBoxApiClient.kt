package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CastActor
import com.example.data.model.CategorySection
import com.example.data.model.DubLanguage
import com.example.data.model.HeroBanner
import com.example.data.model.HomeFeedData
import com.example.data.model.MovieItem
import com.example.data.model.MovieStream
import com.example.data.model.PlatformItem
import com.example.data.model.SeasonInfo
import com.example.data.model.StreamPlayResult
import com.example.data.model.SubjectDetailResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object MovieBoxApiClient {
    private const val TAG = "MovieBoxApiClient"
    private const val HOME_API_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/home"
    private const val SEARCH_SUGGEST_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/search-suggest"
    private const val RANKING_CONTENT_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/ranking-list/content"
    private const val SEARCH_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/search"
    private const val DETAIL_API_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/detail"
    private const val DETAIL_API_FALLBACK_URL = "https://movieboxph.org/wefeed-h5api-bff/detail"
    private const val PLAY_URL = "https://movieboxph.org/wefeed-h5api-bff/subject/play"
    private const val PLAY_URL_FALLBACK = "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/play"

    private const val AUTH_TOKEN =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjI4NDQzODk4NzkxOTMxNTk2NDAsImF0cCI6MywiZXh0IjoiMTc4ODQwNzY5OSIsImV4cCI6MTc5NjE4MzY5OSwiaWF0IjoxNzg4NDA3Mzk5fQ.G3cuG8zV0y5Eug4TZKPgyULcsQTQqrmLWmJ_x0k_2TE"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36"
    private const val ORIGIN = "https://movieboxph.org"
    private const val REFERER = "https://movieboxph.org/"
    private const val CLIENT_INFO = "{\"timezone\":\"Asia/Calcutta\"}"
    private const val CLIENT_TOKEN = "1788407698,9fc3edc44ea88a1f6b8fcfd149a742da"
    private const val REQUEST_LANG = "en"
    private const val USER_JSON =
        "{\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjI4NDQzODk4NzkxOTMxNTk2NDAsImF0cCI6MywiZXh0IjoiMTc4ODQwNzY5OSIsImV4cCI6MTc5NjE4MzY5OSwiaWF0IjoxNzg4NDA3Mzk5fQ.G3cuG8zV0y5Eug4TZKPgyULcsQTQqrmLWmJ_x0k_2TE\",\"userId\":\"2844389879193159640\",\"userType\":0,\"appType\":3}"
    private const val COOKIE_HEADER =
        "moviebox_web_tk=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjI4NDQzODk4NzkxOTMxNTk2NDAsImF0cCI6MywiZXh0IjoiMTc4ODQwNzY5OSIsImV4cCI6MTc5NjE4MzY5OSwiaWF0IjoxNzg4NDA3Mzk5fQ.G3cuG8zV0y5Eug4TZKPgyULcsQTQqrmLWmJ_x0k_2TE; _ga=GA1.1.240979059.1788407700; token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjQ4MDE4ODc4MTM1MTg2NDYzMTIsImF0cCI6MywiZXh0IjoiMTc4ODQyMTgzOCIsImV4cCI6MTc5NjE5NzgzOCwiaWF0IjoxNzg4NDIxNTM4fQ.ZLcGNUu-2zavdxmJ08bgpKbrVvc86DVVfUrjEUqvMc4; NEXT_LOCALE=en; moviebox_country_code=IE; _ga_CJSG3GJM85=GS2.1.s1788525801\$o6\$g1\$t1788525816\$j45\$l0\$h0"

    private var okHttpClient: OkHttpClient? = null
    private var playOkHttpClient: OkHttpClient? = null

    private fun getPlayClient(): OkHttpClient {
        return playOkHttpClient ?: synchronized(this) {
            playOkHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build().also { playOkHttpClient = it }
        }
    }

    private fun getClient(context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: run {
                val cacheDir = File(context.cacheDir, "moviebox_http_cache")
                val cache = Cache(cacheDir, 20L * 1024 * 1024) // 20 MB cache
                OkHttpClient.Builder()
                    .cache(cache)
                    .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("Accept", "*/*")
                            .header("Content-Type", "application/json")
                            .header("Authorization", AUTH_TOKEN)
                            .header("Origin", ORIGIN)
                            .header("Referer", REFERER)
                            .header("User-Agent", USER_AGENT)
                            .header("x-client-info", CLIENT_INFO)
                            .header("x-client-token", CLIENT_TOKEN)
                            .header("x-request-lang", REQUEST_LANG)
                            .header("x-user", USER_JSON)
                            .header("Cookie", COOKIE_HEADER)
                            .header("sec-ch-ua", "\"Chromium\";v=\"152\", \"Not?A_Brand\";v=\"24\", \"Google Chrome\";v=\"152\"")
                            .header("sec-ch-ua-mobile", "?0")
                            .header("sec-ch-ua-platform", "\"Windows\"")
                            .header("sec-fetch-dest", "empty")
                            .header("sec-fetch-mode", "cors")
                            .header("sec-fetch-site", "cross-site")
                            .header("accept-language", "en-US,en;q=0.9,hi;q=0.8")
                            .build()
                        chain.proceed(request)
                    }
                    .build().also { okHttpClient = it }
            }
        }
    }

    suspend fun fetchHomeFeed(context: Context): HomeFeedData = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            HOME_API_URL,
            "https://movieboxph.org/wefeed-h5api-bff/home"
        )
        for (apiUrl in endpoints) {
            try {
                val client = getClient(context)
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Cache-Control", "no-cache")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonString = response.body?.string()
                    if (!jsonString.isNullOrBlank()) {
                        val parsed = parseHomeFeedJson(jsonString)
                        if (parsed.sections.isNotEmpty() || parsed.heroBanners.isNotEmpty()) {
                            Log.d(TAG, "Successfully loaded live home feed from $apiUrl with ${parsed.sections.size} sections")
                            return@withContext parsed
                        }
                    }
                }
                Log.w(TAG, "Network response unsuccessful from $apiUrl code: ${response.code}")
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching feed from $apiUrl", e)
            }
        }

        // Fallback to bundled cache to guarantee 100% reliability and 0% lag
        loadBundledHomeFeed(context)
    }

    fun loadBundledHomeFeed(context: Context): HomeFeedData {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.default_home)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            parseHomeFeedJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bundled cache", e)
            HomeFeedData()
        }
    }

    fun parseHomeFeedJson(jsonString: String): HomeFeedData {
        val root = JSONObject(jsonString)
        val dataObj = root.optJSONObject("data") ?: return HomeFeedData()

        // 1. Platforms
        val platformArray = dataObj.optJSONArray("platformList") ?: JSONArray()
        val platforms = mutableListOf<PlatformItem>()
        for (i in 0 until platformArray.length()) {
            val pObj = platformArray.optJSONObject(i) ?: continue
            val name = pObj.optString("name")
            if (name.isNotBlank()) {
                platforms.add(
                    PlatformItem(
                        name = name,
                        uploadBy = pObj.optString("uploadBy")
                    )
                )
            }
        }

        val opArray = dataObj.optJSONArray("operatingList") ?: JSONArray()
        val heroBanners = mutableListOf<HeroBanner>()
        val sections = mutableListOf<CategorySection>()

        for (i in 0 until opArray.length()) {
            val opObj = opArray.optJSONObject(i) ?: continue
            val type = opObj.optString("type")
            val title = opObj.optString("title").trim()
            val opId = opObj.optString("opId", i.toString())
            val genreTopId = opObj.optString("genreTopId")
            val detailPath = opObj.optString("detailPath")

            when (type) {
                "BANNER" -> {
                    val bannerObj = opObj.optJSONObject("banner")
                    val itemsArray = bannerObj?.optJSONArray("items") ?: JSONArray()
                    for (bIndex in 0 until itemsArray.length()) {
                        val bItem = itemsArray.optJSONObject(bIndex) ?: continue
                        val bSubject = bItem.optJSONObject("subject")
                        val bImage = bItem.optJSONObject("image")

                        val bannerTitle = bItem.optString("title").ifBlank {
                            bSubject?.optString("title") ?: "MovieBox Feature"
                        }
                        val backdropUrl = bImage?.optString("url") ?: bSubject?.optJSONObject("stills")?.optString("url") ?: ""
                        val posterUrl = bSubject?.optJSONObject("cover")?.optString("url") ?: backdropUrl
                        val rDate = bSubject?.optString("releaseDate") ?: ""
                        val rYear = if (rDate.length >= 4) rDate.substring(0, 4) else "2026"
                        val bGenre = bSubject?.optString("genre") ?: "Action"
                        val bCorner = bSubject?.optString("corner") ?: ""
                        val isSeries = bCorner.contains("EP", ignoreCase = true) ||
                            bGenre.contains("Series", ignoreCase = true) ||
                            bGenre.contains("Anime", ignoreCase = true)

                        val rawSubjectId = bSubject?.optString("subjectId")?.takeIf { it.isNotBlank() && it != "0" }
                            ?: bItem.optString("subjectId").takeIf { it.isNotBlank() && it != "0" }
                            ?: bSubject?.optString("id")?.takeIf { it.isNotBlank() && it != "0" }
                            ?: bItem.optString("id").takeIf { it.isNotBlank() && it != "0" }
                            ?: ""

                        val rawDetailPath = bSubject?.optString("detailPath")?.trim()?.ifBlank { null }
                            ?: bItem.optString("detailPath")?.trim()?.ifBlank { null }
                            ?: ""

                        val heroBanner = HeroBanner(
                            id = rawSubjectId.ifBlank { bItem.optString("id", bIndex.toString()) },
                            title = bannerTitle,
                            description = bSubject?.optString("description") ?: "",
                            backdropUrl = backdropUrl.ifBlank { posterUrl },
                            posterUrl = posterUrl.ifBlank { backdropUrl },
                            subjectId = rawSubjectId,
                            rating = bSubject?.optString("imdbRatingValue") ?: "8.2",
                            genre = bGenre,
                            releaseDate = rDate,
                            releaseYear = rYear,
                            country = bSubject?.optString("countryName") ?: "",
                            corner = bCorner,
                            detailPath = rawDetailPath,
                            isSeries = isSeries
                        )
                        heroBanners.add(heroBanner)
                    }
                }
                "SUBJECTS_MOVIE", "APPOINTMENT_LIST" -> {
                    val subjectsArray = opObj.optJSONArray("subjects") ?: JSONArray()
                    val movies = parseSubjectArray(subjectsArray, isShortCategory = title.contains("Short", ignoreCase = true))
                    if (movies.isNotEmpty()) {
                        sections.add(
                            CategorySection(
                                id = opId,
                                title = cleanCategoryTitle(title),
                                type = type,
                                items = movies,
                                genreTopId = genreTopId,
                                detailPath = detailPath,
                                opId = opId
                            )
                        )
                    }
                }
                "CUSTOM" -> {
                    val customData = opObj.optJSONObject("customData")
                    val customItemsArray = customData?.optJSONArray("items") ?: JSONArray()
                    val movies = mutableListOf<MovieItem>()
                    for (cIndex in 0 until customItemsArray.length()) {
                        val cItem = customItemsArray.optJSONObject(cIndex) ?: continue
                        val cSubject = cItem.optJSONObject("subject")
                        val itemMovie = if (cSubject != null) {
                            parseMovieItem(cSubject, isShort = title.contains("Short", ignoreCase = true))
                        } else {
                            val cTitle = cItem.optString("title")
                            val cImgObj = cItem.optJSONObject("image")
                            val cImg = cImgObj?.optString("url") ?: ""
                            val cWidth = cImgObj?.optInt("width", 0) ?: 0
                            val cHeight = cImgObj?.optInt("height", 0) ?: 0
                            MovieItem(
                                id = cItem.optString("id", cIndex.toString()),
                                title = cTitle,
                                coverUrl = cImg,
                                backdropUrl = cImg,
                                isShort = title.contains("Short", ignoreCase = true),
                                coverWidth = cWidth,
                                coverHeight = cHeight
                            )
                        }
                        if (itemMovie.title.isNotBlank() && itemMovie.coverUrl.isNotBlank()) {
                            movies.add(itemMovie)
                        }
                    }
                    if (movies.isNotEmpty()) {
                        sections.add(
                            CategorySection(
                                id = opId,
                                title = cleanCategoryTitle(title),
                                type = type,
                                items = movies,
                                genreTopId = genreTopId,
                                detailPath = detailPath,
                                opId = opId
                            )
                        )
                    }
                }
            }
        }

        return HomeFeedData(
            heroBanners = heroBanners,
            platforms = platforms,
            sections = sections
        )
    }

    private fun parseSubjectArray(array: JSONArray, isShortCategory: Boolean): List<MovieItem> {
        val list = mutableListOf<MovieItem>()
        for (i in 0 until array.length()) {
            val sObj = array.optJSONObject(i) ?: continue
            val movie = parseMovieItem(sObj, isShort = isShortCategory)
            if (movie.title.isNotBlank() && movie.coverUrl.isNotBlank()) {
                list.add(movie)
            }
        }
        return list
    }

    private fun parseMovieItem(sObj: JSONObject, isShort: Boolean): MovieItem {
        val subjectId = sObj.optString("subjectId")
        val title = sObj.optString("title")
        val description = sObj.optString("description")
        val releaseDate = sObj.optString("releaseDate")
        val releaseYear = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2025"
        val rawRating = sObj.optString("imdbRatingValue", "7.5")
        val rating = if (rawRating.isBlank() || rawRating == "0") "7.8" else rawRating
        val ratingCount = sObj.optInt("imdbRatingCount", 0)

        val coverObj = sObj.optJSONObject("cover")
        val stillsObj = sObj.optJSONObject("stills")
        val imageObj = sObj.optJSONObject("image")
        val coverUrl = coverObj?.optString("url") ?: imageObj?.optString("url") ?: ""
        val stillsUrl = stillsObj?.optString("url") ?: coverUrl
        val coverWidth = coverObj?.optInt("width", 0) ?: imageObj?.optInt("width", 0) ?: 0
        val coverHeight = coverObj?.optInt("height", 0) ?: imageObj?.optInt("height", 0) ?: 0
        val genre = sObj.optString("genre", "Drama, Action")
        val country = sObj.optString("countryName", "")
        val corner = sObj.optString("corner")
        val durationSec = sObj.optLong("duration", 0L)
        val durationFormatted = formatDuration(durationSec)
        val source = sObj.optString("source")
        val uploadBy = sObj.optString("uploadBy")
        val isShortMovie = isShort ||
            (source.equals("ugc-anime.com", ignoreCase = true) && uploadBy.equals("MiniTV", ignoreCase = true)) ||
            (sObj.toString().contains("ugc-anime.com", ignoreCase = true) && sObj.toString().contains("MiniTV", ignoreCase = true)) ||
            title.contains("Short", ignoreCase = true) || genre.contains("Short", ignoreCase = true)

        val dubsArray = sObj.optJSONArray("dubs")
        val parsedDubs = mutableListOf<com.example.data.model.DubLanguage>()
        if (dubsArray != null) {
            for (d in 0 until dubsArray.length()) {
                val dObj = dubsArray.optJSONObject(d) ?: continue
                val orig = dObj.optBoolean("original", false)
                val dSubjId = dObj.optString("subjectId").ifBlank { dObj.optString("id") }.ifBlank { if (orig) subjectId else "" }
                val lanName = dObj.optString("lanName")
                val lanCode = dObj.optString("lanCode")
                val dDetailPath = dObj.optString("detailPath")
                if (lanName.isNotBlank() || dSubjId.isNotBlank()) {
                    parsedDubs.add(
                        com.example.data.model.DubLanguage(
                            subjectId = dSubjId,
                            lanName = lanName,
                            lanCode = lanCode,
                            original = orig,
                            detailPath = dDetailPath
                        )
                    )
                }
            }
        }

        val totalEpCount = Regex("""(\d+)\s*(?:EP|ep|Episodes|Ep)""").find(corner)?.groupValues?.get(1)?.toIntOrNull()
            ?: sObj.optInt("totalEpisodes", 0).takeIf { it > 0 }
            ?: sObj.optInt("maxEp", 0).takeIf { it > 0 }
            ?: 0

        return MovieItem(
            id = subjectId,
            title = title,
            description = description,
            coverUrl = coverUrl,
            backdropUrl = stillsUrl,
            rating = rating,
            ratingCount = ratingCount,
            releaseDate = releaseDate,
            releaseYear = releaseYear,
            genre = genre,
            country = country,
            corner = corner,
            duration = durationFormatted,
            isShort = isShortMovie,
            detailPath = sObj.optString("detailPath"),
            source = source,
            uploadBy = uploadBy,
            coverWidth = coverWidth,
            coverHeight = coverHeight,
            dubs = parsedDubs,
            totalEpisodes = totalEpCount
        )
    }

    private fun formatDuration(durationMillisOrSeconds: Long): String {
        if (durationMillisOrSeconds <= 0) return ""
        val seconds = if (durationMillisOrSeconds > 100000) durationMillisOrSeconds / 1000 else durationMillisOrSeconds
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    private fun cleanCategoryTitle(title: String): String {
        return title
            .replace("Banner_Africa", "Featured Releases")
            .trim()
    }

    suspend fun fetchSearchSuggestions(context: Context, keyword: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            val client = getClient(context)
            val jsonBody = JSONObject().apply {
                put("keyword", trimmed)
            }.toString()

            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(SEARCH_SUGGEST_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val root = JSONObject(jsonStr)
                    val dataObj = root.optJSONObject("data")
                    val itemsArray = dataObj?.optJSONArray("items")
                    if (itemsArray != null) {
                        val list = mutableListOf<String>()
                        for (i in 0 until itemsArray.length()) {
                            val itm = itemsArray.optJSONObject(i) ?: continue
                            val word = itm.optString("word").trim()
                            if (word.isNotBlank() && !list.contains(word)) {
                                list.add(word)
                            }
                        }
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching suggestions for: $keyword", e)
        }
        emptyList()
    }

    suspend fun fetchRankingListContent(
        context: Context,
        genreTopId: String,
        page: Int = 1,
        perPage: Int = 20
    ): Pair<List<MovieItem>, Boolean> = withContext(Dispatchers.IO) {
        if (genreTopId.isBlank()) return@withContext Pair(emptyList(), false)
        try {
            val client = getClient(context)
            val url = "$RANKING_CONTENT_URL?id=$genreTopId&page=$page&perPage=$perPage"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val root = JSONObject(jsonStr)
                    val dataObj = root.optJSONObject("data") ?: return@withContext Pair(emptyList(), false)
                    val subjectList = dataObj.optJSONArray("subjectList") ?: JSONArray()
                    val movies = parseSubjectArray(subjectList, isShortCategory = false)
                    val pager = dataObj.optJSONObject("pager")
                    val hasMore = pager?.optBoolean("hasMore", false) ?: (movies.size >= perPage)
                    return@withContext Pair(movies, hasMore)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ranking list for: $genreTopId", e)
        }
        Pair(emptyList(), false)
    }

    suspend fun searchMoviesApi(
        context: Context,
        keyword: String,
        page: Int = 1,
        perPage: Int = 24
    ): List<MovieItem> = withContext(Dispatchers.IO) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        try {
            val client = getClient(context)
            val jsonBody = JSONObject().apply {
                put("keyword", trimmed)
                put("page", page)
                put("perPage", perPage)
            }.toString()

            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(SEARCH_URL).post(body).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val root = JSONObject(jsonStr)
                    val dataObj = root.optJSONObject("data")
                    val itemsArray = dataObj?.optJSONArray("items") ?: JSONArray()
                    val list = mutableListOf<MovieItem>()
                    for (i in 0 until itemsArray.length()) {
                        val sObj = itemsArray.optJSONObject(i) ?: continue
                        val movie = parseMovieItem(sObj, isShort = false)
                        if (movie.title.isNotBlank()) {
                            list.add(movie)
                        }
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching movies for: $keyword", e)
        }
        emptyList()
    }

    suspend fun fetchSubjectDetail(
        context: Context,
        detailPath: String,
        fallbackSubjectId: String = ""
    ): SubjectDetailResult? = withContext(Dispatchers.IO) {
        val path = detailPath.trim()
        if (path.isBlank() && fallbackSubjectId.isBlank()) return@withContext null

        val urls = mutableListOf<String>()
        if (path.isNotBlank()) {
            urls.add("$DETAIL_API_URL?detailPath=$path")
            urls.add("$DETAIL_API_FALLBACK_URL?detailPath=$path")
        }
        if (fallbackSubjectId.isNotBlank()) {
            urls.add("$DETAIL_API_URL?subjectId=$fallbackSubjectId")
            urls.add("$DETAIL_API_FALLBACK_URL?subjectId=$fallbackSubjectId")
        }

        val client = getClient(context)
        val refererUrl = if (path.isNotBlank()) "https://movieboxph.org/play/$path" else "https://movieboxph.org/"

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", AUTH_TOKEN)
                    .header("Origin", ORIGIN)
                    .header("Referer", refererUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("x-client-info", CLIENT_INFO)
                    .header("x-request-lang", REQUEST_LANG)
                    .header("Accept", "*/*")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) continue
                val jsonStr = response.body?.string() ?: continue
                if (jsonStr.isBlank()) continue

                val prettyDetailJson = try {
                    JSONObject(jsonStr).toString(2)
                } catch (e: Exception) {
                    jsonStr
                }

                val root = JSONObject(jsonStr)
                if (root.optInt("code", -1) != 0) continue
                val dataObj = root.optJSONObject("data") ?: continue
                val subj = dataObj.optJSONObject("subject") ?: continue

                val subjectId = subj.optString("subjectId").ifBlank { fallbackSubjectId }
                val subjectType = subj.optInt("subjectType", 1) // 1=Movie, 2=Series
                val title = subj.optString("title")
                val description = subj.optString("description")
                val releaseDate = subj.optString("releaseDate")
                val releaseYear = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                val durationSec = subj.optLong("duration", 0L)
                val durationFormatted = formatDuration(durationSec)
                val genre = subj.optString("genre")
                val country = subj.optString("countryName")
                val rating = subj.optString("imdbRatingValue").ifBlank { "7.8" }
                val ratingCount = subj.optInt("imdbRatingCount", 0)
                val subtitles = subj.optString("subtitles")
                val corner = subj.optString("corner")
                val hasResource = subj.optBoolean("hasResource", true)
                val source = subj.optString("source")
                    .ifBlank { dataObj.optString("source") }
                    .ifBlank { dataObj.optJSONObject("resource")?.optString("source") ?: "" }
                val uploadBy = subj.optString("uploadBy")
                    .ifBlank { dataObj.optString("uploadBy") }
                    .ifBlank { dataObj.optJSONObject("resource")?.optString("uploadBy") ?: "" }

                val coverUrl = subj.optJSONObject("cover")?.optString("url") ?: ""
                val stillsUrl = subj.optJSONObject("stills")?.optString("url") ?: coverUrl
                val trailerUrl = subj.optJSONObject("trailer")?.optJSONObject("videoAddress")?.optString("url") ?: ""

                // Parse real seasons from resource.seasons
                val resourceObj = dataObj.optJSONObject("resource")
                val seasonsArray = resourceObj?.optJSONArray("seasons") ?: JSONArray()
                val seasonInfoList = mutableListOf<SeasonInfo>()
                for (sIdx in 0 until seasonsArray.length()) {
                    val sObj = seasonsArray.optJSONObject(sIdx) ?: continue
                    val se = sObj.optInt("se", 0)
                    val maxEp = sObj.optInt("maxEp", 0)
                    val resArr = sObj.optJSONArray("resolutions") ?: JSONArray()
                    val resList = mutableListOf<Int>()
                    for (rIdx in 0 until resArr.length()) {
                        val rObj = resArr.optJSONObject(rIdx) ?: continue
                        val resVal = rObj.optInt("resolution", 0)
                        if (resVal > 0) resList.add(resVal)
                    }
                    if (se > 0 && maxEp > 0) {
                        seasonInfoList.add(
                            SeasonInfo(
                                seasonNumber = se,
                                maxEp = maxEp,
                                resolutions = resList
                            )
                        )
                    }
                }

                // If seasons array wasn't explicitly populated (e.g. for short TV dramas), extract maxEp from resource/data/subj/corner
                if (seasonInfoList.isEmpty()) {
                    val directMaxEp = resourceObj?.optInt("maxEp", 0)?.takeIf { it > 0 }
                        ?: dataObj.optInt("maxEp", 0).takeIf { it > 0 }
                        ?: subj.optInt("maxEp", 0).takeIf { it > 0 }
                        ?: subj.optInt("episodeCount", 0).takeIf { it > 0 }
                        ?: subj.optInt("totalEpisodes", 0).takeIf { it > 0 }
                        ?: resourceObj?.optInt("episodeCount", 0)?.takeIf { it > 0 }
                        ?: Regex("""(\d+)\s*(?:EP|ep|Episodes|Ep)""").find(corner)?.groupValues?.get(1)?.toIntOrNull()
                        ?: 0

                    if (directMaxEp > 0) {
                        seasonInfoList.add(
                            SeasonInfo(
                                seasonNumber = 1,
                                maxEp = directMaxEp,
                                resolutions = emptyList()
                            )
                        )
                    }
                }

                // Parse actors from stars
                val starsArray = dataObj.optJSONArray("stars") ?: JSONArray()
                val castList = mutableListOf<CastActor>()
                for (cIdx in 0 until starsArray.length()) {
                    val starObj = starsArray.optJSONObject(cIdx) ?: continue
                    val name = starObj.optString("name")
                    if (name.isNotBlank()) {
                        castList.add(
                            CastActor(
                                staffId = starObj.optString("staffId"),
                                name = name,
                                character = starObj.optString("character"),
                                avatarUrl = starObj.optString("avatarUrl")
                            )
                        )
                    }
                }

                // Parse dub languages
                val dubsArray = subj.optJSONArray("dubs") ?: dataObj.optJSONArray("dubs") ?: JSONArray()
                val dubList = mutableListOf<DubLanguage>()
                for (dIdx in 0 until dubsArray.length()) {
                    val dObj = dubsArray.optJSONObject(dIdx) ?: continue
                    val original = dObj.optBoolean("original", false)
                    val dubSubjId = dObj.optString("subjectId")
                        .ifBlank { dObj.optString("id") }
                        .ifBlank { dObj.optString("targetId") }
                        .ifBlank { if (original) subjectId else "" }
                    val lanName = dObj.optString("lanName")
                    val lanCode = dObj.optString("lanCode")
                    val type = dObj.optInt("type", 0)
                    val dubDetailPath = dObj.optString("detailPath")
                    if (lanName.isNotBlank() || dubSubjId.isNotBlank()) {
                        dubList.add(
                            DubLanguage(
                                subjectId = dubSubjId,
                                lanName = lanName,
                                lanCode = lanCode,
                                original = original,
                                type = type,
                                detailPath = dubDetailPath
                            )
                        )
                    }
                }

                val isSeries = (subjectType == 2) || seasonInfoList.isNotEmpty() ||
                    genre.contains("Series", ignoreCase = true) ||
                    genre.contains("Anime", ignoreCase = true)

                val parsedDetailPath = subj.optString("detailPath")
                    .ifBlank { dataObj.optString("detailPath") }
                    .ifBlank { path }

                return@withContext SubjectDetailResult(
                        subjectId = subjectId,
                        subjectType = subjectType,
                        title = title,
                        description = description,
                        coverUrl = coverUrl,
                        backdropUrl = stillsUrl,
                        releaseDate = releaseDate,
                        releaseYear = releaseYear,
                        genre = genre,
                        country = country,
                        rating = rating,
                        ratingCount = ratingCount,
                        durationFormatted = durationFormatted,
                        corner = corner,
                        subtitles = subtitles,
                        isSeries = isSeries,
                        seasons = seasonInfoList.sortedBy { it.seasonNumber },
                        cast = castList,
                        detailPath = parsedDetailPath,
                        hasResource = hasResource,
                        trailerUrl = trailerUrl,
                        source = source,
                        uploadBy = uploadBy,
                        dubs = dubList,
                        rawJson = prettyDetailJson
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching subject detail from $url", e)
                }
            }
            null
        }

    suspend fun fetchPlayStreams(
        context: Context,
        subjectId: String,
        detailPath: String = "",
        isShort: Boolean = false,
        season: Int? = null,
        episode: Int? = null
    ): StreamPlayResult = withContext(Dispatchers.IO) {
        if (subjectId.isBlank() && detailPath.isBlank()) return@withContext StreamPlayResult()

        // 1. Normalize subjectId and detailPath
        var effectiveDetailPath = detailPath.trim()
        var realSubjectId = subjectId.trim()
        if (realSubjectId == "0") realSubjectId = ""

        if (effectiveDetailPath.isBlank() && realSubjectId.isNotBlank()) {
            try {
                val detail = fetchSubjectDetail(context, "", realSubjectId)
                if (detail != null && detail.detailPath.isNotBlank()) {
                    effectiveDetailPath = detail.detailPath.trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve detailPath for subjectId: $realSubjectId", e)
            }
        } else if (realSubjectId.isBlank() && effectiveDetailPath.isNotBlank()) {
            try {
                val detail = fetchSubjectDetail(context, effectiveDetailPath, "")
                if (detail != null && detail.subjectId.isNotBlank() && detail.subjectId != "0") {
                    realSubjectId = detail.subjectId.trim()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve subjectId for detailPath: $effectiveDetailPath", e)
            }
        }

        // 2. Determine primary season and episode
        val reqSeason = season ?: if (isShort) 1 else 0
        val reqEpisode = episode ?: if (isShort) 1 else 0

        val firstResult = executePlayRequest(context, realSubjectId, effectiveDetailPath, reqSeason, reqEpisode)
        if (firstResult.hasResource && firstResult.streams.isNotEmpty()) {
            return@withContext firstResult
        }

        // 3. Fallback strategy:
        // If first attempt used se != 0 or ep != 0 (e.g. series or mistakenly flagged as series),
        // try movie format: se=0&ep=0
        if (reqSeason != 0 || reqEpisode != 0) {
            val movieResult = executePlayRequest(context, realSubjectId, effectiveDetailPath, 0, 0)
            if (movieResult.hasResource && movieResult.streams.isNotEmpty()) {
                return@withContext movieResult
            }
        } else {
            // If first attempt used se=0&ep=0, but had no stream resources, try se=1&ep=1
            val seriesResult = executePlayRequest(context, realSubjectId, effectiveDetailPath, 1, 1)
            if (seriesResult.hasResource && seriesResult.streams.isNotEmpty()) {
                return@withContext seriesResult
            }
        }

        // Return firstResult so full response metadata is preserved for debugging
        return@withContext firstResult
    }

    private fun executePlayRequest(
        context: Context,
        subjectId: String,
        detailPath: String,
        season: Int,
        episode: Int
    ): StreamPlayResult {
        val baseUrls = listOf(PLAY_URL, PLAY_URL_FALLBACK)
        var lastResult = StreamPlayResult()
        for (baseUrl in baseUrls) {
            val trimmedPath = detailPath.trim()
            val validSubjectId = subjectId.trim().takeIf { it.isNotBlank() && it != "0" }

            var requestUrl = if (validSubjectId != null) {
                "$baseUrl?subjectId=$validSubjectId&se=$season&ep=$episode"
            } else {
                "$baseUrl?detailPath=$trimmedPath&se=$season&ep=$episode"
            }
            if (trimmedPath.isNotBlank() && validSubjectId != null) {
                requestUrl += "&detailPath=$trimmedPath"
            }

            // Attempt 1: Clean guest request (highest reliability for direct streams without token expiry limits)
            // Attempt 2: Request with user authorization and cookies
            val authOptions = listOf(false, true)
            for (useAuth in authOptions) {
                try {
                    val client = getPlayClient()
                    val refererUrl = if (trimmedPath.isNotBlank()) "https://movieboxph.org/play/$trimmedPath" else "https://movieboxph.org/"

                    val reqBuilder = Request.Builder()
                        .url(requestUrl)
                        .header("accept", "*/*")
                        .header("accept-language", "en-US,en;q=0.9,hi;q=0.8")
                        .header("content-type", "application/json")
                        .header("priority", "u=1, i")
                        .header("referer", refererUrl)
                        .header("sec-ch-ua", "\"Chromium\";v=\"152\", \"Not?A_Brand\";v=\"24\", \"Google Chrome\";v=\"152\"")
                        .header("sec-ch-ua-mobile", "?0")
                        .header("sec-ch-ua-platform", "\"Windows\"")
                        .header("sec-fetch-dest", "empty")
                        .header("sec-fetch-mode", "cors")
                        .header("sec-fetch-site", "same-origin")
                        .header("user-agent", USER_AGENT)
                        .header("x-client-info", CLIENT_INFO)
                        .header("x-request-lang", REQUEST_LANG)
                        .header("x-source", "")

                    if (useAuth) {
                        reqBuilder.header("authorization", AUTH_TOKEN)
                        reqBuilder.header("cookie", COOKIE_HEADER)
                    }

                    val request = reqBuilder.get().build()
                    val response = client.newCall(request).execute()
                    val jsonStr = response.body?.string() ?: ""
                    val prettyJson = try {
                        if (jsonStr.isNotBlank()) JSONObject(jsonStr).toString(2) else ""
                    } catch (e: Exception) {
                        jsonStr
                    }

                    if (!response.isSuccessful) {
                        lastResult = StreamPlayResult(
                            hasResource = false,
                            rawJson = prettyJson,
                            requestUrl = requestUrl,
                            httpCode = response.code,
                            errorMessage = "HTTP ${response.code}: ${response.message}"
                        )
                        continue
                    }

                    if (jsonStr.isNotBlank()) {
                        val root = JSONObject(jsonStr)
                        val dataObj = root.optJSONObject("data")
                        if (dataObj != null) {
                            val hasResource = dataObj.optBoolean("hasResource", false)
                            val streamList = mutableListOf<MovieStream>()

                            // 1. Direct MP4 Streams (e.g. 2160, 1080, 720, 480)
                            val streamsArray = dataObj.optJSONArray("streams") ?: JSONArray()
                            for (i in 0 until streamsArray.length()) {
                                val sObj = streamsArray.optJSONObject(i) ?: continue
                                val sUrl = sObj.optString("url")
                                if (sUrl.isNotBlank()) {
                                    val resRaw = sObj.optString("resolution")
                                        .ifBlank { sObj.optString("resolutions") }
                                        .ifBlank { sObj.optInt("resolution", 0).takeIf { it > 0 }?.toString() ?: "720" }
                                    val cleanRes = resRaw.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "720" } ?: "720"
                                    streamList.add(
                                        MovieStream(
                                            id = sObj.optString("id", "stream_$i"),
                                            resolution = cleanRes,
                                            format = sObj.optString("format", "MP4"),
                                            url = sUrl,
                                            size = sObj.optLong("size", 0L),
                                            duration = sObj.optLong("duration", 0L),
                                            codecName = sObj.optString("codecName", "h264")
                                        )
                                    )
                                }
                            }

                            // 2. DASH Streams (.mpd)
                            val dashArray = dataObj.optJSONArray("dash") ?: JSONArray()
                            for (i in 0 until dashArray.length()) {
                                val dObj = dashArray.optJSONObject(i) ?: continue
                                val dUrl = dObj.optString("url")
                                if (dUrl.isNotBlank()) {
                                    val resRaw = dObj.optString("resolution")
                                        .ifBlank { dObj.optString("resolutions") }
                                        .ifBlank { dObj.optInt("resolution", 0).takeIf { it > 0 }?.toString() ?: "1080" }
                                    val cleanRes = resRaw.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "1080" } ?: "1080"
                                    streamList.add(
                                        MovieStream(
                                            id = dObj.optString("id", "dash_$i"),
                                            resolution = cleanRes,
                                            format = dObj.optString("format", "DASH"),
                                            url = dUrl,
                                            size = dObj.optLong("size", 0L),
                                            duration = dObj.optLong("duration", 0L),
                                            codecName = dObj.optString("codecName", "hevc")
                                        )
                                    )
                                }
                            }

                            // 3. HLS Streams (.m3u8)
                            val hlsArray = dataObj.optJSONArray("hls") ?: JSONArray()
                            for (i in 0 until hlsArray.length()) {
                                val hObj = hlsArray.optJSONObject(i) ?: continue
                                val hUrl = hObj.optString("url")
                                if (hUrl.isNotBlank()) {
                                    val resRaw = hObj.optString("resolution")
                                        .ifBlank { hObj.optString("resolutions") }
                                        .ifBlank { hObj.optInt("resolution", 0).takeIf { it > 0 }?.toString() ?: "1080" }
                                    val cleanRes = resRaw.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "1080" } ?: "1080"
                                    streamList.add(
                                        MovieStream(
                                            id = hObj.optString("id", "hls_$i"),
                                            resolution = cleanRes,
                                            format = hObj.optString("format", "HLS"),
                                            url = hUrl,
                                            size = hObj.optLong("size", 0L),
                                            duration = hObj.optLong("duration", 0L),
                                            codecName = hObj.optString("codecName", "h264")
                                        )
                                    )
                                }
                            }

                            val sorted = if (streamList.isNotEmpty()) {
                                streamList.sortedWith(
                                    compareByDescending<MovieStream> {
                                        it.resolution.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                                    }.thenBy {
                                        if (it.format.equals("MP4", ignoreCase = true)) 0 else 1
                                    }
                                )
                            } else emptyList()

                            // Default choose highest quality stream!
                            val defaultStream = sorted.firstOrNull { it.url.isNotBlank() }

                            val res = StreamPlayResult(
                                streams = sorted,
                                hasResource = hasResource || sorted.isNotEmpty(),
                                defaultStream = defaultStream,
                                rawJson = prettyJson,
                                requestUrl = requestUrl,
                                httpCode = response.code
                            )
                            if (res.streams.isNotEmpty()) {
                                return res
                            } else {
                                lastResult = res
                            }
                        } else {
                            lastResult = StreamPlayResult(
                                hasResource = false,
                                rawJson = prettyJson,
                                requestUrl = requestUrl,
                                httpCode = response.code,
                                errorMessage = root.optString("message", "No 'data' object in response")
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching play streams from $baseUrl for subjectId: $subjectId", e)
                    lastResult = StreamPlayResult(
                        hasResource = false,
                        rawJson = "",
                        requestUrl = requestUrl,
                        httpCode = 0,
                        errorMessage = "${e::class.java.simpleName}: ${e.message}"
                    )
                }
            }
        }
        return lastResult
    }
}
