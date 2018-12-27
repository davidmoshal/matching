package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side

internal class PriceHasCrossedTest : StringSpec({
    val entry = aBookEntry()
    val entryKey = entry.key

    "BUY aggressor of lower price and SELL passive of higher price do not cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(9))),
            passive = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(10)))
        ) shouldBe false
    }
    "BUY aggressor and SELL passive of same price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(10))),
            passive = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(10)))
        ) shouldBe true
    }
    "BUY aggressor of higher price and SELL passive of lower price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(10))),
            passive = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(9)))
        ) shouldBe true
    }
    "BUY aggressor without price and SELL passive with price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.BUY, key = entryKey.copy(price = null)),
            passive = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(10)))
        ) shouldBe true
    }
    "BUY aggressor with price and SELL passive without price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(10))),
            passive = entry.copy(side = Side.SELL, key = entryKey.copy(price = null))
        ) shouldBe true
    }
    "SELL aggressor of higher price and BUY passive of lower price do not cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(10))),
            passive = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(9)))
        ) shouldBe false
    }
    "SELL aggressor and BUY passive of same price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(10))),
            passive = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(10)))
        ) shouldBe true
    }
    "SELL aggressor of lower price and BUY passive of higher price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(9))),
            passive = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(10)))
        ) shouldBe true
    }
    "SELL aggressor without price and BUY passive with price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.SELL, key = entryKey.copy(price = null)),
            passive = entry.copy(side = Side.BUY, key = entryKey.copy(price = Price(10)))
        ) shouldBe true
    }
    "SELL aggressor with price and BUY passive without price cross" {
        priceHasCrossed(
            aggressor = entry.copy(side = Side.SELL, key = entryKey.copy(price = Price(10))),
            passive = entry.copy(side = Side.BUY, key = entryKey.copy(price = null))
        ) shouldBe true
    }
})