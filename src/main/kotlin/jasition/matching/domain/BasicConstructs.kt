package jasition.matching.domain

import io.vavr.collection.List
import java.util.function.BiFunction

interface Aggregate

interface Command

interface Query

interface Event<K, A : Aggregate> {
    fun aggregateId(): K
    fun eventId(): EventId
    fun isPrimary(): Boolean
    fun play(aggregate: A): Transaction<K, A>
}

data class EventId(val value: Long) : Comparable<EventId> {
    init {
        if (value < 0) throw IllegalArgumentException("Event ID must be non-negative. value=$value")
    }

    fun next(): EventId = EventId(if (value == Long.MAX_VALUE) 0 else value + 1)

    fun isNextOf(other: EventId): Boolean =
        if (value == 0L && other.value == Long.MAX_VALUE) true else (value == other.value + 1)

    override fun compareTo(other: EventId): Int =
        if (value == Long.MAX_VALUE && other.value == 0L) -1
        else if (other.value == Long.MAX_VALUE && value == 0L) 1
        else value.compareTo(other.value)
}

data class Transaction<K, A : Aggregate>(val aggregate: A, val events: List<Event<K, A>> = List.empty()) {
    fun append(
        other: Transaction<K, A>,
        mergeFunction: BiFunction<A, A, A> = BiFunction { _, right -> right }
    ): Transaction<K, A> =
        Transaction(
            aggregate = mergeFunction.apply(aggregate, other.aggregate),
            events = events.appendAll(other.events)
        )

    fun append(vararg event: Event<K, A>): Transaction<K, A> = Transaction(
        aggregate = aggregate,
        events = events.appendAll(event.asList())
    )
}


