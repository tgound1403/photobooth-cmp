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
    fun chipsWithoutFavoritesAreAllAndFiveGroupsInCatalogOrder() {
        val chips = FilterGrouping.chips(filters, hasFavorites = false)

        assertEquals(
            listOf("all", "group:Classic", "group:Film", "group:Portrait", "group:Mood", "group:B&W"),
            chips.map { it.id },
        )
    }

    @Test
    fun chipsWithFavoritesPutFavoriteSecond() {
        val chips = FilterGrouping.chips(filters, hasFavorites = true)

        assertEquals(7, chips.size)
        assertEquals(FilterGrouping.FAVORITE, chips[1].id)
    }

    @Test
    fun filmGroupHasSixFilmFiltersPlusOriginalFirst() {
        val film = FilterGrouping.filterBy("group:Film", filters, emptySet())

        assertEquals(FilterCatalog.ORIGINAL_ID, film.first().id)
        assertEquals(6, film.count { it.group == "Film" })
        assertEquals(7, film.size)
    }

    @Test
    fun classicGroupKeepsOriginalOnlyOnce() {
        val classic = FilterGrouping.filterBy("group:Classic", filters, emptySet())

        assertEquals(FilterCatalog.ORIGINAL_ID, classic.first().id)
        assertEquals(1, classic.count { it.id == FilterCatalog.ORIGINAL_ID })
        assertTrue(classic.all { it.group == "Classic" })
    }

    @Test
    fun favoriteReturnsOriginalThenFavoritesInCatalogOrder() {
        val result = FilterGrouping.filterBy(
            FilterGrouping.FAVORITE,
            filters,
            favorites = setOf("ink_wash", "kodak"),
        )

        assertEquals(listOf("original", "kodak", "ink_wash"), ids(result))
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

        assertEquals(31, unknownChip.size)
        assertEquals(ids(filters), ids(unknownChip))
        assertEquals(ids(filters), ids(unknownGroup))
    }

    @Test
    fun everyNonEmptyChipListStartsWithOriginal() {
        val favorites = setOf("kodak", "ink_wash")
        FilterGrouping.chips(filters, hasFavorites = true).forEach { chip ->
            val result = FilterGrouping.filterBy(chip.id, filters, favorites)
            if (result.isNotEmpty()) {
                assertEquals("chip ${chip.id}", FilterCatalog.ORIGINAL_ID, result.first().id)
            }
        }
    }
}
