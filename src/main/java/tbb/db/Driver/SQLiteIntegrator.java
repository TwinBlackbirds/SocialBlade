// Name: Michael Amyotte
// Date: 6/20/25
// Purpose: Database pre-run configuration integrator (similar to DbContext)
package tbb.db.Driver;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SQLiteIntegrator implements Integrator {

    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.sqlite");
        	Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=30000");
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply SQLite pragmas", e);
        }
    }

    @Override
    public void disintegrate(
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
        // no-op
    }
}