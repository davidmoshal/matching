package jasition.cqrs

import arrow.core.Either
import io.vavr.collection.List
import java.util.function.BiConsumer

@Deprecated("Old CQRS semantics")
data class Transaction<K, A : Aggregate<K>>(
    val aggregate: A,
    val events: List<Event<K, A>> = List.empty(),
    val updateFunction: BiConsumer<Repository<K, A>, A> = BiConsumer { repo, updated -> repo.createOrUpdate(updated) }
) {

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
}

@Deprecated("Old CQRS semantics")
infix fun <K, A : Aggregate<K>> Transaction<K, A>.thenPlay(
    event: Event<K, A>
): Transaction<K, A> {
    val other = event.play(aggregate)
    return Transaction(
        aggregate = other.aggregate,
        events = events.append(event).appendAll(other.events)
    )
}

//TODO: Unit test
infix fun <K, A : Aggregate<K>> Transaction<K, A>.thenPlay_2_(event: Event<K, A>): Transaction<K, A> =
    Transaction(aggregate = event.play_2_(aggregate), events = events.append(event))

/**
 * Transaction is a unit of work that affects a given aggregate. The source of changes is the list of [events]. From
 * the [events] the new [aggregate] is derived. It is important to note that no direct change must be made to the
 * [aggregate] other than by the [play] function of an [Event]. There is also an [updateFunction] in a Transaction to
 * specify how the final [aggregate] updates the given [Repository], which provides standard CRUD operations.
 */
data class Transaction_2_<K, A : Aggregate<K>>(
    val aggregate: A,
    val events: List<Event<K, A>>,
    val updateFunction: BiConsumer<A, Repository<K, A>> = BiConsumer { final, repo -> repo.createOrUpdate(final) }
) {
    /**
     * Appends the given Transaction after this Transaction, implying that the given Transaction happens after this
     * Transaction. As a result, the [events] in the given Transaction are appended after the current list of [events].
     * Moreover the [updateFunction] of the given Transaction will overwrite the current [updateFunction].
     */
    //TODO: Unit test
    fun append(other: Transaction_2_<K, A>): Transaction_2_<K, A> =
        Transaction_2_(
            aggregate = other.aggregate,
            events = events.appendAll(other.events),
            updateFunction = other.updateFunction
        )

    @Deprecated("Remove after refactor is done")
    fun append(other: Transaction<K, A>): Transaction_2_<K, A> =
        Transaction_2_(
            aggregate = other.aggregate,
            events = events.appendAll(other.events)
        )
}

//TODO: Unit test
infix fun <K, A : Aggregate<K>> Either<Exception, Transaction_2_<K, A>>.commitOrThrow(
    repository: Repository<K, A>
): Transaction_2_<K, A> = fold(
    ifLeft = { throw it },
    ifRight = {
        it.updateFunction.accept(it.aggregate, repository)
        it
    }
)