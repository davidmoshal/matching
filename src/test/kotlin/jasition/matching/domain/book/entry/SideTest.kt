package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.aBookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.LimitBook

internal class SideTest : StringSpec({
    val buyLimitBook = LimitBook(Side.BUY)
    val sellLimitBook = LimitBook(Side.SELL)
    val books = Books(aBookId())
        .copy(buyLimitBook = buyLimitBook, sellLimitBook = sellLimitBook)

    "BUY side has -1 as the comparator multiplier"{
        Side.BUY.comparatorMultiplier() shouldBe -1
    }
    "BUY side has the same side book as BUY"{
        Side.BUY.sameSideBook(books) shouldBe buyLimitBook
    }
    "BUY side has the opposite side book as SELL"{
        Side.BUY.oppositeSideBook(books) shouldBe sellLimitBook
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
})