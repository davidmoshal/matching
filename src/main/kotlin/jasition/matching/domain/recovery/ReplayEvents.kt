package jasition.matching.domain.recovery

import io.vavr.collection.Seq
import jasition.matching.domain.Aggregate
import jasition.matching.domain.Event

fun <K, A : Aggregate> replay(initial: A, events: Seq<Event<K, A>>): A {
    // Deliberately not use loop recursion to prevent potential stack overflow if the number of messages is large
    var latest = initial
    events.filter { it.isPrimary() }
        .forEach { latest = it.play(latest).aggregate }
    return latest
}