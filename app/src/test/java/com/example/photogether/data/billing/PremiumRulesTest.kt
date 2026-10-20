package com.example.photogether.data.billing

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
}
