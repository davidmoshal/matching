package jasition.cqrs

import io.vavr.collection.List

data class Transaction<K, A : Aggregate<K>>(val aggregate: A, val events: List<Event<K, A>> = List.empty()) {

    fun append(
        other: Transaction<K, A>
    ): Transaction<K, A> =
        Transaction(
            aggregate = other.aggregate,
            events = events.appendAll(other.events)
        )

    fun append(vararg event: Event<K, A>): Transaction<K, A> = Transaction(
        aggregate = aggregate,
        events = events.appendAll(event.asList())
    )

    fun thenPlay(
        event: Event<K, A>
    ): Transaction<K, A> {
        val other = event.play(aggregate)
        return Transaction(
            aggregate = other.aggregate,
            events = events.append(event).appendAll(other.events)
        )
    }
}