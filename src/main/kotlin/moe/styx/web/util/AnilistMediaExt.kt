package moe.styx.web.util

import pw.vodes.anilistkmp.graphql.fragment.MediaBig

fun MediaBig.anyTitle(): String {
    val title = title
    if (title?.english.isNullOrBlank())
        return if (title?.romaji.isNullOrBlank()) title?.native ?: "N/A" else title?.romaji ?: "N/A"
    return title?.english ?: "N/A"
}

fun MediaBig.listingURL() = "https://anilist.co/anime/$id"

fun MediaBig.coverImageURL() = coverImage?.extraLarge ?: coverImage?.large

val MediaBig.cleanedDescription: String?
    get() = description?.replace("<br><br>", "\n")?.replace("<br>", "")

fun MediaBig.genresString(): String = genres.orEmpty().filterNotNull().joinToString(", ")

fun MediaBig.tagsString(): String =
    tags.orEmpty()
        .filterNotNull()
        .filter { (it.rank ?: 0) > 60 && it.isMediaSpoiler != true }
        .take(10)
        .joinToString(", ") { it.name }
