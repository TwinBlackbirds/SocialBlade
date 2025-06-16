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
import org.hibernate.cfg.Configuration;



public class Sqlite {
	private static Logger log;
	public static final SessionFactory db;
	
	static {
		Configuration config = new Configuration().configure();
		// debug
		// System.out.println("Dialect = " + config.getProperty("hibernate.dialect"));
		db = config.buildSessionFactory();
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
			log.Write(LogLevel.ERROR, "WriteChannel operation failed! " + e);
		}
	}
}
