package jasition.cqrs.recovery

import io.vavr.collection.Seq
import jasition.cqrs.Aggregate
import jasition.cqrs.Event

fun <K, A : Aggregate<K>> replay(initial: A, events: Seq<Event<K, A>>): A {
    // Deliberately not use loop recursion to prevent potential stack overflow if the number of messages is large
    var latest = initial
    events.filter { it.isPrimary() }
        .forEach { latest = it.play(latest).aggregate }
    return latest
}