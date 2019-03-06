package jasition.cqrs

interface Aggregate<KEY> {
    fun aggregateId(): KEY
}