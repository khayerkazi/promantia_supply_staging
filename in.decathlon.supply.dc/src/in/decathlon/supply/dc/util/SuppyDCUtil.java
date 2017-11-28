package in.decathlon.supply.dc.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.openbravo.dal.service.OBDal;

public class SuppyDCUtil implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;
  private static Properties prop;
  private static volatile SuppyDCUtil instance;
  private static Connection conn;
  private static PreparedStatement pst = null;
  private static ResultSet rs = null;

  private SuppyDCUtil() {
    // no-op
  }

  public static SuppyDCUtil getInstance() {
    if (instance == null) {
      synchronized (SuppyDCUtil.class) {
        if (instance == null) {
          instance = new SuppyDCUtil();
        }
      }
    }
    return instance;
  }

  protected Object readResolve() {
    return instance;
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  public Properties getProperties() {
    prop = new Properties();
    conn = OBDal.getInstance().getConnection();
    final String query = "select * from dsidef_module_config where module_name='in.decathlon.supply.dc'";
    try {
      pst = conn.prepareStatement(query);
      rs = pst.executeQuery();
      while (rs.next()) {
        prop.put(rs.getString("key"), rs.getString("value"));
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return prop;
  }

  public Properties getProperties(String module_name) {
    prop = new Properties();
    conn = OBDal.getInstance().getConnection();
    final String query = "select * from dsidef_module_config where module_name='" + module_name
        + "'";
    try {
      pst = conn.prepareStatement(query);
      rs = pst.executeQuery();
      while (rs.next()) {
        prop.put(rs.getString("key"), rs.getString("value"));
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return prop;
  }
}
