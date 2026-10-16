package com.example.cameraxapp.core.render

/** One chip above the filter strip. [id] is what [FilterGrouping.filterBy] takes back. */
data class FilterChipSpec(val id: String, val label: String)

/**
 * Splits the filter strip into chips: ALL, then FAVORITE when the user has favorites, then one
 * chip per filter group in catalog order. Pure Kotlin, usable from JVM unit tests.
 */
object FilterGrouping {

    const val ALL = "all"
    const val FAVORITE = "favorite"
    private const val GROUP_PREFIX = "group:"

    fun chips(filters: List<FilterInfo>, hasFavorites: Boolean): List<FilterChipSpec> {
        val chips = mutableListOf(FilterChipSpec(ALL, "ALL"))
        if (hasFavorites) chips += FilterChipSpec(FAVORITE, "FAVORITE")
        filters.map { it.group }.distinct().forEach { group ->
            chips += FilterChipSpec("$GROUP_PREFIX$group", group)
        }
        return chips
    }

    /**
     * Returns the filters behind [chipId], in catalog order. An unknown chip id behaves like ALL.
     * The original filter leads every non-empty result. An empty FAVORITE selection stays empty.
     */
    fun filterBy(chipId: String, filters: List<FilterInfo>, favorites: Set<String>): List<FilterInfo> {
        val selected = when {
            chipId == FAVORITE -> filters.filter { it.id in favorites }
            chipId.startsWith(GROUP_PREFIX) -> {
                val group = chipId.removePrefix(GROUP_PREFIX)
                if (filters.none { it.group == group }) filters else filters.filter { it.group == group }
            }
            else -> filters
        }
        return originalFirst(selected, filters)
    }

    private fun originalFirst(selected: List<FilterInfo>, all: List<FilterInfo>): List<FilterInfo> {
        if (selected.isEmpty()) return selected
        val original = all.firstOrNull { it.id == FilterCatalog.ORIGINAL_ID } ?: return selected
        return listOf(original) + selected.filter { it.id != FilterCatalog.ORIGINAL_ID }
    }
}
