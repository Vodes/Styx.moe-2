package moe.styx.web.util

import moe.styx.common.data.*
import moe.styx.common.data.tmdb.StackType
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.json
import moe.styx.db.tables.MediaTable
import moe.styx.web.data.getMalIDForAnilistID
import moe.styx.web.data.tmdb.tmdbFindMedia
import moe.styx.web.dbClient
import moe.styx.web.newGUID
import org.jetbrains.exposed.v1.jdbc.selectAll
import pw.vodes.anilistkmp.graphql.fragment.MediaBig
import pw.vodes.anilistkmp.graphql.type.MediaRelation

fun createMediaFromAnilist(data: MediaBig, image: Image? = null, category: Category? = null, existingMedia: Media? = null): List<Media> {
    val malID = getMalIDForAnilistID(data.id)
    val tmdbResult = tmdbFindMedia(data.anyTitleNoSeason())

    val mapping = existingMedia?.let {
        runCatching { json.decodeFromString<MappingCollection>(existingMedia.metadataMap!!) }.getOrNull()
    } ?: MappingCollection()

    mapping.apply {
        if (anilistMappings.find { it.remoteID == data.id } == null)
            anilistMappings.add(BasicMapping(remoteID = data.id))
        if (malID != null && malMappings.find { it.remoteID == malID } == null)
            malMappings.add(BasicMapping(remoteID = malID))
        if (tmdbResult.isNotEmpty() && tmdbMappings.find { it.remoteID == tmdbResult.first().id } == null)
            tmdbMappings.add(TMDBMapping(remoteID = tmdbResult.first().id))
    }

    val media = existingMedia?.let { m ->
        m.copy(
            name = data.anyTitle(),
            nameEN = data.title?.english ?: "",
            nameJP = data.title?.romaji ?: "",
            synopsisEN = data.cleanedDescription ?: "",
            genres = data.genresString(),
            tags = data.tagsString(),
            metadataMap = json.encodeToString(mapping),
            thumbID = image?.GUID ?: m.thumbID,
            categoryID = category?.GUID ?: m.categoryID
        )
    } ?: Media(
        newGUID(),
        name = data.anyTitle(),
        nameEN = data.title?.english ?: "",
        nameJP = data.title?.romaji ?: "",
        synopsisEN = data.cleanedDescription ?: "",
        synopsisDE = null,
        thumbID = image?.GUID,
        genres = data.genresString(),
        tags = data.tagsString(),
        metadataMap = json.encodeToString(mapping),
        categoryID = category?.GUID,
        added = currentUnixSeconds()
    )

    val (prequel, sequel) = getPrequelOrSequel(data)
    val linkedMedia = media.copy(
        prequel = media.prequel.takeUnless { it.isNullOrBlank() } ?: prequel?.GUID,
        sequel = media.sequel.takeUnless { it.isNullOrBlank() } ?: sequel?.GUID
    )

    return buildList {
        add(linkedMedia)
        if (prequel != null && linkedMedia.prequel == prequel.GUID && prequel.sequel != linkedMedia.GUID)
            add(prequel.copy(sequel = linkedMedia.GUID))
        if (sequel != null && linkedMedia.sequel == sequel.GUID && sequel.prequel != linkedMedia.GUID)
            add(sequel.copy(prequel = linkedMedia.GUID))
    }
}

private fun getPrequelOrSequel(data: MediaBig): Pair<Media?, Media?> {
    val anilistSequel = data.relations?.edges?.find { it?.relationType == MediaRelation.SEQUEL }
    val anilistPrequel = data.relations?.edges?.find { it?.relationType == MediaRelation.PREQUEL }

    val prequel = anilistPrequel?.node?.mediaSmall?.id?.let { findMediaByRemoteID(StackType.ANILIST, it) }
    val sequel = anilistSequel?.node?.mediaSmall?.id?.let { findMediaByRemoteID(StackType.ANILIST, it) }
    return prequel to sequel
}

private fun findMediaByRemoteID(type: StackType, remoteID: Int): Media? =
    dbClient.transaction {
        MediaTable.query {
            selectAll()
                .where { mappingRemoteIdExists(type, remoteID) }
                .limit(2)
                .toList()
        }.singleOrNull()
    }
