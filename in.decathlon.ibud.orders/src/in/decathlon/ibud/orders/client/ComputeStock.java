package in.decathlon.ibud.orders.client;

import in.decathlon.ibud.orders.data.OrderReturn;
import in.decathlon.ibud.orders.data.StockState;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;

public class ComputeStock {

	public void createTemporaryTable() {
		String creation = "create global temporary table if not exists WORK_GET_STOCK ("
				+ " m_product_id varchar(32),"
				+ " cacstock numeric(10) default 0,"
				+ " cacrelease numeric(10) default 0,"
				+ " carstock numeric(10) default 0,"
				+ " carrelease numeric(10) default 0,"
				+ " hubstock numeric(10) default 0,"
				+ " hubrelease numeric(10) default 0,"
				+ " constraint PK_TEMP_WORK_GET_STOCK primary key (m_product_id)"
				+ ") on commit delete rows";

		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(creation);
		} catch (SQLException e) {
			throw new OBException("Temporary table exception...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	public void insertIntoTableProduct(Set<String> products) {
		StringBuffer sb = new StringBuffer("insert into WORK_GET_STOCK")
				.append("(m_product_id) values(?)");

		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(sb.toString());
			for (String id : products) {
				stmt.setString(1, id);
				stmt.addBatch();
			}

			stmt.executeBatch();
		} catch (SQLException e) {
			throw new OBException("cannot insert products list...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * Compute stock for a warehouse
	 * 
	 * @param org
	 */
	public void computeStock(String name, String name2, String warehouseId) {
		String innerquery = " SELECT sd.m_product_id, sum(qtyOnHand) as qty "
				+ " from m_Storage_Detail sd"
				+ " inner join m_locator l on l.m_locator_id=sd.m_locator_id"
				+ " inner join m_warehouse mw on mw.m_warehouse_id=l.m_warehouse_id"
				+ " where mw.em_idsd_whgroup = ? "
				+ " and sd.m_product_id in (select m_product_id from WORK_GET_STOCK)"
				+ " and l.isactive='Y'"
				+ " and l.em_oBWHS_Type='ST' "
				+ " group by sd.m_product_id";

		String query = "update WORK_GET_STOCK w set " + name + "=t.qty "
				+ "from ( " + innerquery + ") t where t.m_product_id=w.m_product_id";

		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(query);
			stmt.setString(1, warehouseId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new OBException("Cannot calculate stock", e);
		} finally {
			closeStatement(stmt);
		}

		String innerquery2 = " SELECT sd.m_product_id, sum(reservedqty) as qty "
				+ " from m_Storage_Detail sd"
				+ " inner join m_locator l on l.m_locator_id=sd.m_locator_id"
				+ " inner join m_warehouse mw on mw.m_warehouse_id=l.m_warehouse_id"
				+ " where mw.em_idsd_whgroup = ? "
				+ " and sd.m_product_id in (select m_product_id from WORK_GET_STOCK)"
				+ " and l.isactive='Y'"
				+ " and l.em_oBWHS_Type='ST' "
				+ " group by sd.m_product_id";

		String query2 = "update WORK_GET_STOCK w set " + name2 + "=t.qty "
				+ "from ( " + innerquery2 + ") t where t.m_product_id=w.m_product_id";

		PreparedStatement stmt2 = null;
		try {
			stmt2 = OBDal.getInstance().getConnection().prepareStatement(query2);
			stmt2.setString(1, warehouseId);
			stmt2.executeUpdate();
		} catch (SQLException e) {
			throw new OBException("Cannot calculate stock", e);
		} finally {
			closeStatement(stmt2);
		}

	}

	public void retriveStock(OrderReturn orderReturn) {
		String query = "select m_product_id, cacstock-cacrelease,carstock-carrelease,hubstock-hubrelease from WORK_GET_STOCK";

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			rs = stmt.executeQuery(query);

			Map<String, StockState> map = orderReturn.getStocks();
			while (rs.next()) {
				StockState s = new StockState();
				s.setCacStock(rs.getInt(2));
				s.setCarStock(rs.getInt(3));
				s.setHubStock(rs.getInt(4));
				map.put(rs.getString(1), s);
			}
		} catch (SQLException e) {
			throw new OBException("Cannot get stock...", e);
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
		}
	}

	/**
	 * Close statement if needed
	 * 
	 * @param stmt
	 *            prepared statement
	 */
	private void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				// Nothing to do
			}
		}
	}

	/**
	 * Close resultset if needed
	 * 
	 * @param rs
	 *            result set
	 */
	private void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
				// Nothing to do
			}
		}
	}

}
