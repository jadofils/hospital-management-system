package hospital.management.backend.exceptions;

/**
 * Thrown when a lookup by ID or unique key returns no result.
 * Carries the resource type and the ID that was searched so callers
 * can build a clear user-facing message without string formatting at every call site.
 */
public class ResourceNotFoundException extends AppException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " not found: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId   = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public String getResourceId()   { return resourceId; }
}