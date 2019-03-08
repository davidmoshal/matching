package jasition.cqrs

import arrow.core.Either
import io.vavr.collection.List


internal class TestAggregate(
    val aggregateId: Int = 1,
    val value: String = "test"
) : Aggregate<Int> {
    override fun aggregateId(): Int = aggregateId
}

internal data class TestEvent(
    val aggregateId: Int = 1,
    val eventId: EventId
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun play(aggregate: TestAggregate): TestAggregate = aggregate
}

internal class TestCommand : Command<Int, TestAggregate> {
    override fun execute(aggregate: TestAggregate?): Either<Exception, Transaction<Int, TestAggregate>> {
        return Either.right(Transaction(aggregate = aggregate!!, events = List.empty()))
    }

}

internal data class TestRejectedEvent  (
    val aggregateId : Int,
    val eventId : EventId,
    val reason : String
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId

    override fun eventId(): EventId = eventId

    override fun play(aggregate: TestAggregate): TestAggregate = aggregate

}