package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side

internal class FindTradePriceTest : StringSpec({
    val lowerPrice = Price(8)
    val samePrice = Price(9)
    val higherPrice = Price(10)

    "Given BUY aggressor without price and SELL passive without price, then no trade price found" {
        findTradePrice(aggressorSide = Side.BUY, aggressor = null, passive = null) shouldBe null
    }
    "Given BUY aggressor of lower price and SELL passive of higher price, then no trade price is found" {
        findTradePrice(aggressorSide = Side.BUY, aggressor = lowerPrice, passive = higherPrice) shouldBe null
    }
    "Given BUY aggressor and SELL passive of same price, then trade price is the price" {
        findTradePrice(aggressorSide = Side.BUY, aggressor = samePrice, passive = samePrice) shouldBe samePrice
    }
    "Given BUY aggressor of higher price and SELL passive of lower price, then the trade price is the lower price" {
        findTradePrice(aggressorSide = Side.BUY, aggressor = higherPrice, passive = lowerPrice) shouldBe lowerPrice
    }
    "Given BUY aggressor without price and SELL passive with price, then the trade price is the passive price" {
        findTradePrice(aggressorSide = Side.BUY, aggressor = null, passive = samePrice) shouldBe samePrice
    }
    "Given BUY aggressor with price and SELL passive without price, then the trade price is the aggressor price" {
        findTradePrice(aggressorSide = Side.BUY, aggressor = samePrice, passive = null) shouldBe samePrice
    }
    "Given SELL aggressor without price and BUY passive without price, then no trade price found" {
        findTradePrice(aggressorSide = Side.SELL, aggressor = null, passive = null) shouldBe null
    }
    "Given SELL aggressor of higher price and BUY passive of lower price, then no trade price is found" {
        findTradePrice(aggressorSide = Side.SELL, aggressor = higherPrice, passive = lowerPrice) shouldBe null
    }
    "Given SELL aggressor and BUY passive of same price, then trade price is the price" {
        findTradePrice(aggressorSide = Side.SELL, aggressor = samePrice, passive = samePrice) shouldBe samePrice
    }
    "Given SELL aggressor of lower price and BUY passive of higher price, then the trade price is the higher price" {
        findTradePrice(aggressorSide = Side.SELL, aggressor = lowerPrice, passive = higherPrice) shouldBe higherPrice
    }
    "Given SELL aggressor without price and BUY passive with price, then the trade price is the passive price" {
        findTradePrice(aggressorSide = Side.SELL, aggressor = null, passive = samePrice) shouldBe samePrice
    }
    "Given SELL aggressor with price and BUY passive without price, then the trade price is the aggressor price" {
        findTradePrice(aggressorSide = Side.SELL, aggressor = samePrice, passive = null) shouldBe samePrice
    }
})