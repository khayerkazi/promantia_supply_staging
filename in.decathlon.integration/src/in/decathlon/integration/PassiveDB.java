package in.decathlon.integration;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;

public class PassiveDB implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private static Properties prop;

	private static volatile PassiveDB instance;
	private Connection conn = null;
	
	private  static Logger log4j = Logger.getLogger(PassiveDB.class);

	private PassiveDB() {
		// no-op
	}

	static {
		prop = new Properties();
		ResourceBundle rb = ResourceBundle
				.getBundle("in/decathlon/integration/dbp");
		Set<String> keys = rb.keySet();
		for (String key : keys) {
			prop.put(key, rb.getString(key));
		}
	}

	public static PassiveDB getInstance() {
		if (instance == null) {
			synchronized (PassiveDB.class) {
				if (instance == null) {
					instance = new PassiveDB();
				}
			}
		}
		return instance;
	}

	public Connection getConnection() throws SQLException,
			ClassNotFoundException {
		try {
			Class.forName(prop.getProperty("db.passivedb.driverClassName"));
			conn = DriverManager.getConnection(prop
					.getProperty("db.passivedb.url"), prop
					.getProperty("db.passivedb.user"), prop
					.getProperty("db.passivedb.password"));
			System.out.println("connected");
		} catch (SQLException sql) {
			throw sql;
		} catch (ClassNotFoundException e) {
			throw e;
		}
		return conn;
	}
	
	public Connection getSupplyDBConnection() throws SQLException,
			ClassNotFoundException {
		try {
			Class.forName(prop.getProperty("supplydb.passivedb.driverClassName"));
			conn = DriverManager.getConnection(prop
					.getProperty("supplydb.passivedb.url"), prop
					.getProperty("supplydb.passivedb.user"), prop
					.getProperty("supplydb.passivedb.password"));
			System.out.println("Supply DB connected");
		} catch (SQLException sql) {
			throw sql;
		} catch (ClassNotFoundException e) {
			throw e;
		}
		return conn;
	}
	
	public Connection getRetailDBConnection() throws SQLException,
		ClassNotFoundException {
		try {
			Class.forName(prop.getProperty("retaildb.passivedb.driverClassName"));
			conn = DriverManager.getConnection(prop
					.getProperty("retaildb.passivedb.url"), prop
					.getProperty("retaildb.passivedb.user"), prop
					.getProperty("retaildb.passivedb.password"));
			System.out.println("Retail DB connected");
		} catch (SQLException sql) {
			throw sql;
		} catch (ClassNotFoundException e) {
			throw e;
		}
		return conn;
		}


}
