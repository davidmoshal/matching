package jasition.cqrs

/**
 * [Aggregate] is the aggregation of domain entities, while the Aggregate Root is the contact point where the service
 * should reach out.
 */
interface Aggregate<KEY> {
    /**
     * Returns the key which identifies the [Aggregate]
     */
    fun aggregateId(): KEY
}