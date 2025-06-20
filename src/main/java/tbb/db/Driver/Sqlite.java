// Name: Michael Amyotte
// Date: 6/16/25
// Purpose: SQLite ORM example driver for JScraper template

package tbb.db.Driver;

// database table objects
import tbb.db.Schema.Channel;
import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import jakarta.persistence.EntityNotFoundException;



public class Sqlite {
	private static Logger log;
	public static final SessionFactory db;
	
	static {
		
		BootstrapServiceRegistry bootstrapRegistry =
			    new BootstrapServiceRegistryBuilder()
			        .applyIntegrator(new SQLiteIntegrator()) // ✔️ register integrator here
			        .build();
		
		StandardServiceRegistry standardRegistry =
			    new StandardServiceRegistryBuilder(bootstrapRegistry)
			        .configure() // hibernate.cfg.xml
			        .build();
		
		db = new MetadataSources(standardRegistry)
			    .buildMetadata() // annotated classes
			    .buildSessionFactory();
	}
	
	public Sqlite(Logger log) {
		this.log = log;
	}
	
	// example write method
	public void writeChannel(Channel c) throws Exception {
		try (Session s = db.openSession()){ // try-with-resources
			Transaction t = s.beginTransaction();
			s.persist(c);
			t.commit();
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "writeChannel operation failed! " + e);
		}
	}
	public boolean findChannel(String ID) {
		try (Session s = db.openSession()){ // try-with-resources
			Channel ch = s.find(Channel.class, ID);
			if (ch != null) {
				return true;
			}
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "findChannel operation failed! " + e);
		}
		return false;
	}
	
	public Channel getChannel(String ID) throws Exception {
		Channel ch = null;
		try (Session s = db.openSession()){ // try-with-resources
			ch = s.find(Channel.class, ID);
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "getChannel operation failed! " + e);
		} 
		if (ch == null) {
			throw new EntityNotFoundException("No channel with ID " + ID + " exists in database!");
		}
		return ch;
	}
	
	public void updateChannel(Channel c) throws Exception {
		Channel ch = null;
		try (Session s = db.openSession()){ // try-with-resources
			ch = s.find(Channel.class, c.ID);
			if (ch == null) {
				throw new EntityNotFoundException("No channel with ID " + c.ID + " exists in database!");
			}
			// update operation
			Transaction t = s.beginTransaction();
			s.merge(c);
			t.commit();
		} catch (EntityNotFoundException ex) {
			log.Write(LogLevel.ERROR, "updateChannel operation failed! " + ex);
			throw ex;
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "updateChannel operation failed! " + e);
		}
	}
	
	public int countChannels() {
		try (Session s = db.openSession()){ // try-with-resources
			s.beginTransaction();
			int result = s.createNativeQuery("select count(*) from channels", Integer.class).uniqueResult();
			s.getTransaction().commit();
			return result;
		} catch (Exception e) {
			log.Write(LogLevel.ERROR, "countChannels operation failed! " + e);
		}
		return 0;
	}
}
