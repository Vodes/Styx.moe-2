package moe.styx.web.data

import com.github.mvysny.dynatest.DynaTest

class OtherDatabasesTest : DynaTest({
    test("decodes Fribb IDs and ignores unrelated fields") {
        val mappings = parseAnimeMappings(
            """[{"anilist_id":164,"mal_id":164,"anisearch_id":3320,
                "type":"MOVIE","anidb_id":7,"themoviedb_id":{"movie":[128]}}]"""
        )

        assertEqual(164, mappings.findMalID(164))
        assertEqual(3320, mappings.findAnisearchID(164))
        assertEqual(null, mappings.findMalID(999))
        assertEqual(null, mappings.findAnisearchID(999))
    }

    test("missing and null destination IDs are independent") {
        val mappings = parseAnimeMappings(
            """[
                {"anilist_id":1,"anisearch_id":101},
                {"anilist_id":2,"mal_id":202},
                {"anilist_id":3,"mal_id":null,"anisearch_id":103},
                {"anilist_id":4,"mal_id":204,"anisearch_id":null},
                {"anilist_id":5}
            ]"""
        )

        assertEqual(null, mappings.findMalID(1))
        assertEqual(101, mappings.findAnisearchID(1))
        assertEqual(202, mappings.findMalID(2))
        assertEqual(null, mappings.findAnisearchID(2))
        assertEqual(null, mappings.findMalID(3))
        assertEqual(103, mappings.findAnisearchID(3))
        assertEqual(204, mappings.findMalID(4))
        assertEqual(null, mappings.findAnisearchID(4))
        assertEqual(null, mappings.findMalID(5))
        assertEqual(null, mappings.findAnisearchID(5))
    }

    test("records without AniList IDs are ignored") {
        val mappings = parseAnimeMappings(
            """[{"mal_id":1,"anisearch_id":101},
                {"anilist_id":null,"mal_id":2,"anisearch_id":102},{}]"""
        )

        assertEqual(0, mappings.size)
        assertEqual(null, mappings.findMalID(1))
        assertEqual(null, mappings.findAnisearchID(2))
    }

    test("duplicate records use the first available ID for each destination") {
        val mappings = parseAnimeMappings(
            """[
                {"anilist_id":1},
                {"anilist_id":1,"mal_id":201},
                {"anilist_id":1,"mal_id":202,"anisearch_id":101},
                {"anilist_id":1,"anisearch_id":102}
            ]"""
        )

        assertEqual(201, mappings.findMalID(1))
        assertEqual(101, mappings.findAnisearchID(1))
    }

    test("empty dataset has no mappings") {
        val mappings = parseAnimeMappings("[]")

        assertEqual(null, mappings.findMalID(1))
        assertEqual(null, mappings.findAnisearchID(1))
    }
})

private fun assertEqual(expected: Int?, actual: Int?) {
    if (expected != actual)
        throw AssertionError("Expected $expected, got $actual")
}
