package hospital.management.backend.service.department;

import com.fasterxml.jackson.core.type.TypeReference;
import hospital.management.backend.cache.CacheDomain;
import hospital.management.backend.cache.CacheKey;
import hospital.management.backend.cache.CacheService;
import hospital.management.backend.dao.department.interfaces.DoctorScheduleDAO;
import hospital.management.backend.dto.doctor.CreateDoctorScheduleDTO;
import hospital.management.backend.dto.doctor.DoctorScheduleDTO;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.exceptions.ValidationException;
import hospital.management.backend.mapper.doctor.DoctorScheduleMapper;
import hospital.management.backend.model.doctor.DoctorSchedule;
import hospital.management.backend.service.department.interfaces.DoctorScheduleService;
import hospital.management.backend.utils.ValidatorUtils;
import hospital.management.backend.utils.listeners.AppEventType;
import hospital.management.backend.utils.listeners.EventBus;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final DoctorScheduleDAO scheduleDAO;

    public DoctorScheduleServiceImpl(DoctorScheduleDAO scheduleDAO) {
        this.scheduleDAO = scheduleDAO;
    }

    @Override
    public DoctorScheduleDTO create(CreateDoctorScheduleDTO dto) throws Exception {
        String doctorId = ValidatorUtils.requireNonBlank(dto.getDoctorId(), "doctorId");
        ValidatorUtils.requireNonBlank(dto.getDayOfWeek(), "dayOfWeek");
        validateTimeOrder(dto.getStartTime(), dto.getEndTime());

        CacheService.evict(CacheKey.doctorSchedule(doctorId));
        DoctorSchedule saved = scheduleDAO.save(DoctorScheduleMapper.toEntity(dto));
        EventBus.publish(AppEventType.DOCTOR_SCHEDULE_UPDATED, saved.getDoctorId());
        return DoctorScheduleMapper.toDTO(saved);
    }

    @Override
    public List<DoctorScheduleDTO> findByDoctor(String doctorId) throws Exception {
        Optional<List<DoctorScheduleDTO>> cached = CacheService.get(
                CacheKey.doctorSchedule(doctorId),
                new TypeReference<List<DoctorScheduleDTO>>() {});
        if (cached.isPresent()) return cached.get();

        List<DoctorScheduleDTO> dtos = new ArrayList<>();
        for (DoctorSchedule schedule : scheduleDAO.findByDoctorId(doctorId)) {
            dtos.add(DoctorScheduleMapper.toDTO(schedule));
        }
        CacheService.set(CacheKey.doctorSchedule(doctorId), dtos, CacheDomain.DOCTOR);
        return dtos;
    }

    @Override
    public DoctorScheduleDTO update(String scheduleId, CreateDoctorScheduleDTO dto) throws Exception {
        DoctorSchedule schedule = scheduleDAO.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorSchedule", scheduleId));

        String dayOfWeek = dto.getDayOfWeek() != null ? dto.getDayOfWeek() : schedule.getDayOfWeek();
        LocalTime startTime = dto.getStartTime() != null ? dto.getStartTime() : schedule.getStartTime();
        LocalTime endTime   = dto.getEndTime() != null ? dto.getEndTime() : schedule.getEndTime();
        validateTimeOrder(startTime, endTime);

        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setIsAvailable(dto.getIsAvailable() != null ? dto.getIsAvailable() : schedule.isIsAvailable());

        CacheService.evict(CacheKey.doctorSchedule(schedule.getDoctorId()));
        DoctorSchedule saved = scheduleDAO.update(schedule);
        EventBus.publish(AppEventType.DOCTOR_SCHEDULE_UPDATED, saved.getDoctorId());
        return DoctorScheduleMapper.toDTO(saved);
    }

    @Override
    public void delete(String scheduleId) throws Exception {
        DoctorSchedule schedule = scheduleDAO.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("DoctorSchedule", scheduleId));

        CacheService.evict(CacheKey.doctorSchedule(schedule.getDoctorId()));
        scheduleDAO.softDelete(scheduleId);
        EventBus.publish(AppEventType.DOCTOR_SCHEDULE_UPDATED, schedule.getDoctorId());
    }

    /** Mirrors the `chk_schedule_time_order` DB constraint (end_time > start_time). */
    private void validateTimeOrder(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new ValidationException("endTime", "Start time and end time are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("endTime", "End time must be after start time.");
        }
    }
}
