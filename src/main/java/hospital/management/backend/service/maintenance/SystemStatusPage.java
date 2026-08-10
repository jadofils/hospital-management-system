package hospital.management.backend.service.maintenance;

/**
 * Which visual variant the system-status page renders for a blocked user.
 * Purely presentational — {@link MaintenanceGate}'s block/allow decision is
 * independent of which variant is configured.
 */
public enum SystemStatusPage { MAINTENANCE, ERROR_502, ERROR_503 }
