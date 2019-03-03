package jasition.cqrs

import io.vavr.collection.Seq

interface Event<K, A : Aggregate<K>> {
    fun aggregateId(): K
    fun eventId(): EventId
    fun play(aggregate: A): A
}

data class EventId(val value: Long) : Comparable<EventId> {
    init {
        if (value < 0) throw IllegalArgumentException("Event ID must be non-negative. value=$value")
    }

    operator fun inc(): EventId = EventId(if (value == Long.MAX_VALUE) 0 else value + 1)

    fun isNextOf(other: EventId): Boolean =
        if (value == 0L && other.value == Long.MAX_VALUE) true
        else (value == other.value + 1)

    override fun compareTo(other: EventId): Int =
        if (value == Long.MAX_VALUE && other.value == 0L) -1
        else if (other.value == Long.MAX_VALUE && value == 0L) 1
        else value.compareTo(other.value)
}

infix fun <K, A : Aggregate<K>> Seq<Event<K, A>>.play(initial: A): A =
    fold(initial) { aggregate, event -> event.play(aggregate) }