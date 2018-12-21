package jasition.matching.domain.book.entry

enum class TimeInForce {
    GOOD_TILL_CANCEL {
        override fun canStayOnBook(size: EntryQuantity): Boolean = size.availableSize > 0
    };

    abstract fun canStayOnBook(size : EntryQuantity) : Boolean
}