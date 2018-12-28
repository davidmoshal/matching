package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.book.entry.EntrySizes

internal class GetTradeSizeTest : StringSpec({
    "Use passive available sizes if it is smaller" {
        getTradeSize(
            aggressor = EntrySizes(available = 10),
            passive = EntrySizes(available = 9)
        ) shouldBe 9
    }
    "Use aggressor available sizes if it is smaller" {
        getTradeSize(
            aggressor = EntrySizes(available = 10),
            passive = EntrySizes(available = 11)
        ) shouldBe 10
    }
    "Use the available sizes of aggressor and passive if they are the same" {
        getTradeSize(
            aggressor = EntrySizes(available = 11),
            passive = EntrySizes(available = 11)
        ) shouldBe 11
    }
})