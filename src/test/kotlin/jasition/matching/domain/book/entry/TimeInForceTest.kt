package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class TimeInForceTest : StringSpec({
    "Good-till-cancel allows entries to stay on book if available sizes is positive"{
        TimeInForce.GOOD_TILL_CANCEL.canStayOnBook(EntrySizes(available = 1)) shouldBe true
    }
    "Good-till-cancel disallows entries to stay on book if available sizes is zero"{
        TimeInForce.GOOD_TILL_CANCEL.canStayOnBook(EntrySizes(available = 0)) shouldBe false
    }
})