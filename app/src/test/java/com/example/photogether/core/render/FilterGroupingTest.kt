package com.example.photogether.core.render

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterGroupingTest {

    private val catalogFile = File("src/main/assets/filters/catalog.json")
    private val filters by lazy { FilterCatalog.parse(catalogFile.readText()) }

    private fun ids(list: List<FilterInfo>): List<String> = list.map { it.id }

    @Test
    fun chipsWithoutFavoritesAreAllGroupsThenTagsInOrder() {
        val chips = FilterGrouping.chips(filters, hasFavorites = false)

        assertEquals(
            listOf(
                "all",
                "group:Kodak",
                "group:Fuji",
                "group:Vintage",
                "group:Korea",
                "group:Leica",
                "group:Ricoh",
                "tag:warm",
                "tag:cool",
                "tag:soft",
                "tag:bold",
                "tag:mono",
            ),
            chips.map { it.id },
        )
    }

    @Test
    fun japanPackHasNoGroupChipBecauseTagsBrowseIt() {
        val chips = FilterGrouping.chips(filters, hasFavorites = false).map { it.id }

        assertTrue(chips.none { it == "group:Japan Pack" })
        assertTrue(filters.any { it.group == "Japan Pack" && it.tags.isNotEmpty() })
    }

    @Test
    fun chipsWithFavoritesPutFavoriteSecond() {
        val chips = FilterGrouping.chips(filters, hasFavorites = true)

        assertEquals(13, chips.size)
        assertEquals(FilterGrouping.FAVORITE, chips[1].id)
    }

    @Test
    fun tagChipKeepsOnlyLooksWithThatTagAndOriginalFirst() {
        val warm = FilterGrouping.filterBy("tag:warm", filters, emptySet())

        assertEquals(FilterCatalog.ORIGINAL_ID, warm.first().id)
        assertTrue(warm.drop(1).all { "warm" in it.tags })
        assertEquals(1 + filters.count { "warm" in it.tags }, warm.size)
    }

    @Test
    fun fujiGroupHasFiveFujiFiltersPlusOriginalFirst() {
        val fuji = FilterGrouping.filterBy("group:Fuji", filters, emptySet())

        assertEquals(FilterCatalog.ORIGINAL_ID, fuji.first().id)
        assertEquals(5, fuji.count { it.group == "Fuji" })
        assertEquals(6, fuji.size)
    }

    @Test
    fun kodakGroupKeepsOriginalOnlyOnce() {
        val kodak = FilterGrouping.filterBy("group:Kodak", filters, emptySet())

        assertEquals(FilterCatalog.ORIGINAL_ID, kodak.first().id)
        assertEquals(1, kodak.count { it.id == FilterCatalog.ORIGINAL_ID })
        assertTrue(kodak.all { it.group == "Kodak" })
    }

    @Test
    fun favoriteReturnsOriginalThenFavoritesInCatalogOrder() {
        val result = FilterGrouping.filterBy(
            FilterGrouping.FAVORITE,
            filters,
            favorites = setOf("bw_soft_mono", "kodak_portra"),
        )

        assertEquals(listOf("original", "kodak_portra", "bw_soft_mono"), ids(result))
    }

    @Test
    fun favoriteWithNoFavoritesIsEmpty() {
        assertEquals(emptyList<FilterInfo>(), FilterGrouping.filterBy(FilterGrouping.FAVORITE, filters, emptySet()))
    }

    @Test
    fun favoriteIdsUnknownToCatalogAreEmpty() {
        val result = FilterGrouping.filterBy(FilterGrouping.FAVORITE, filters, setOf("ghost"))

        assertEquals(emptyList<FilterInfo>(), result)
    }

    @Test
    fun allChipReturnsWholeCatalogWithOriginalFirst() {
        val result = FilterGrouping.filterBy(FilterGrouping.ALL, filters, emptySet())

        assertEquals(filters.size, result.size)
        assertEquals(FilterCatalog.ORIGINAL_ID, result.first().id)
    }

    @Test
    fun unknownChipIdBehavesLikeAll() {
        val unknownChip = FilterGrouping.filterBy("no_such_chip", filters, emptySet())
        val unknownGroup = FilterGrouping.filterBy("group:Nope", filters, emptySet())

        assertEquals(filters.size, unknownChip.size)
        assertEquals(ids(filters), ids(unknownChip))
        assertEquals(ids(filters), ids(unknownGroup))
    }

    @Test
    fun everyNonEmptyChipListStartsWithOriginal() {
        val favorites = setOf("kodak_portra", "bw_soft_mono")
        FilterGrouping.chips(filters, hasFavorites = true).forEach { chip ->
            val result = FilterGrouping.filterBy(chip.id, filters, favorites)
            if (result.isNotEmpty()) {
                assertEquals("chip ${chip.id}", FilterCatalog.ORIGINAL_ID, result.first().id)
            }
        }
    }
}
