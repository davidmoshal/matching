package jasition.matching.domain.book

import io.kotlintest.specs.DescribeSpec
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side

internal class LimitBookTest : DescribeSpec() {
    init {
        describe("BUY LimitBook") {
            val limitBook = LimitBook(Side.BUY)
            it("prioritises higher prices over lower prices") {
                limitBook.add(aBookEntry(side = Side.BUY, price = Price(10)))
            }
        }
    }
}