package hospital.management.backend.dao.finance;

import hospital.management.backend.dao.finance.interfaces.InvoiceDAO;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.util.List;
import java.util.Optional;

public class InvoiceDAOImpl implements InvoiceDAO {

    @Override
    public Invoice save(Invoice invoice) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Invoice> findById(String invoiceId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Invoice> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Invoice> findByPatientId(String patientId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public PageResult<Invoice> findAll(PageRequest request) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Invoice updatePaymentStatus(String invoiceId, String status) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String invoiceId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}