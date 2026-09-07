package com.tieyoh.mobile

// Thin data-access layer for the Feed screen, kept separate from the
// composable so the "which endpoint, how a page works, what counts as an
// error" logic doesn't live inside UI code. This app doesn't use an
// Android ViewModel/lifecycle layer anywhere else -- every other screen
// (ContactsInviteScreen, ProfileSetupScreen, NotificationsScreen, and the
// Feed screen itself before this) calls RetrofitClient directly from a
// LaunchedEffect/coroutine in the composable. Introducing a full
// ViewModel just for this one screen would be a parallel architecture
// the spec itself warns against ("adapt this requirement to the existing
// Tieyoh architecture" / don't build parallel structures just because
// names differ) -- so this stays a plain repository object instead,
// which still gives a clean UI/data separation without a one-off pattern.
object FeedRepository {

    suspend fun loadPage(viewerId: Long, page: Int, size: Int = 20): FeedPageResult {
        return try {
            val response = RetrofitClient.feedApi.getFeed(viewerId = viewerId, page = page, size = size)
            FeedPageResult.Success(response.items, response.hasMore)
        } catch (e: Exception) {
            FeedPageResult.Error(e.message ?: "Couldn't load your feed right now")
        }
    }
}

sealed class FeedPageResult {
    data class Success(val items: List<FeedItem>, val hasMore: Boolean) : FeedPageResult()
    data class Error(val message: String) : FeedPageResult()
}
