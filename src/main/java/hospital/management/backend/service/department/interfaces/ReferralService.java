package hospital.management.backend.service.department.interfaces;

import hospital.management.backend.dto.doctor.CreateReferralDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;

import java.util.List;

public interface ReferralService {
    ReferralDTO create(CreateReferralDTO dto) throws Exception;
    ReferralDTO findById(String referralId) throws Exception;
    List<ReferralDTO> findByAppointment(String appointmentId) throws Exception;
    ReferralDTO updateStatus(String referralId, String status) throws Exception;
    void delete(String referralId) throws Exception;
}