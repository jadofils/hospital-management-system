package hospital.management.backend.service.clinical;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.clinical.interfaces.AppointmentDAO;
import hospital.management.backend.dao.department.interfaces.DoctorDAO;
import hospital.management.backend.dao.patient.interfaces.PatientDAO;
import hospital.management.backend.dto.clinical.AppointmentDTO;
import hospital.management.backend.dto.clinical.AppointmentSummaryDTO;
import hospital.management.backend.dto.clinical.CreateAppointmentDTO;
import hospital.management.backend.dto.clinical.UpdateAppointmentDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.clinical.AppointmentMapper;
import hospital.management.backend.model.doctor.Doctor;
import hospital.management.backend.model.enums.AppointmentStatus;
import hospital.management.backend.model.patient.Appointment;
import hospital.management.backend.model.patient.Patient;
import hospital.management.backend.service.clinical.interfaces.AppointmentService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentDAO appointmentDAO;
    private final PatientDAO     patientDAO;
    private final DoctorDAO      doctorDAO;

    public AppointmentServiceImpl(AppointmentDAO appointmentDAO, PatientDAO patientDAO, DoctorDAO doctorDAO) {
        this.appointmentDAO = appointmentDAO;
        this.patientDAO     = patientDAO;
        this.doctorDAO      = doctorDAO;
    }

    @Override
    public AppointmentDTO book(CreateAppointmentDTO dto) throws Exception {
        ValidatorUtils.requireNonBlank(dto.getPatientId(), "patientId");
        ValidatorUtils.requireNonBlank(dto.getDoctorId(), "doctorId");
        if (dto.getAppointmentDate() == null) {
            throw new ValidationException("appointmentDate", "appointmentDate must not be null.");
        }

        // Single INSERT is already atomic — no TransactionManager needed here.
        CacheService.evictByPattern(CacheKey.ALL_APPOINTMENTS);
        Appointment saved = appointmentDAO.save(AppointmentMapper.toEntity(dto));
        EventBus.publish(AppEventType.APPOINTMENT_BOOKED, saved.getAppointmentId());
        return AppointmentMapper.toDTO(saved);
    }

    @Override
    public AppointmentDTO findById(String appointmentId) throws Exception {
        Optional<AppointmentDTO> cached = CacheService.get(CacheKey.appointment(appointmentId), AppointmentDTO.class);
        if (cached.isPresent()) return cached.get();

        Appointment appointment = appointmentDAO.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        AppointmentDTO dto = AppointmentMapper.toDTO(appointment);
        CacheService.set(CacheKey.appointment(appointmentId), dto, CacheDomain.APPOINTMENT);
        return dto;
    }

    @Override
    public PageResult<AppointmentSummaryDTO> findAll(PageRequest request) throws Exception {
        PageResult<Appointment> page = appointmentDAO.findAll(request);
        return page.map(a -> AppointmentMapper.toSummaryDTO(
                a, resolvePatientName(a.getPatientId()), resolveDoctorName(a.getDoctorId())));
    }

    @Override
    public List<AppointmentDTO> findByPatient(String patientId) throws Exception {
        Optional<List<AppointmentDTO>> cached = CacheService.get(
            CacheKey.appointmentsByPatient(patientId),
            new TypeReference<List<AppointmentDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<AppointmentDTO> dtos = new ArrayList<>();
        for (Appointment a : appointmentDAO.findByPatientId(patientId)) dtos.add(AppointmentMapper.toDTO(a));
        CacheService.set(CacheKey.appointmentsByPatient(patientId), dtos, CacheDomain.APPOINTMENT);
        return dtos;
    }

    @Override
    public List<AppointmentDTO> findByDoctor(String doctorId) throws Exception {
        Optional<List<AppointmentDTO>> cached = CacheService.get(
            CacheKey.appointmentsByDoctor(doctorId),
            new TypeReference<List<AppointmentDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<AppointmentDTO> dtos = new ArrayList<>();
        for (Appointment a : appointmentDAO.findByDoctorId(doctorId)) dtos.add(AppointmentMapper.toDTO(a));
        CacheService.set(CacheKey.appointmentsByDoctor(doctorId), dtos, CacheDomain.APPOINTMENT);
        return dtos;
    }

    @Override
    public AppointmentDTO update(UpdateAppointmentDTO dto) throws Exception {
        ValidatorUtils.requireNonBlank(dto.getAppointmentId(), "appointmentId");

        Appointment appointment = appointmentDAO.findById(dto.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", dto.getAppointmentId()));

        if (dto.getAppointmentDate() != null) appointment.setAppointmentDate(dto.getAppointmentDate());
        if (dto.getStatus() != null) {
            try {
                appointment.setStatus(AppointmentStatus.fromDbValue(dto.getStatus()).getDbValue());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("status", e.getMessage());
            }
        }
        if (dto.getReason() != null) appointment.setReason(dto.getReason());

        CacheService.evict(CacheKey.appointment(dto.getAppointmentId()));
        CacheService.evict(CacheKey.appointmentsByPatient(appointment.getPatientId()));
        CacheService.evict(CacheKey.appointmentsByDoctor(appointment.getDoctorId()));
        CacheService.evictByPattern(CacheKey.ALL_APPOINTMENTS);

        Appointment saved = appointmentDAO.update(appointment);
        EventBus.publish(AppEventType.APPOINTMENT_UPDATED, saved.getAppointmentId());
        return AppointmentMapper.toDTO(saved);
    }

    @Override
    public void cancel(String appointmentId) throws Exception {
        Appointment appointment = appointmentDAO.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        CacheService.evict(CacheKey.appointment(appointmentId));
        CacheService.evict(CacheKey.appointmentsByPatient(appointment.getPatientId()));
        CacheService.evict(CacheKey.appointmentsByDoctor(appointment.getDoctorId()));
        CacheService.evictByPattern(CacheKey.ALL_APPOINTMENTS);

        appointmentDAO.softDelete(appointmentId);
        EventBus.publish(AppEventType.APPOINTMENT_CANCELLED, appointmentId);
    }

    // ── Name resolution for AppointmentSummaryDTO ─────────────────────────────
    //
    // Patient/Doctor DAOs are owned by other domain passes; if they are not yet
    // implemented (UnsupportedOperationException) or the referenced row is
    // missing, fall back to showing the raw id rather than failing the whole
    // page load.

    private String resolvePatientName(String patientId) {
        try {
            Optional<Patient> patient = patientDAO.findById(patientId);
            return patient.map(Patient::getFullName).orElse(patientId);
        } catch (Exception e) {
            return patientId;
        }
    }

    private String resolveDoctorName(String doctorId) {
        try {
            Optional<Doctor> doctor = doctorDAO.findById(doctorId);
            return doctor.map(Doctor::getFullName).orElse(doctorId);
        } catch (Exception e) {
            return doctorId;
        }
    }
}
