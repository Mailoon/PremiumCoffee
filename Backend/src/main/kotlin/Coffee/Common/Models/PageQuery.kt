package Coffee.Common.Models

data class PageQuery(
    val page: Int,
    val size: Int
) {
    companion object {
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100
    }
}
