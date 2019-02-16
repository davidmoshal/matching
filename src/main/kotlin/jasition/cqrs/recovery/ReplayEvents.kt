package jasition.cqrs.recovery

import io.vavr.collection.Seq
import jasition.cqrs.Aggregate
import jasition.cqrs.Event
import jasition.matching.domain.book.event.BooksCreatedEvent

fun <K, A : Aggregate<K>> replay(initial: A, events: Seq<Event<K, A>>): A {
    // Deliberately not use loop recursion to prevent potential stack overflow if the number of messages is large
    var latest = initial
    events.filter { it.isPrimary() }
        .forEach { latest = it.play(latest).aggregate }
    return latest
}

//TODO: Unit test
fun <K, A : Aggregate<K>> replay_2_(initial: A, events: Seq<Event<K, A>>): A = events.fold(initial) { state, event ->
    //TODO: Re-visit and re-consider if aggregate creation should be moved to the CRUD semantics
    if (event is BooksCreatedEvent) {
        state
    } else {
        event.play_2_(state)
    }
}