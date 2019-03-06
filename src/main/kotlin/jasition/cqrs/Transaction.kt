package jasition.cqrs

import arrow.core.Either
import io.vavr.collection.List

/**
 * Transaction is a unit of work that affects a given aggregate. The source of changes is the list of [events]. From
 * the [events] the new [aggregate] is derived. It is important to note that no direct change must be made to the
 * [aggregate] other than by the [play] function of an [Event]. There is also an [updateFunction] in a Transaction to
 * specify how the final [aggregate] updates the given [Repository], which provides standard CRUD operations.
 */
data class Transaction<KEY, AGG : Aggregate<KEY>>(
    val aggregate: AGG,
    val events: List<Event<KEY, AGG>>,
    val updateFunction: RepositoryUpdateFunction = CreateOrUpdateFunction
)

/**
 * Appends the given Transaction after this Transaction, implying that the given Transaction happens after this
 * Transaction. As a result, the [events] in the given Transaction are   appended after the current list of [events].
 * Moreover the [updateFunction] of the given Transaction will overwrite the current [updateFunction].
 */
infix fun <KEY, AGG : Aggregate<KEY>> Transaction<KEY, AGG>.append(other: Transaction<KEY, AGG>): Transaction<KEY, AGG> =
    Transaction(
        aggregate = other.aggregate,
        events = events.appendAll(other.events),
        updateFunction = other.updateFunction
    )

infix fun <KEY, AGG : Aggregate<KEY>> Either<Exception, Transaction<KEY, AGG>>.commitOrThrow(
    repository: Repository<KEY, AGG>
): Transaction<KEY, AGG> = fold(
    ifLeft = { throw it },
    ifRight = {
        it.updateFunction.update(it.aggregate, repository)
        it
    }
)