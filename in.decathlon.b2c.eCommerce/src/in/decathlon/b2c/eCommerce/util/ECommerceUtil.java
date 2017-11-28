package in.decathlon.b2c.eCommerce.util;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

public class ECommerceUtil implements Serializable, Cloneable{
	
	private static final long serialVersionUID = 1L;
	private static Properties prop;
	private static volatile ECommerceUtil instance;
	private static Connection conn;
	private static PreparedStatement pst = null;
	private static ResultSet rs = null;
	
	private ECommerceUtil(){
		//no-op
	}
	
	public static ECommerceUtil getInstance(){
		if(instance == null){
			synchronized(ECommerceUtil.class){
				if(instance == null){
					instance = new ECommerceUtil();
				}
			}
		}
		return instance;
	}
	
	protected Object readResolve(){
		return instance;
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	public Properties getProperties(){
		prop = new Properties();
		conn = OBDal.getInstance().getConnection();
		final String query="select * from dsidef_module_config where module_name='in.decathlon.b2c.eCommerce'";
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
