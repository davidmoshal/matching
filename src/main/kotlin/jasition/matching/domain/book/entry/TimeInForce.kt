package jasition.matching.domain.book.entry

import io.vavr.collection.List
import jasition.cqrs.Transaction
import jasition.cqrs.Transaction_2_
import jasition.cqrs.playAndAppend
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.trade.MatchingResult
import jasition.matching.domain.trade.MatchingResult_2_

enum class TimeInForce(val code: String) {
    GOOD_TILL_CANCEL("GTC") {
        override fun finalise(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                return if (canStayOnBook(aggressor.sizes))
                    transaction.copy(aggregate = transaction.aggregate.addBookEntry(aggressor))
                else
                    transaction
            }
        }

        //TODO: Unit test
        override fun finalise_2_(result: MatchingResult_2_): Transaction_2_<BookId, Books> {
            with(result) {
                with(transaction) {
                    return if (canStayOnBook(aggressor.sizes)) {
                        val eventId = aggregate.lastEventId.next()
                        val addedEvent = EntryAddedToBookEvent(
                            bookId = aggregate.bookId,
                            eventId = eventId,
                            entry = aggressor.withKey(eventId = eventId)
                        )

                        val addedBooks = addedEvent.play_2_(aggregate)
                        append(Transaction_2_<BookId, Books>(aggregate = addedBooks, events = List.of(addedEvent)))
                    } else
                        this

                }
            }
        }

        override fun canStayOnBook(size: EntrySizes): Boolean = size.available > 0
    },

    IMMEDIATE_OR_CANCEL("IOC") {
        //TODO: Unit test
        override fun finalise_2_(result: MatchingResult_2_): Transaction_2_<BookId, Books> {
            with(result) {
                return if (aggressor.sizes.available > 0) {
                    with(transaction) {
                        val cancelledEvent = aggressor.toOrderCancelledByExchangeEvent(
                                eventId = aggregate.lastEventId.next(),
                                bookId = aggregate.bookId

                        )

                        val cancelledBooks = cancelledEvent.play_2_(aggregate)

                        append(Transaction_2_<BookId, Books>(aggregate = cancelledBooks, events = List.of(cancelledEvent)))
                    }
                } else transaction
            }
        }

        override fun finalise(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                return if (aggressor.sizes.available > 0) {
                    with(transaction) {
                        return append(
                            aggressor.toOrderCancelledByExchangeEvent(
                                eventId = aggregate.lastEventId.next(),
                                bookId = aggregate.bookId
                            ) playAndAppend aggregate
                        )
                    }
                } else transaction
            }
        }

        override fun canStayOnBook(size: EntrySizes): Boolean = false
    };

    abstract fun canStayOnBook(size: EntrySizes): Boolean

    /**
     * Finalises a [MatchingResult] and turns it into a [Transaction]. This is the final acceptance check
     * and post-processing of the [MatchingResult]. Depends on the [TimeInForce], the [MatchingResult]
     * may be accepted or rejected. Also post-processing may include adding the entry of remaining size to
     * the book, cancelling the remaining size of the aggressor, or cancelling the whole aggressor or
     * reverting the whole [MatchingResult].
     */
    @Deprecated("Old CQRS semantics")
    abstract fun finalise(result: MatchingResult): Transaction<BookId, Books>

    /**
     * Finalises a [MatchingResult] and turns it into a [Transaction]. This is the final acceptance check
     * and post-processing of the [MatchingResult]. Depends on the [TimeInForce], the [MatchingResult]
     * may be accepted or rejected. Also post-processing may include adding the entry of remaining size to
     * the book, cancelling the remaining size of the aggressor, or cancelling the whole aggressor or
     * reverting the whole [MatchingResult].
     */
    abstract fun finalise_2_(result: MatchingResult_2_): Transaction_2_<BookId, Books>
}