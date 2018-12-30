package jasition.cqrs


internal class TestAggregate(val aggregateId: Int = 1,
                             val value: String = "test") : Aggregate<Int> {
    override fun aggregateId(): Int = aggregateId
}

internal data class TestEvent(
    val aggregateId: Int = 1,
    val eventId: EventId
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = false
    override fun play(aggregate: TestAggregate): Transaction<Int, TestAggregate> =
        Transaction(aggregate)
}

internal data class TestPrimaryEvent(
    val aggregateId: Int = 1,
    val eventId: EventId
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = true
    override fun play(aggregate: TestAggregate): Transaction<Int, TestAggregate> =
        Transaction(aggregate)
}