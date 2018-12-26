package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class TimeInForceTest : StringSpec({
    "Good-till-cancel allows entries to stay on book if available size is positive"{
        TimeInForce.GOOD_TILL_CANCEL.canStayOnBook(EntryQuantity(availableSize = 1)) shouldBe true
    }
    "Good-till-cancel disallows entries to stay on book if available size is zero"{
        TimeInForce.GOOD_TILL_CANCEL.canStayOnBook(EntryQuantity(availableSize = 0)) shouldBe false
    }
})