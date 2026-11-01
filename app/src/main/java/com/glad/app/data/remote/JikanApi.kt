package com.glad.app.data.remote

import org.json.JSONObject
import java.net.URLEncoder

data class AnimeSummary(
    val malId: Int,
    val title: String,
    val imageUrl: String?,
    val score: Double?,
    val episodes: Int,
    val status: String?,
    val type: String?,
    val year: Int?,
    val synopsis: String?,
    val genres: List<String>,
    val rank: Int?,
    val duration: String?,
    val rating: String?,
    val studios: List<String>
)

data class AnimePage(val items: List<AnimeSummary>, val hasNext: Boolean)

/** Jikan v4 (unofficial MyAnimeList API). Free, no key required. */
class JikanApi {
    private val base = "https://api.jikan.moe/v4"

    suspend fun top(page: Int): AnimePage = list("$base/top/anime?page=$page&limit=24&sfw=true")

    suspend fun airing(page: Int): AnimePage = list("$base/seasons/now?page=$page&limit=24&sfw=true")

    suspend fun search(query: String, page: Int): AnimePage {
        val q = URLEncoder.encode(query, "UTF-8")
        return list("$base/anime?q=$q&page=$page&limit=24&sfw=true&order_by=members&sort=desc")
    }

    suspend fun detail(id: Int): AnimeSummary {
        val root = JSONObject(Http.get("$base/anime/$id/full"))
        return parse(root.getJSONObject("data"))
    }

    private suspend fun list(url: String): AnimePage {
        val root = JSONObject(Http.get(url))
        val arr = root.optJSONArray("data")
        val items = buildList {
            if (arr != null) for (i in 0 until arr.length()) add(parse(arr.getJSONObject(i)))
        }.distinctBy { it.malId }
        val hasNext = root.optJSONObject("pagination")?.optBoolean("has_next_page", false) ?: false
        return AnimePage(items, hasNext)
    }

    private fun parse(o: JSONObject): AnimeSummary {
        val jpg = o.optJSONObject("images")?.optJSONObject("jpg")
        val image = jpg?.optString("large_image_url")?.takeIf { it.isNotBlank() && it != "null" }
            ?: jpg?.optString("image_url")?.takeIf { it.isNotBlank() && it != "null" }
        val english = o.optString("title_english", "").takeIf { it.isNotBlank() && it != "null" }
        val names = { key: String ->
            val arr = o.optJSONArray(key)
            buildList { if (arr != null) for (i in 0 until arr.length()) add(arr.getJSONObject(i).optString("name")) }
        }
        return AnimeSummary(
            malId = o.getInt("mal_id"),
            title = english ?: o.optString("title", "Untitled"),
            imageUrl = image,
            score = if (o.isNull("score")) null else o.optDouble("score"),
            episodes = if (o.isNull("episodes")) 0 else o.optInt("episodes", 0),
            status = o.optString("status").takeIf { it.isNotBlank() && it != "null" },
            type = o.optString("type").takeIf { it.isNotBlank() && it != "null" },
            year = if (o.isNull("year")) null else o.optInt("year"),
            synopsis = o.optString("synopsis").takeIf { it.isNotBlank() && it != "null" },
            genres = names("genres"),
            rank = if (o.isNull("rank")) null else o.optInt("rank"),
            duration = o.optString("duration").takeIf { it.isNotBlank() && it != "null" },
            rating = o.optString("rating").takeIf { it.isNotBlank() && it != "null" },
            studios = names("studios")
        )
    }
}
