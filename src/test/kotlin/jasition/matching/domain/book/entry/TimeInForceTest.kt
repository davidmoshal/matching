package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec

internal class TimeInForceTest : DescribeSpec (){
    init {
        describe("Good-till-cancel as time-in-force") {
            val timeInForce = TimeInForce.GOOD_TILL_CANCEL
            it("allows entries to stay on book if available size is positive") {
                timeInForce.canStayOnBook(EntryQuantity(availableSize = 1)) shouldBe true
            }
            it("disallows entries to stay on book if available size is zero") {
                timeInForce.canStayOnBook(EntryQuantity(availableSize = 0)) shouldBe false
            }
        }
    }
}