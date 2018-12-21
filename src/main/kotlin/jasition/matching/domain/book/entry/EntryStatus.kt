package jasition.matching.domain.book.entry

enum class EntryStatus {
    NEW, PARTIAL_FILL, FILLED, CANCELLED;

    fun traded(newQuantity: EntryQuantity): EntryStatus = if (newQuantity.availableSize == 0) FILLED else PARTIAL_FILL
}