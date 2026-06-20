package moe.styx.web.util

import moe.styx.common.data.BasicMapping
import moe.styx.common.data.IMapping
import moe.styx.common.data.MappingCollection
import moe.styx.common.data.TMDBMapping
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun mergeMappings(
    targetMappings: MappingCollection?,
    donorMappings: MappingCollection?,
    targetEpisodeCount: Int,
    donorEpisodeCount: Int
): MappingCollection {
    val targetCollection = targetMappings ?: MappingCollection()
    val donorCollection = donorMappings ?: MappingCollection()

    return MappingCollection(
        tmdbMappings = mergeMappingList(
            targetCollection.tmdbMappings,
            donorCollection.tmdbMappings,
            targetEpisodeCount,
            donorEpisodeCount
        ) { copyMapping(it) as TMDBMapping }.toMutableList(),
        anilistMappings = mergeMappingList(
            targetCollection.anilistMappings,
            donorCollection.anilistMappings,
            targetEpisodeCount,
            donorEpisodeCount
        ) { copyMapping(it) as BasicMapping }.toMutableList(),
        malMappings = mergeMappingList(
            targetCollection.malMappings,
            donorCollection.malMappings,
            targetEpisodeCount,
            donorEpisodeCount
        ) { copyMapping(it) as BasicMapping }.toMutableList()
    )
}

private fun <T : IMapping> mergeMappingList(
    targetMappings: List<T>,
    donorMappings: List<T>,
    targetEpisodeCount: Int,
    donorEpisodeCount: Int,
    copy: (T) -> T
): List<T> {
    val targetCopies = targetMappings.map { mapping ->
        val hasCoveredDonor = donorMappings.any { donor -> mapping.coversShiftedDonor(donor, targetEpisodeCount) }
        copy(mapping).apply {
            if (!hasCoveredDonor)
                normalizeFallback(targetEpisodeCount)
        }
    }
    val donorCopies = donorMappings.mapNotNull { donor ->
        val matchingTarget = targetCopies.find { target -> target.coversShiftedDonor(donor, targetEpisodeCount) }
        if (matchingTarget != null) {
            matchingTarget.extendRangeTo(targetEpisodeCount + donorEpisodeCount)
            null
        } else {
            copy(donor).normalizeFallback(donorEpisodeCount).offsetLocalEpisodes(targetEpisodeCount)
        }
    }
    return targetCopies + donorCopies
}

private fun <T : IMapping> T.normalizeFallback(entryCount: Int): T {
    if (matchFrom == -1.0 && matchUntil == -1.0 && entryCount > 0) {
        matchFrom = 1.0
        matchUntil = entryCount.toDouble()
    }
    return this
}

private fun <T : IMapping> T.offsetLocalEpisodes(episodeOffset: Int): T {
    val offset = episodeOffset.toDouble()
    if (matchFrom != -1.0)
        matchFrom += offset
    if (matchUntil != -1.0)
        matchUntil += offset
    this.offset -= offset
    return this
}

private fun IMapping.coversShiftedDonor(donor: IMapping, episodeOffset: Int): Boolean =
    pointsToSameRemoteAs(donor) && donor.offset == episodeOffset.toDouble() && canCoverDonorEpisodes()

private fun IMapping.pointsToSameRemoteAs(other: IMapping): Boolean =
    when {
        this is TMDBMapping && other is TMDBMapping ->
            remoteID == other.remoteID &&
                    seasonEntry == other.seasonEntry &&
                    orderType == other.orderType &&
                    orderID == other.orderID

        this is BasicMapping && other is BasicMapping -> remoteID == other.remoteID
        else -> false
    }

private fun IMapping.canCoverDonorEpisodes(): Boolean =
    matchFrom == -1.0 && matchUntil == -1.0 || matchFrom != -1.0 && matchUntil != -1.0 && matchFrom != matchUntil

private fun IMapping.extendRangeTo(episodeCount: Int) {
    if (matchFrom != -1.0 && matchUntil != -1.0 && matchFrom != matchUntil)
        matchUntil = maxOf(matchUntil, episodeCount.toDouble())
}

private fun copyMapping(mapping: IMapping): IMapping =
    when (mapping) {
        is TMDBMapping -> mapping.copy()
        is BasicMapping -> mapping.copy()
        else -> error("Unsupported mapping type: ${mapping::class.simpleName}")
    }

internal fun offsetEpisode(episode: String, offset: Int): String? {
    val number = episode.toDoubleOrNull() ?: return null
    return DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(number + offset)
}
