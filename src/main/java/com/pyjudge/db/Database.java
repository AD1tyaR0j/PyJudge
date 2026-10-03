package com.pyjudge.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Owns the JDBC connection pool and provides two ways to run database work:
 * <ul>
 *   <li>{@link #withConnection} - borrow a connection for one unit of work (auto-commit);</li>
 *   <li>{@link #inTransaction} - run several statements atomically: commit on success, roll back on any error.</li>
 * </ul>
 * A transaction is bound to the current thread, so DAO calls made inside {@code inTransaction}
 * automatically join it instead of borrowing a separate connection.
 */
public final class Database implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final ThreadLocal<Connection> transaction = new ThreadLocal<>();

    public Database(String jdbcUrl) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setPoolName("pyjudge-db");
        cfg.setMaximumPoolSize(jdbcUrl.startsWith("jdbc:sqlite") ? 4 : 10);
        if (jdbcUrl.startsWith("jdbc:sqlite")) {
            // passed to the driver for every new connection:
            cfg.addDataSourceProperty("foreign_keys", "true");   // enforce REFERENCES / ON DELETE
            cfg.addDataSourceProperty("busy_timeout", "10000");  // wait for the write lock instead of failing
            cfg.addDataSourceProperty("journal_mode", "WAL");    // readers don't block the judge's writes
        }
        this.dataSource = new HikariDataSource(cfg);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    /** Runs {@code work} on the current transaction's connection, or a pooled one. */
    public <T> T withConnection(SqlFunction<T> work) {
        Connection tx = transaction.get();
        try {
            if (tx != null) {
                return work.apply(tx);
            }
            try (Connection c = dataSource.getConnection()) {
                return work.apply(c);
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    /** Runs {@code work} atomically. Nested calls join the outer transaction. */
    public <T> T inTransaction(SqlFunction<T> work) {
        if (transaction.get() != null) {
            return withConnection(work);
        }
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            transaction.set(c);
            try {
                T result = work.apply(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e instanceof RuntimeException re ? re : new DataAccessException((SQLException) e);
            } finally {
                transaction.remove();
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public void inTransaction(SqlRunnable work) {
        inTransaction(c -> {
            work.run(c);
            return null;
        });
    }

    /** Executes a .sql resource (statements separated by semicolons) inside one transaction. */
    public void runScript(String resource) {
        String sql;
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing SQL resource " + resource);
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        inTransaction(c -> {
            try (Statement st = c.createStatement()) {
                for (String stmt : sql.split(";\\s*\\n")) {
                    String trimmed = stmt.replaceAll("(?m)^--.*$", "").trim();
                    if (!trimmed.isEmpty()) {
                        st.execute(trimmed);
                    }
                }
            }
        });
    }

    @Override
    public void close() {
        dataSource.close();
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection c) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlRunnable {
        void run(Connection c) throws SQLException;
    }
}
