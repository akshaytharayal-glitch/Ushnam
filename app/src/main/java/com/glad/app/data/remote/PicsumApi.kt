package com.glad.app.data.remote

import org.json.JSONArray

data class OnlineWallpaper(val id: String, val author: String)

/** Lorem Picsum: free photo API (Unsplash photographers), no key required. */
class PicsumApi {
    suspend fun list(page: Int, limit: Int = 30): List<OnlineWallpaper> {
        val arr = JSONArray(Http.get("https://picsum.photos/v2/list?page=$page&limit=$limit"))
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(OnlineWallpaper(o.getString("id"), o.optString("author", "Unknown")))
            }
        }
    }

    companion object {
        fun thumbUrl(id: String) = "https://picsum.photos/id/$id/360/640"
        fun previewUrl(id: String) = "https://picsum.photos/id/$id/900/1600"
        fun fullUrl(id: String, w: Int, h: Int) = "https://picsum.photos/id/$id/$w/$h"
    }
}
