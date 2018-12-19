package jasition.matching.domain

fun nextSequenceNumber(current : Long) : Long {
    return if (current == Long.MAX_VALUE) 0 else current + 1
}