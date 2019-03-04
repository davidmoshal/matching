package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.*
import jasition.matching.domain.book.LimitBook

internal class SideTest : StringSpec({
    val buyLimitBook = LimitBook(Side.BUY)
    val sellLimitBook = LimitBook(Side.SELL)
    val books = aBooks(aBookId())
        .copy(buyLimitBook = buyLimitBook, sellLimitBook = sellLimitBook)

    "Opposite side of BUY is SELL" {
        Side.BUY.oppositeSide() shouldBe Side.SELL
    }
    "BUY side has -1 as the comparator multiplier"{
        Side.BUY.comparatorMultiplier() shouldBe -1
    }
    "BUY side has the same side book as BUY"{
        Side.BUY.sameSideBook(books) shouldBe buyLimitBook
    }
    "BUY side has the opposite side book as SELL"{
        Side.BUY.oppositeSideBook(books) shouldBe sellLimitBook
    }
    "Returns the BID side of a quote entry" {
        val price = randomPrice()
        val size = randomSize()
        Side.BUY.sizeAtPrice(
            aQuoteEntry(
                bid = SizeAtPrice(price = price, size = size)
            )
        ) shouldBe SizeAtPrice(price = price, size = size)
    }
    "Returns null if the BID side is absent in a quote entry" {
        Side.BUY.sizeAtPrice(aQuoteEntry(bid = null)) shouldBe null
    }
    "Opposite side of SELL is BUY" {
        Side.SELL.oppositeSide() shouldBe Side.BUY
    }
    "SELL side has 1 as the comparator multiplier"{
        Side.SELL.comparatorMultiplier() shouldBe 1
    }
    "SELL side has the same side book as SELL"{
        Side.SELL.sameSideBook(books) shouldBe sellLimitBook
    }
    "SELL side has the opposite side book as BUY"{
        Side.SELL.oppositeSideBook(books) shouldBe buyLimitBook
    }
    "Returns the OFFER side of a quote entry" {
        val price = randomPrice()
        val size = randomSize()
        Side.SELL.sizeAtPrice(
            aQuoteEntry(offer = SizeAtPrice(size = size, price = price))
        ) shouldBe SizeAtPrice(size = size, price = price)
    }
    "Returns null if the OFFER side is absent in a quote entry" {
        Side.SELL.sizeAtPrice(aQuoteEntry(offer = null)) shouldBe null
    }
})