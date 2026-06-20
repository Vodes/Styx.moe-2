package moe.styx.web.util

import com.github.mvysny.dynatest.DynaTest
import moe.styx.common.data.BasicMapping
import moe.styx.common.data.MappingCollection
import moe.styx.common.data.TMDBMapping

class MappingUtilTest : DynaTest({
    test("mergeMappings converts fallbacks into offset ranges") {
        val merged = mergeMappings(
            targetMappings = MappingCollection(
                anilistMappings = mutableListOf(BasicMapping(remoteID = 108511)),
                malMappings = mutableListOf(BasicMapping(remoteID = 39551))
            ),
            donorMappings = MappingCollection(
                anilistMappings = mutableListOf(BasicMapping(remoteID = 116742)),
                malMappings = mutableListOf(BasicMapping(remoteID = 41487))
            ),
            targetEpisodeCount = 12,
            donorEpisodeCount = 12
        )

        merged.anilistMappings[0].assertMapping(1.0, 12.0, 108511, 0.0)
        merged.anilistMappings[1].assertMapping(13.0, 24.0, 116742, -12.0)
        merged.malMappings[0].assertMapping(1.0, 12.0, 39551, 0.0)
        merged.malMappings[1].assertMapping(13.0, 24.0, 41487, -12.0)
    }

    test("mergeMappings offsets donor-specific mappings") {
        val merged = mergeMappings(
            targetMappings = MappingCollection(),
            donorMappings = MappingCollection(
                anilistMappings = mutableListOf(
                    BasicMapping(matchFrom = 1.0, matchUntil = 3.0, remoteID = 1),
                    BasicMapping(matchFrom = 6.5, remoteID = 2, offset = -5.5)
                )
            ),
            targetEpisodeCount = 12,
            donorEpisodeCount = 8
        )

        merged.anilistMappings[0].assertMapping(13.0, 15.0, 1, -12.0)
        merged.anilistMappings[1].assertMapping(18.5, -1.0, 2, -17.5)
    }

    test("mergeMappings skips donor mapping already covered after episode shift") {
        val merged = mergeMappings(
            targetMappings = MappingCollection(
                tmdbMappings = mutableListOf(TMDBMapping(remoteID = 82684, seasonEntry = 3))
            ),
            donorMappings = MappingCollection(
                tmdbMappings = mutableListOf(TMDBMapping(remoteID = 82684, seasonEntry = 3, offset = 12.0))
            ),
            targetEpisodeCount = 12,
            donorEpisodeCount = 12
        )

        if (merged.tmdbMappings.size != 1)
            throw AssertionError("Expected one TMDB mapping, got ${merged.tmdbMappings}")
        merged.tmdbMappings[0].assertMapping(-1.0, -1.0, 82684, 0.0, 3)
    }

    test("mergeMappings extends explicit target range when skipping covered donor mapping") {
        val merged = mergeMappings(
            targetMappings = MappingCollection(
                tmdbMappings = mutableListOf(TMDBMapping(matchFrom = 1.0, matchUntil = 12.0, remoteID = 82684, seasonEntry = 3))
            ),
            donorMappings = MappingCollection(
                tmdbMappings = mutableListOf(TMDBMapping(remoteID = 82684, seasonEntry = 3, offset = 12.0))
            ),
            targetEpisodeCount = 12,
            donorEpisodeCount = 12
        )

        if (merged.tmdbMappings.size != 1)
            throw AssertionError("Expected one TMDB mapping, got ${merged.tmdbMappings}")
        merged.tmdbMappings[0].assertMapping(1.0, 24.0, 82684, 0.0, 3)
    }

    test("offsetEpisode formats integer and half episodes") {
        assertEquals("13", offsetEpisode("1", 12))
        assertEquals("18.5", offsetEpisode("6.5", 12))
    }
})

private fun BasicMapping.assertMapping(matchFrom: Double, matchUntil: Double, remoteID: Int, offset: Double) {
    if (this.matchFrom != matchFrom || this.matchUntil != matchUntil || this.remoteID != remoteID || this.offset != offset) {
        throw AssertionError("Expected ($matchFrom, $matchUntil, $remoteID, $offset), got $this")
    }
}

private fun TMDBMapping.assertMapping(matchFrom: Double, matchUntil: Double, remoteID: Int, offset: Double, seasonEntry: Int) {
    if (
        this.matchFrom != matchFrom ||
        this.matchUntil != matchUntil ||
        this.remoteID != remoteID ||
        this.offset != offset ||
        this.seasonEntry != seasonEntry
    ) {
        throw AssertionError("Expected ($matchFrom, $matchUntil, $remoteID, $offset, $seasonEntry), got $this")
    }
}

private fun assertEquals(expected: String, actual: String?) {
    if (expected != actual)
        throw AssertionError("Expected $expected, got $actual")
}
