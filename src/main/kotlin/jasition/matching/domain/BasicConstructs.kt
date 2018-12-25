package jasition.matching.domain

import io.vavr.collection.List
import java.util.function.BiFunction

interface Aggregate

interface Command

interface Query

interface Event<K, A : Aggregate> {
    fun aggregateId(): K
    fun eventId(): EventId
    fun eventType(): EventType
    fun play(aggregate: A): Transaction<K, A>
}

enum class EventType {
    /**
     * A Primary [Event] is a direct response to a [Command].
     */
    PRIMARY,

    /**
     * An [Event] that is generated when playing another [Event]. During recovery stage, Side-effect [Event]s are not
     * re-played because they will be re-generated when re-playing the Primary [Event].
     */
    SIDE_EFFECT
}

data class EventId(val value: Long) : Comparable<EventId> {
    init {
        if (value < 0) throw IllegalArgumentException("Event ID must be non-negative. value=$value")
    }

    fun next(): EventId = EventId(if (value == Long.MAX_VALUE) 0 else value + 1)

    fun isNextOf(other: EventId): Boolean =
        if (value == 0L && other.value == Long.MAX_VALUE) true else (value == other.value + 1)

    override fun compareTo(other: EventId): Int =
        if (value == Long.MAX_VALUE && other.value == 0L) -1 else value.compareTo(other.value)

    operator fun plus(delta: Long) :EventId = EventId(value + delta)

    operator fun minus(delta: Long) :EventId = EventId(value - delta)
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

    fun append(event: Event<K, A>): Transaction<K, A> = Transaction(
        aggregate = aggregate,
        events = events.append(event)
    )
}


