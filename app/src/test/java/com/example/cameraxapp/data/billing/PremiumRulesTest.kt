package com.example.cameraxapp.data.billing

import com.example.cameraxapp.core.render.FilterInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumRulesTest {

    private val known = setOf("premium_monthly", "premium_yearly")

    @Test
    fun purchasedKnownProduct_isPurchased() {
        assertEquals(
            PurchaseStatus.PURCHASED,
            purchaseStatusOf(BILLING_STATE_PURCHASED, listOf("premium_yearly"), known),
        )
    }

    @Test
    fun pendingKnownProduct_isPending() {
        assertEquals(
            PurchaseStatus.PENDING,
            purchaseStatusOf(BILLING_STATE_PENDING, listOf("premium_monthly"), known),
        )
    }

    @Test
    fun unknownProduct_isNoneEvenWhenPurchased() {
        assertEquals(
            PurchaseStatus.NONE,
            purchaseStatusOf(BILLING_STATE_PURCHASED, listOf("someone_elses_sku"), known),
        )
    }

    @Test
    fun unspecifiedState_isNone() {
        assertEquals(PurchaseStatus.NONE, purchaseStatusOf(0, listOf("premium_monthly"), known))
    }

    @Test
    fun premium_onlyWhenAPurchaseIsPurchased() {
        assertTrue(isPremiumFrom(listOf(PurchaseStatus.PURCHASED)))
        assertTrue(isPremiumFrom(listOf(PurchaseStatus.PENDING, PurchaseStatus.PURCHASED)))
    }

    @Test
    fun watermark_shownForFree_hiddenForPremium() {
        assertTrue(shouldShowWatermark(isPremium = false))
        assertFalse(shouldShowWatermark(isPremium = true))
    }

    @Test
    fun premium_isFalseForNoneOrPendingOnly() {
        assertFalse(isPremiumFrom(emptyList()))
        assertFalse(isPremiumFrom(listOf(PurchaseStatus.NONE)))
        assertFalse(isPremiumFrom(listOf(PurchaseStatus.PENDING)))
        assertFalse(isPremiumFrom(listOf(PurchaseStatus.NONE, PurchaseStatus.PENDING)))
    }

    private val filters = listOf(
        FilterInfo(id = "original", name = "Original", group = "Classic", premium = false),
        FilterInfo(id = "kodak", name = "Kodak", group = "Classic", premium = false),
        FilterInfo(id = "rose_skin", name = "Rose Skin", group = "Portrait", premium = true),
    )

    @Test
    fun save_freeFilter_neverNeedsPaywall() {
        assertFalse(requiresPaywall("kodak", filters, isPremium = false))
    }

    @Test
    fun save_premiumFilter_needsPaywallForFreeUser() {
        assertTrue(requiresPaywall("rose_skin", filters, isPremium = false))
    }

    @Test
    fun save_premiumFilter_doesNotNeedPaywallForPremiumUser() {
        assertFalse(requiresPaywall("rose_skin", filters, isPremium = true))
    }

    @Test
    fun save_unknownFilter_doesNotNeedPaywall() {
        assertFalse(requiresPaywall("missing", filters, isPremium = false))
    }
}
