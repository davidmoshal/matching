package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.LimitBook

internal class SideTest : DescribeSpec() {
    init {
        describe("BUY side") {
            val side = Side.BUY
            val buyLimitBook = LimitBook(Side.BUY)
            val sellLimitBook = LimitBook(Side.SELL)
            val books = Books(BookId("bookId"))
                .copy(buyLimitBook = buyLimitBook, sellLimitBook = sellLimitBook)

            it("has -1 as the comparator multilier") {
                side.comparatorMultipler() shouldBe -1
            }
            it("has the same side book as BUY") {
                side.sameSideBook(books) shouldBe buyLimitBook
            }
            it("has the opposite side book as SELL") {
                side.oppositeSideBook(books) shouldBe sellLimitBook
            }
        }
        describe("SELL side") {
            val side = Side.SELL
            val buyLimitBook = LimitBook(Side.BUY)
            val sellLimitBook = LimitBook(Side.SELL)
            val books = Books(BookId("bookId"))
                .copy(buyLimitBook = buyLimitBook, sellLimitBook = sellLimitBook)

            it("has 1 as the comparator multilier") {
                side.comparatorMultipler() shouldBe 1
            }
            it("has the same side book as SELL") {
                side.sameSideBook(books) shouldBe sellLimitBook
            }
            it("has the opposite side book as BUY") {
                side.oppositeSideBook(books) shouldBe buyLimitBook
            }
        }
    }
}