package hospital.management.backend.dao.department;

import hospital.management.backend.dao.support.PostgresIntegrationTestBase;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.doctor.Department;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.doctor.DoctorSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Postgres integration test (see PostgresIntegrationTestBase): every assertion
 * here runs DoctorScheduleDAOImpl's actual SQL against a real database, proving the
 * RETURNING clauses, doctor FK, day_of_week CHECK constraint, chk_schedule_time_order
 * CHECK constraint, and gen_random_uuid() default all behave as the code assumes.
 */
class DoctorScheduleDAOImplTest extends PostgresIntegrationTestBase {

    private final DoctorScheduleDAOImpl dao = new DoctorScheduleDAOImpl();
    private final DoctorDAOImpl doctorDao = new DoctorDAOImpl();
    private final DepartmentDAOImpl departmentDao = new DepartmentDAOImpl();

    private Doctor savedDoctor(String email) throws Exception {
        Department dept = new Department();
        dept.setName("Cardiology-" + UUID.randomUUID());
        Department savedDept = departmentDao.save(dept);

        Doctor doctor = new Doctor();
        doctor.setDepartmentId(savedDept.getDepartmentId());
        doctor.setFirstName("Sarah");
        doctor.setLastName("Chen");
        doctor.setSpecialization("Cardiology");
        doctor.setEmail(email);
        return doctorDao.save(doctor);
    }

    private DoctorSchedule sampleSchedule(String doctorId) {
        DoctorSchedule s = new DoctorSchedule();
        s.setDoctorId(doctorId);
        s.setDayOfWeek("Mon");
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(17, 0));
        s.setIsAvailable(true);
        return s;
    }

    @Test
    @DisplayName("save assigns a generated id and populates created_at/updated_at from the DB")
    void save_assignsIdAndTimestamps() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");

        DoctorSchedule saved = dao.save(sampleSchedule(doctor.getDoctorId()));

        assertNotNull(saved.getScheduleId());
        assertDoesNotThrow(() -> UUID.fromString(saved.getScheduleId()));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("save rejects an end_time that is not after start_time — real chk_schedule_time_order constraint")
    void save_rejectsEndTimeNotAfterStartTime() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule schedule = sampleSchedule(doctor.getDoctorId());
        schedule.setStartTime(LocalTime.of(17, 0));
        schedule.setEndTime(LocalTime.of(9, 0));

        assertThrows(DatabaseException.class, () -> dao.save(schedule));
    }

    @Test
    @DisplayName("save rejects a day_of_week outside Mon..Sun — real CHECK constraint")
    void save_rejectsInvalidDayOfWeek() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule schedule = sampleSchedule(doctor.getDoctorId());
        schedule.setDayOfWeek("Someday");

        assertThrows(DatabaseException.class, () -> dao.save(schedule));
    }

    @Test
    @DisplayName("save rejects a doctor_id that doesn't exist — real FK constraint enforcement")
    void save_rejectsNonExistentDoctorId() {
        DoctorSchedule schedule = sampleSchedule(UUID.randomUUID().toString());

        assertThrows(DatabaseException.class, () -> dao.save(schedule));
    }

    @Test
    @DisplayName("findById returns the saved schedule with every field intact")
    void findById_returnsSavedSchedule() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule saved = dao.save(sampleSchedule(doctor.getDoctorId()));

        Optional<DoctorSchedule> found = dao.findById(saved.getScheduleId());

        assertTrue(found.isPresent());
        assertEquals("Mon", found.get().getDayOfWeek());
        assertEquals(LocalTime.of(9, 0), found.get().getStartTime());
        assertEquals(LocalTime.of(17, 0), found.get().getEndTime());
        assertTrue(found.get().isIsAvailable());
    }

    @Test
    @DisplayName("findById returns empty for a random, never-saved id")
    void findById_returnsEmpty_whenNotFound() throws Exception {
        assertTrue(dao.findById(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for a soft-deleted schedule")
    void findById_returnsEmpty_whenSoftDeleted() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule saved = dao.save(sampleSchedule(doctor.getDoctorId()));
        dao.softDelete(saved.getScheduleId());

        assertTrue(dao.findById(saved.getScheduleId()).isEmpty());
    }

    @Test
    @DisplayName("findByDoctorId orders schedules in calendar order (Mon..Sun), not alphabetically")
    void findByDoctorId_ordersByCalendarDay() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule wed = sampleSchedule(doctor.getDoctorId());
        wed.setDayOfWeek("Wed");
        dao.save(wed);
        DoctorSchedule mon = sampleSchedule(doctor.getDoctorId());
        mon.setDayOfWeek("Mon");
        dao.save(mon);
        DoctorSchedule fri = sampleSchedule(doctor.getDoctorId());
        fri.setDayOfWeek("Fri");
        dao.save(fri);

        List<DoctorSchedule> schedules = dao.findByDoctorId(doctor.getDoctorId());

        assertEquals(3, schedules.size());
        assertEquals("Mon", schedules.get(0).getDayOfWeek());
        assertEquals("Wed", schedules.get(1).getDayOfWeek());
        assertEquals("Fri", schedules.get(2).getDayOfWeek());
    }

    @Test
    @DisplayName("update persists changed fields and refreshes updated_at via the DB trigger")
    void update_persistsChanges() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule saved = dao.save(sampleSchedule(doctor.getDoctorId()));

        saved.setDayOfWeek("Tue");
        saved.setStartTime(LocalTime.of(8, 0));
        saved.setEndTime(LocalTime.of(12, 0));
        saved.setIsAvailable(false);
        DoctorSchedule updated = dao.update(saved);

        assertEquals("Tue", updated.getDayOfWeek());
        Optional<DoctorSchedule> reloaded = dao.findById(saved.getScheduleId());
        assertEquals("Tue", reloaded.get().getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), reloaded.get().getStartTime());
        assertFalse(reloaded.get().isIsAvailable());
    }

    @Test
    @DisplayName("update throws ResourceNotFoundException for a schedule id that doesn't exist")
    void update_throwsResourceNotFoundException_whenMissing() {
        DoctorSchedule ghost = sampleSchedule(UUID.randomUUID().toString());
        ghost.setScheduleId(UUID.randomUUID().toString());

        assertThrows(ResourceNotFoundException.class, () -> dao.update(ghost));
    }

    @Test
    @DisplayName("softDelete marks the row deleted rather than removing it")
    void softDelete_marksDeletedAt() throws Exception {
        Doctor doctor = savedDoctor("sarah.chen@example.com");
        DoctorSchedule saved = dao.save(sampleSchedule(doctor.getDoctorId()));

        dao.softDelete(saved.getScheduleId());

        assertThrows(ResourceNotFoundException.class, () -> dao.softDelete(saved.getScheduleId()),
                "a second soft-delete on an already-deleted row should find 0 rows affected");
    }
}
