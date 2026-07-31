package hospital.management.backend.service.department;

import hospital.management.backend.dao.department.interfaces.ReferralDAO;
import hospital.management.backend.dto.doctor.CreateReferralDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.service.department.interfaces.ReferralService;

import java.util.List;

public class ReferralServiceImpl implements ReferralService {

    private final ReferralDAO referralDAO;

    public ReferralServiceImpl(ReferralDAO referralDAO) {
        this.referralDAO = referralDAO;
    }

    @Override
    public ReferralDTO create(CreateReferralDTO dto) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ReferralDTO findById(String referralId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<ReferralDTO> findByAppointment(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ReferralDTO updateStatus(String referralId, String status) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String referralId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}