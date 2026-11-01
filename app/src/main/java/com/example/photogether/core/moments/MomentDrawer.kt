package com.example.photogether.core.moments

/** The two drawers of the gallery: photobooth strips on their own, everything shot or edited in the other. */
enum class MomentDrawer {
    PHOTOBOOTH,
    SHOTS;

    fun accepts(kind: MomentKind): Boolean = this == of(kind)

    fun filter(moments: List<Moment>): List<Moment> = moments.filter { accepts(it.kind) }

    companion object {
        fun of(kind: MomentKind): MomentDrawer = if (kind == MomentKind.PHOTOBOOTH) PHOTOBOOTH else SHOTS
    }
}
