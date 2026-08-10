package hospital.management.backend.dao.finance;

import hospital.management.backend.config.db.DBConnection;
import hospital.management.backend.dao.finance.interfaces.InvoiceDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.finance.Invoice;
import hospital.management.backend.utils.filters.QueryBuilder;
import hospital.management.backend.utils.pagination.CursorPagination;
import hospital.management.backend.utils.pagination.PageRequest;
import hospital.management.backend.utils.pagination.PageResult;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC implementation against the `invoices` table (see hospital_schema.sql). */
public class InvoiceDAOImpl implements InvoiceDAO {

    private static final String SELECT_COLUMNS =
        "invoice_id, appointment_id, patient_id, total_amount, payment_status, issued_at, updated_at, deleted_at";

    @Override
    public Invoice save(Invoice invoice) throws Exception {
        UUID id = invoice.getInvoiceId() != null ? UUID.fromString(invoice.getInvoiceId()) : UUID.randomUUID();
        String sql = "INSERT INTO invoices (invoice_id, appointment_id, patient_id, total_amount, payment_status) "
                   + "VALUES (?, ?, ?, ?, ?) RETURNING issued_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, UUID.fromString(invoice.getAppointmentId()));
            ps.setObject(3, UUID.fromString(invoice.getPatientId()));
            ps.setBigDecimal(4, invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
            ps.setString(5, invoice.getPaymentStatus() != null ? invoice.getPaymentStatus() : "unpaid");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    invoice.setInvoiceId(id.toString());
                    invoice.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
                    invoice.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save invoice: " + e.getMessage(), e);
        }
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(String invoiceId) throws Exception {
        return findOneWhere("invoice_id = ?", UUID.fromString(invoiceId));
    }

    @Override
    public Optional<Invoice> findByAppointmentId(String appointmentId) throws Exception {
        return findOneWhere("appointment_id = ?", UUID.fromString(appointmentId));
    }

    private Optional<Invoice> findOneWhere(String predicate, Object param) throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM invoices WHERE " + predicate + " AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up invoice: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Invoice> findByPatientId(String patientId) throws Exception {
        String sql = QueryBuilder.select(SELECT_COLUMNS)
            .from("invoices")
            .where("patient_id = ?")
            .whereActive()
            .orderBy("issued_at", QueryBuilder.SortDir.DESC)
            .build();
        List<Invoice> invoices = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(patientId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) invoices.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list invoices by patient: " + e.getMessage(), e);
        }
        return invoices;
    }

    @Override
    public PageResult<Invoice> findAll(PageRequest request) throws Exception {
        QueryBuilder.SortDir dir = request.getDirection() == PageRequest.SortDirection.DESC
            ? QueryBuilder.SortDir.DESC : QueryBuilder.SortDir.ASC;
        QueryBuilder qb = QueryBuilder.select(SELECT_COLUMNS).from("invoices").whereActive();
        String cursorFrag = CursorPagination.whereClause(request, "issued_at");
        if (!cursorFrag.isBlank()) qb.and(cursorFrag.trim().replaceFirst("(?i)^AND\\s+", ""));
        String sql = qb.orderBy("issued_at", dir).limit(request.getPageSize() + 1).build();
        List<Invoice> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list invoices: " + e.getMessage(), e);
        }
        return CursorPagination.toResult(rows, request, Invoice::getIssuedAt);
    }

    @Override
    public Invoice updatePaymentStatus(String invoiceId, String status) throws Exception {
        String sql = "UPDATE invoices SET payment_status = ? "
                   + "WHERE invoice_id = ? AND deleted_at IS NULL RETURNING appointment_id, patient_id, "
                   + "total_amount, issued_at, updated_at";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setObject(2, UUID.fromString(invoiceId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ResourceNotFoundException("Invoice", invoiceId);
                Invoice invoice = new Invoice();
                invoice.setInvoiceId(invoiceId);
                invoice.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
                invoice.setPatientId(rs.getObject("patient_id", UUID.class).toString());
                invoice.setTotalAmount(rs.getBigDecimal("total_amount"));
                invoice.setPaymentStatus(status);
                invoice.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
                invoice.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                return invoice;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update invoice payment status: " + e.getMessage(), e);
        }
    }

    @Override
    public void softDelete(String invoiceId) throws Exception {
        String sql = "UPDATE invoices SET deleted_at = CURRENT_TIMESTAMP WHERE invoice_id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, UUID.fromString(invoiceId));
            if (ps.executeUpdate() == 0) throw new ResourceNotFoundException("Invoice", invoiceId);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete invoice: " + e.getMessage(), e);
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(rs.getObject("invoice_id", UUID.class).toString());
        invoice.setAppointmentId(rs.getObject("appointment_id", UUID.class).toString());
        invoice.setPatientId(rs.getObject("patient_id", UUID.class).toString());
        invoice.setTotalAmount(rs.getBigDecimal("total_amount"));
        invoice.setPaymentStatus(rs.getString("payment_status"));
        invoice.setIssuedAt(rs.getTimestamp("issued_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        invoice.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        invoice.setDeletedAt(deletedAt != null ? deletedAt.toLocalDateTime() : null);
        return invoice;
    }
}
