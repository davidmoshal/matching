package jasition.cqrs

import arrow.core.Either
import io.vavr.collection.List

/**
 * Transaction is a unit of work that affects a given aggregate. The source of changes is the list of [events]. From
 * the [events] the new [aggregate] is derived. It is important to note that no direct change must be made to the
 * [aggregate] other than by the [play] function of an [Event]. There is also an [updateFunction] in a Transaction to
 * specify how the final [aggregate] updates the given [Repository], which provides standard CRUD operations.
 */
data class Transaction<K, A : Aggregate<K>>(
    val aggregate: A,
    val events: List<Event<K, A>>,
    val updateFunction: RepositoryUpdateFunction = CreateOrUpdateFunction
)

/**
 * Appends the given Transaction after this Transaction, implying that the given Transaction happens after this
 * Transaction. As a result, the [events] in the given Transaction are   appended after the current list of [events].
 * Moreover the [updateFunction] of the given Transaction will overwrite the current [updateFunction].
 */
infix fun <K, A : Aggregate<K>> Transaction<K, A>.append(other: Transaction<K, A>): Transaction<K, A> =
    Transaction(
        aggregate = other.aggregate,
        events = events.appendAll(other.events),
        updateFunction = other.updateFunction
    )

//TODO: Unit test
infix fun <K, A : Aggregate<K>> Either<Exception, Transaction<K, A>>.commitOrThrow(
    repository: Repository<K, A>
): Transaction<K, A> = fold(
    ifLeft = { throw it },
    ifRight = {
        it.updateFunction.update(it.aggregate, repository)
        it
    }
)