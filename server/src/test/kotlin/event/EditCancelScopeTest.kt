package event

import kotlin.test.Ignore
import kotlin.test.Test

class EditCancelScopeTest {
    @Test @Ignore("Wave 0 stub - ES-09: scope this_only preserves overrides")
    fun `edit this_only marks event as series override`() {}

    @Test @Ignore("Wave 0 stub - ES-09: scope this_and_future creates new series")
    fun `edit this_and_future ends old series and creates new`() {}

    @Test @Ignore("Wave 0 stub - ES-10: scope all updates template and future events")
    fun `edit all updates series template and non-override events`() {}

    @Test @Ignore("Wave 0 stub - ES-10: past events unchanged")
    fun `edit all does not modify past events`() {}
}
