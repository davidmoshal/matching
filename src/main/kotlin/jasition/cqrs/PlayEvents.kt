package jasition.cqrs

import io.vavr.collection.Seq

fun <K, A : Aggregate<K>> play(initial: A, events: Seq<Event<K, A>>): A =
    events.fold(initial) { aggregate, event -> event.play(aggregate).aggregate }