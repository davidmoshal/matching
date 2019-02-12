package jasition.cqrs

import io.vavr.collection.List

interface Event<K, A : Aggregate<K>> {
    fun aggregateId(): K
    fun eventId(): EventId
    fun isPrimary(): Boolean

    @Deprecated("Old CQRS semantics")
    fun play(aggregate: A): Transaction<K, A>

    fun play_2_(aggregate: A): A
}

infix fun <K, A : Aggregate<K>> Event<K, A>.playAndAppend(aggregate: A): Transaction<K, A> {
    val transaction = play(aggregate)
    return transaction.copy(events = List.of(this).appendAll(transaction.events))
}


data class EventId(val value: Long) : Comparable<EventId> {
    init {
        if (value < 0) throw IllegalArgumentException("Event ID must be non-negative. value=$value")
    }

    fun next(): EventId = EventId(if (value == Long.MAX_VALUE) 0 else value + 1)

    fun increment(delta : Int) : EventId = copy(value = value + delta)

    fun isNextOf(other: EventId): Boolean =
        if (value == 0L && other.value == Long.MAX_VALUE) true
        else (value == other.value + 1)

    override fun compareTo(other: EventId): Int =
        if (value == Long.MAX_VALUE && other.value == 0L) -1
        else if (other.value == Long.MAX_VALUE && value == 0L) 1
        else value.compareTo(other.value)
}