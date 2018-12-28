package jasition.matching.domain.book.entry

import jasition.matching.domain.book.Books
import jasition.matching.domain.book.LimitBook

enum class Side {
    BUY {
        override fun comparatorMultiplier(): Int = -1
        override fun sameSideBook(books: Books): LimitBook = books.buyLimitBook
        override fun oppositeSideBook(books: Books): LimitBook = books.sellLimitBook
    },
    SELL {
        override fun comparatorMultiplier(): Int = 1
        override fun sameSideBook(books: Books): LimitBook = books.sellLimitBook
        override fun oppositeSideBook(books: Books): LimitBook = books.buyLimitBook
    };

    abstract fun comparatorMultiplier(): Int

    abstract fun sameSideBook(books: Books): LimitBook

    abstract fun oppositeSideBook(books: Books): LimitBook
}