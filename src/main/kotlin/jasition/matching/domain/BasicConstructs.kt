package jasition.matching.domain

import java.lang.IllegalArgumentException

interface Command

interface Query

interface Event

interface Aggregate

data class EventId(val value: Long) : Comparable<EventId> {
    init {
        if (value < 0) throw IllegalArgumentException("Event ID must be non-negative. value=$value")
    }

    fun next(): EventId = EventId(nextSequenceNumber(value))

    private fun nextSequenceNumber(current: Long): Long =
        if (current == Long.MAX_VALUE) 0 else current + 1

    override fun compareTo(other: EventId): Int =
        if (value == Long.MAX_VALUE && other.value == 0L) -1 else value.compareTo(other.value)
}