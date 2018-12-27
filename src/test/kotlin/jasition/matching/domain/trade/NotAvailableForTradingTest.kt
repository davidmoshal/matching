package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.book.entry.EntryQuantity

internal class NotAvailableForTradingTest : StringSpec({
    "Available for trading if available size is positive" {
        notAvailableForTrade(
            aggressor = EntryQuantity(
                availableSize = 10
            )
        ) shouldBe false
    }
    "Not available for trading if available size is zero" {
        notAvailableForTrade(
            aggressor = EntryQuantity(
                availableSize = 0
            )
        ) shouldBe true
    }
})