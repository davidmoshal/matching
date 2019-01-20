@file:JvmName("TestExecutor")
package jasition.matching.domain

import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.quote.command.PlaceMassQuoteCommand

fun validateAndPlay(command: PlaceOrderCommand, books : Books) : Transaction<BookId, Books> {
    val result = command.validate(books)

    if (result.isLeft()) {
        throw IllegalStateException("Order should be placed but was rejected")
    }

    return result.fold({ it.play(books) }, { it.play(books) })
}
fun validateAndPlay(command: PlaceMassQuoteCommand, books : Books) : Transaction<BookId, Books> {
    val result = command.validate(books)

    if (result.isLeft()) {
        throw IllegalStateException("Mass Quote should be placed but was rejected")
    }

    return result.fold({ it.play(books) }, { it.play(books) })
}

