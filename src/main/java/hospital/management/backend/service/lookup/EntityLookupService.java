package hospital.management.backend.service.lookup;

import hospital.management.backend.dao.auth.UserDAOImpl;
import hospital.management.backend.dao.clinical.AppointmentDAOImpl;
import hospital.management.backend.dao.department.DepartmentDAOImpl;
import hospital.management.backend.dao.department.DoctorDAOImpl;
import hospital.management.backend.dao.patient.PatientDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicalInventoryDAOImpl;
import hospital.management.backend.dao.pharmacy.MedicationDAOImpl;
import hospital.management.backend.dto.auth.UserDTO;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import hospital.management.backend.dto.doctor.DoctorDTO;
import hospital.management.backend.dto.patient.PatientDTO;
import hospital.management.backend.dto.pharmacy.MedicationDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.service.auth.UserServiceImpl;
import hospital.management.backend.service.clinical.AppointmentServiceImpl;
import hospital.management.backend.service.department.DepartmentServiceImpl;
import hospital.management.backend.service.department.DoctorServiceImpl;
import hospital.management.backend.service.patient.PatientServiceImpl;
import hospital.management.backend.service.pharmacy.PharmacyServiceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic "find any record by its id" lookup, keyed by entity type (mirrors
 * the domain prefixes already used in {@link hospital.management.backend.cache.CacheKey}).
 * Routes through the existing per-domain Service layer rather than DAOs directly,
 * so every lookup transparently benefits from each Service's existing CacheService
 * caching instead of re-querying the database.
 *
 * There is no shared BaseDAO/CrudDAO interface across domains — every domain
 * independently declares its own findById — so this is a registry of per-type
 * lookup/label functions rather than a single generically-typed implementation.
 */
public class EntityLookupService {

    public static final String PATIENT = "patient";
    public static final String DOCTOR = "doctor";
    public static final String DEPARTMENT = "department";
    public static final String APPOINTMENT = "appointment";
    public static final String USER = "user";
    public static final String MEDICATION = "medication";

    @FunctionalInterface
    private interface Lookup {
        Object find(String id) throws Exception;
    }

    @FunctionalInterface
    private interface Labeler {
        String label(Object entity) throws Exception;
    }

    private final Map<String, Lookup> lookups = new HashMap<>();
    private final Map<String, Labeler> labelers = new HashMap<>();

    private final PatientServiceImpl patientService = new PatientServiceImpl(new PatientDAOImpl());
    private final DepartmentServiceImpl departmentService = new DepartmentServiceImpl(new DepartmentDAOImpl());
    private final DoctorServiceImpl doctorService = new DoctorServiceImpl(new DoctorDAOImpl(), new DepartmentDAOImpl());
    private final UserServiceImpl userService = new UserServiceImpl(new UserDAOImpl());
    private final PharmacyServiceImpl pharmacyService = new PharmacyServiceImpl(new MedicationDAOImpl(), new MedicalInventoryDAOImpl());
    private final AppointmentServiceImpl appointmentService = new AppointmentServiceImpl(
        new AppointmentDAOImpl(), new PatientDAOImpl(), new DoctorDAOImpl());

    public EntityLookupService() {
        register(PATIENT, patientService::findById, o -> ((PatientDTO) o).getFullName());
        register(DOCTOR, doctorService::findById, o -> ((DoctorDTO) o).getFullName());
        register(DEPARTMENT, departmentService::findById, o -> ((DepartmentDTO) o).getName());
        register(USER, userService::findById, o -> ((UserDTO) o).getUsername());
        register(MEDICATION, pharmacyService::findMedicationById, o -> ((MedicationDTO) o).getName());
        register(APPOINTMENT, appointmentService::findById, o -> buildAppointmentLabel((AppointmentDTO) o));
    }

    private void register(String entityType, Lookup lookup, Labeler labeler) {
        lookups.put(entityType, lookup);
        labelers.put(entityType, labeler);
    }

    /** Returns the record for the given entity type + id, or empty if it doesn't exist. */
    public Optional<Object> findById(String entityType, String id) throws Exception {
        Lookup lookup = lookups.get(entityType);
        if (lookup == null) {
            throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }
        try {
            return Optional.ofNullable(lookup.find(id));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    /** Human-readable label for the given entity type + id, for dropdown options and detail views. */
    public String getDisplayLabel(String entityType, String id) throws Exception {
        if (id == null || id.isBlank()) return "—";
        Optional<Object> found = findById(entityType, id);
        if (found.isEmpty()) return "Unknown";
        return labelers.get(entityType).label(found.get());
    }

    public String patientLabel(String id) throws Exception { return getDisplayLabel(PATIENT, id); }
    public String doctorLabel(String id) throws Exception { return getDisplayLabel(DOCTOR, id); }
    public String departmentLabel(String id) throws Exception { return getDisplayLabel(DEPARTMENT, id); }
    public String appointmentLabel(String id) throws Exception { return getDisplayLabel(APPOINTMENT, id); }
    public String userLabel(String id) throws Exception { return getDisplayLabel(USER, id); }
    public String medicationLabel(String id) throws Exception { return getDisplayLabel(MEDICATION, id); }

    private String buildAppointmentLabel(AppointmentDTO appointment) throws Exception {
        String patientName = getDisplayLabel(PATIENT, appointment.getPatientId());
        String doctorName = getDisplayLabel(DOCTOR, appointment.getDoctorId());
        return appointment.getAppointmentDate() + " — " + patientName + " with " + doctorName;
    }
}
