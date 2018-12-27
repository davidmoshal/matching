package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.book.entry.EntryQuantity

internal class GetTradeSizeTest : StringSpec({
    "Use passive available size if it is smaller" {
        getTradeSize(
            aggressor = EntryQuantity(availableSize = 10),
            passive = EntryQuantity(availableSize = 9)
        ) shouldBe 9
    }
    "Use aggressor available size if it is smaller" {
        getTradeSize(
            aggressor = EntryQuantity(availableSize = 10),
            passive = EntryQuantity(availableSize = 11)
        ) shouldBe 10
    }
    "Use the available size of aggressor and passive if they are the same" {
        getTradeSize(
            aggressor = EntryQuantity(availableSize = 11),
            passive = EntryQuantity(availableSize = 11)
        ) shouldBe 11
    }
})