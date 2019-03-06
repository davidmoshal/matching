package jasition.matching.domain.book.entry

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.EntryType.MARKET
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL


internal class EntryTypeTimeInForceComboTest : StringSpec({
    forall(
        row(LIMIT, GOOD_TILL_CANCEL, true),
        row(LIMIT, IMMEDIATE_OR_CANCEL, true),
        row(MARKET, GOOD_TILL_CANCEL, false),
        row(MARKET, IMMEDIATE_OR_CANCEL, true)
    ) { entryType, timeInForce, expectedResult ->
        "$entryType and $timeInForce is ${if (expectedResult) "a valid" else "an invalid"} combo" {
            EntryTypeTimeInForceCombo.isValid(entryType, timeInForce) shouldBe expectedResult
        }
    }
})