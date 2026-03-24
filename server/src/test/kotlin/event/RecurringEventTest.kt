package event

import kotlin.test.Ignore
import kotlin.test.Test

class RecurringEventTest {
    @Test @Ignore("Wave 0 stub - ES-08: create recurring series")
    fun `POST events with recurring creates series and materialises`() {}

    @Test @Ignore("Wave 0 stub - ES-09: edit recurring this only")
    fun `PATCH event with scope this_only updates single event`() {}

    @Test @Ignore("Wave 0 stub - ES-09: edit recurring this and future")
    fun `PATCH event with scope this_and_future splits series`() {}

    @Test @Ignore("Wave 0 stub - ES-10: cancel recurring all")
    fun `POST cancel with scope all cancels entire series`() {}
}
