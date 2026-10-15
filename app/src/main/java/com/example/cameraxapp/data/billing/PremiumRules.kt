package com.example.cameraxapp.data.billing

import com.example.cameraxapp.core.render.FilterInfo

/** Google Play purchase states, as the app reasons about them. */
enum class PurchaseStatus { PURCHASED, PENDING, NONE }

/** Matches Purchase.PurchaseState.PURCHASED in Play Billing. */
internal const val BILLING_STATE_PURCHASED = 1

/** Matches Purchase.PurchaseState.PENDING in Play Billing. */
internal const val BILLING_STATE_PENDING = 2

/**
 * Maps one raw Play purchase to a status. A purchase only counts for this app when it names one
 * of [knownProductIds]; anything else is [PurchaseStatus.NONE].
 */
fun purchaseStatusOf(
    billingState: Int,
    productIds: List<String>,
    knownProductIds: Set<String>,
): PurchaseStatus {
    if (productIds.none { it in knownProductIds }) return PurchaseStatus.NONE
    return when (billingState) {
        BILLING_STATE_PURCHASED -> PurchaseStatus.PURCHASED
        BILLING_STATE_PENDING -> PurchaseStatus.PENDING
        else -> PurchaseStatus.NONE
    }
}

/**
 * Premium is granted only by a completed purchase. A pending purchase (for example awaiting
 * payment) does not unlock anything until Play reports it as purchased.
 */
fun isPremiumFrom(statuses: List<PurchaseStatus>): Boolean =
    statuses.any { it == PurchaseStatus.PURCHASED }

/**
 * A save of a premium filter needs the paywall unless the user is premium. Free users can still
 * preview premium filters, so this is only consulted on save.
 */
fun requiresPaywall(filterId: String, filters: List<FilterInfo>, isPremium: Boolean): Boolean {
    if (isPremium) return false
    return filters.any { it.id == filterId && it.premium }
}
