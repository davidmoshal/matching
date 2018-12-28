package jasition.matching.domain.book.entry

enum class TimeInForce {
    GOOD_TILL_CANCEL {
        override fun canStayOnBook(size: EntrySizes): Boolean = size.available > 0
    };

    abstract fun canStayOnBook(size: EntrySizes): Boolean
}