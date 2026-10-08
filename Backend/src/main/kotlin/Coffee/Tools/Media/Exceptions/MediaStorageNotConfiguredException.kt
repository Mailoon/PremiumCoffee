package Coffee.Tools.Media.Exceptions

class MediaStorageNotConfiguredException(provider: String) :
    RuntimeException("Storage provider '$provider' is not configured")
