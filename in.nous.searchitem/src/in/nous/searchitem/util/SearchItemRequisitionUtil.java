package in.nous.searchitem.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.openbravo.dal.service.OBDal;

public class SearchItemRequisitionUtil implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	private static Properties prop;
	private static volatile SearchItemRequisitionUtil instance;
	private static Connection conn;
	private static PreparedStatement pst = null;
	private static ResultSet rs = null;
	
	private SearchItemRequisitionUtil(){
		//no-op
	}
	
	public static SearchItemRequisitionUtil getInstance(){
		if(instance == null){
			synchronized(SearchItemRequisitionUtil.class){
				if(instance == null){
					instance = new SearchItemRequisitionUtil();
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
		final String query="select * from dsidef_module_config where module_name='in.nous.searchitem'";
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
