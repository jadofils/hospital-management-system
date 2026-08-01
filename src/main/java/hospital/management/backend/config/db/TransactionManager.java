package hospital.management.backend.config.db;

import hospital.management.backend.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * The single place commit()/rollback() is ever called in this codebase.
 *
 * Any operation that touches more than one statement/table atomically (e.g.
 * create a user AND assign a role, log in AND open a session) must run its
 * DAO calls inside {@link #executeInTransaction}, passing the given
 * {@link Connection} down to each DAO's connection-accepting overload rather
 * than letting each DAO open its own connection. That's what makes the unit
 * of work atomic: one connection, one commit, one rollback.
 *
 * A single-statement DAO call (a plain SELECT, or a lone INSERT/UPDATE with
 * nothing else riding on it) doesn't need this — Postgres already commits a
 * single statement atomically under its default autocommit behaviour.
 */
public final class TransactionManager {

    @FunctionalInterface
    public interface TransactionalWork<T> {
        T execute(Connection conn) throws Exception;
    }

    @FunctionalInterface
    public interface VoidTransactionalWork {
        void execute(Connection conn) throws Exception;
    }

    private TransactionManager() {}

    public static <T> T executeInTransaction(TransactionalWork<T> work) throws Exception {
        Connection conn;
        try {
            conn = DBConnection.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Could not obtain a database connection: " + e.getMessage(), e);
        }

        try {
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // connection is being discarded either way
            }
        }
    }

    public static void executeInTransaction(VoidTransactionalWork work) throws Exception {
        executeInTransaction((TransactionalWork<Void>) conn -> {
            work.execute(conn);
            return null;
        });
    }
}
