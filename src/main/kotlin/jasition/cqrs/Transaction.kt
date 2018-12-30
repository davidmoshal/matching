package jasition.cqrs

import io.vavr.collection.List
import java.util.function.BiFunction

data class Transaction<K, A : Aggregate<K>>(val aggregate: A, val events: List<Event<K, A>> = List.empty()) {
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