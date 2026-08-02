package hospital.management.backend.dao.clinical.interfaces;

import hospital.management.backend.model.patient.MedicalRecord;

import java.util.Optional;

public interface MedicalRecordDAO {
    MedicalRecord save(MedicalRecord record) throws Exception;
    Optional<MedicalRecord> findById(String recordId) throws Exception;
    Optional<MedicalRecord> findByAppointmentId(String appointmentId) throws Exception;
    MedicalRecord update(MedicalRecord record) throws Exception;
    void softDelete(String recordId) throws Exception;
}