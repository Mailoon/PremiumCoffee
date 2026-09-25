package Coffee.Common.Exceptions

class ResourceNotFoundException(
    val resourceName: String,
    val id: Any
) : RuntimeException("$resourceName with id '$id' not found")
