package hospital.management.pages.utils;

import hospital.management.backend.dto.auth.PermissionDTO;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Formats a role's permissions for display grouped by resource, instead of one long
 *  flat comma-joined string — e.g. "Patients: create, read, update\nAppointments: read". */
public final class PermissionDisplayFormatter {

    private PermissionDisplayFormatter() {}

    public static String groupedByResource(List<PermissionDTO> permissions) {
        if (permissions == null || permissions.isEmpty()) return "None";

        Map<String, List<PermissionDTO>> byResource = permissions.stream()
                .collect(Collectors.groupingBy(PermissionDTO::getResource, TreeMap::new, Collectors.toList()));

        return byResource.entrySet().stream()
                .map(entry -> capitalize(entry.getKey()) + ": " + entry.getValue().stream()
                        .map(PermissionDTO::getAction)
                        .sorted()
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    private static String capitalize(String resource) {
        if (resource == null || resource.isBlank()) return "Unknown";
        String normalized = resource.replace('_', ' ');
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
