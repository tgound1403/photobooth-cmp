package com.example.photogether.core.analytics

/**
 * Product analytics events. Names are stable: they are the keys dashboards are built on.
 */
enum class AnalyticsEvent(val eventName: String) {
    PHOTO_CAPTURED("photo_captured"),
    FILTER_APPLIED("filter_applied"),
    IMAGE_SAVED("image_saved"),
    PAYWALL_SHOWN("paywall_shown"),
    PURCHASE_COMPLETED("purchase_completed"),
}

/**
 * Sink for analytics events. The app ships with [NoOpAnalytics] until a real backend (Firebase)
 * is wired in; call sites do not change when it is.
 */
interface Analytics {
    fun log(event: AnalyticsEvent, params: Map<String, String> = emptyMap())
}

object NoOpAnalytics : Analytics {
    override fun log(event: AnalyticsEvent, params: Map<String, String>) = Unit
}
