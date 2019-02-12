package jasition.matching.domain.book.entry

import jasition.matching.domain.book.Books
import jasition.matching.domain.book.LimitBook
import jasition.matching.domain.quote.QuoteEntry

enum class Side {
    BUY {
        override fun oppositeSide(): Side = Side.SELL
        override fun priceWithSize(quoteEntry: QuoteEntry): PriceWithSize? = quoteEntry.bid
        override fun comparatorMultiplier(): Int = -1
        override fun sameSideBook(books: Books): LimitBook = books.buyLimitBook
        override fun oppositeSideBook(books: Books): LimitBook = books.sellLimitBook
    },
    SELL {
        override fun oppositeSide(): Side = Side.BUY
        override fun priceWithSize(quoteEntry: QuoteEntry): PriceWithSize? = quoteEntry.offer
        override fun comparatorMultiplier(): Int = 1
        override fun sameSideBook(books: Books): LimitBook = books.sellLimitBook
        override fun oppositeSideBook(books: Books): LimitBook = books.buyLimitBook
    };

    //TODO: Unit test
    abstract fun oppositeSide(): Side

    abstract fun comparatorMultiplier(): Int

    abstract fun sameSideBook(books: Books): LimitBook

    abstract fun oppositeSideBook(books: Books): LimitBook

    abstract fun priceWithSize(quoteEntry: QuoteEntry): PriceWithSize?
}