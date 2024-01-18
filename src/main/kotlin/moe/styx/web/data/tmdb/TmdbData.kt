package moe.styx.web.data.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat

private val format = SimpleDateFormat("yyyy-MM-dd")

@Serializable
data class ImageResults(
    val backdrops: List<TmdbImage>,
    val logos: List<TmdbImage>,
    val posters: List<TmdbImage>
)

@Serializable
data class TmdbImage(
    @SerialName("aspect_ratio")
    val aspectRatio: Double,
    val height: Int,
    val width: Int,
    @SerialName("iso_639_1")
    val languageCode: String?,
    @SerialName("file_path")
    val filePath: String,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int
) {
    fun getURL() = "https://www.themoviedb.org/t/p/original$filePath"
}

@Serializable
data class TmdbMeta(
    val id: Int,
    @SerialName("name")
    private val _name: String? = null,
    private val title: String? = null,
    val overview: String,
    @SerialName("number_of_seasons")
    val numberSeasons: Int? = null
) {
    val name: String
        get() = if (_name.isNullOrBlank()) title ?: "" else _name
}

@Serializable
data class TmdbGroupQueryResult(val type: Int, val name: String, val id: String, @SerialName("group_count") val groupCount: Int)

@Serializable
data class TmdbGroupQuery(val id: Int, val results: List<TmdbGroupQueryResult>?)

@Serializable
data class TmdbEpisode(
    @SerialName("air_date")
    val airDate: String,
    @SerialName("episode_number")
    val episodeNumber: Int,
    val id: Int,
    val name: String,
    val overview: String,
    @SerialName("still_path")
    val stillPath: String,
    val order: Int
) {
    fun getThumbnail() = "https://www.themoviedb.org/t/p/original$stillPath"
    fun parseDate() = format.parse(airDate)
    fun parseDateUnix() = parseDate().toInstant().epochSecond
}

@Serializable
data class TmdbEpisodeGroup(
    val id: String,
    val name: String,
    val order: Int,
    val episodes: List<TmdbEpisode>
)

@Serializable
data class TmdbSeason(
    val id: Int,
    val name: String,
    val overview: String,
    val episodes: List<TmdbEpisode>
)


@Serializable
data class TmdbEpisodeOrder(val description: String, val groups: List<TmdbEpisodeGroup>)