package com.example.photogether.core.render

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
    private const val TAG_PREFIX = "tag:"

    /**
     * Groups browsed by tag instead of by a chip of their own. The Japan Pack has hundreds of looks,
     * so a single chip would show nearly the whole catalog. Its looks still show under ALL and the tag chips.
     */
    private val TAGGED_GROUPS = setOf("Japan Pack")

    /** Tag chips in display order. Must match TAG_ORDER in tools/lut-gen/generate_luts.py. */
    private val TAG_LABELS = linkedMapOf("warm" to "Warm", "cool" to "Cool", "soft" to "Soft", "bold" to "Bold", "mono" to "Mono")

    fun chips(filters: List<FilterInfo>, hasFavorites: Boolean): List<FilterChipSpec> {
        val chips = mutableListOf(FilterChipSpec(ALL, "ALL"))
        if (hasFavorites) chips += FilterChipSpec(FAVORITE, "FAVORITE")
        filters.map { it.group }.distinct().filter { it !in TAGGED_GROUPS }.forEach { group ->
            chips += FilterChipSpec("$GROUP_PREFIX$group", group)
        }
        val presentTags = filters.flatMap { it.tags }.toSet()
        TAG_LABELS.forEach { (tag, label) ->
            if (tag in presentTags) chips += FilterChipSpec("$TAG_PREFIX$tag", label)
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
            chipId.startsWith(TAG_PREFIX) -> {
                val tag = chipId.removePrefix(TAG_PREFIX)
                if (filters.none { tag in it.tags }) filters else filters.filter { tag in it.tags }
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
