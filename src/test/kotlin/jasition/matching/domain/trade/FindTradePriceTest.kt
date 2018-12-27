package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.book.entry.Price

internal class FindTradePriceTest : StringSpec({
    "Use passive price if present" {
        findTradePrice(
            aggressor = Price(9),
            passive = Price(10)
        ) shouldBe Price(10)
    }
    "Use aggressor price if passive price is absent" {
        findTradePrice(
            aggressor = Price(9),
            passive = null
        ) shouldBe Price(9)
    }
    "No price found if both aggressor and passive prices are absent" {
        findTradePrice(aggressor = null, passive = null) shouldBe null
    }
})