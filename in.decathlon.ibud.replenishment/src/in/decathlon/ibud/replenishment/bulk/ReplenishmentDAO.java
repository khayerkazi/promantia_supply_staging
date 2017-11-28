package in.decathlon.ibud.replenishment.bulk;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;

import com.sysfore.catalog.CLBrand;

public class ReplenishmentDAO {

	public void createTemporaryTable() {
		String creation = "create global temporary table if not exists WORK_AUTO_REPLENISHMENT ("
				+ " m_product_id varchar(32),"
				+ " brand_id varchar(32),"
				+ " displaymin numeric(4),"
				+ " min_qty numeric(4),"
				+ " max_qty numeric(4),"
				+ " ue_qty numeric(4),"
				+ " pcb_qty numeric(4),"
				+ " log_rec varchar(60),"
				+ " in_stock numeric(10) default 0,"
				+ " required_qty numeric(10) default 0,"
				+ " qty_alrdy_order numeric(10) default 0,"
				+ " qty_tobe_ordered numeric(10) default 0,"
				+ " is_pcb boolean,"
				+ " rk numeric(5) default -1,"
				+ " cessionprice numeric(10) default NULL,"
				+ " order_id varchar(32),"
				+ " constraint PK_TEMP_WORK_AUTO_REPLENISHMENT primary key (m_product_id)"
				+ " ) on commit delete rows";

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

	public void insertIntoTableProductConfiguration(Organization org) {
		StringBuffer sb = new StringBuffer("insert into WORK_AUTO_REPLENISHMENT")
				.append("(m_product_id,brand_id,displaymin,min_qty,max_qty,ue_qty,pcb_qty,log_rec,is_pcb)")
				.append(" select p.m_product_id,m.CL_Brand_ID, coalesce(mm.displaymin,'0'), coalesce(mm.minqty,'0'), coalesce(mm.maxqty,'0'), ")
				.append(" coalesce(p.em_cl_ue_qty,'0'),coalesce(p.em_cl_pcb_qty,'0'),coalesce(p.em_cl_log_rec,'0'),")
				.append(" (p.em_idsd_oxylane_prodcat_id is not null or mm.em_idsd_pcb_threshold_id is not null)")
				.append(" from m_product p")
				.append(" inner join cl_minmax mm on p.m_product_id=mm.m_product_id")
				.append(" inner join CL_Model m on p.EM_Cl_Model_ID=m.CL_Model_ID")
				.append(" where mm.ad_org_id=?")
				.append(" and mm.Isinrange='Y'")
				.append(" and mm.em_facst_is_direct_delivery != 'Y'");


		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(sb.toString());
			stmt.setString(1, org.getId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new OBException("cannot find product list for stores...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * get the product stock and put it in temporary table
	 * 
	 * @param org
	 */
	public void computeStock(Organization org) {
		StringBuffer sb = new StringBuffer("update WORK_AUTO_REPLENISHMENT ar")
				.append(" set in_stock=agr.qtyonhand")
				.append(" from")
				.append("(select m_product_id,sum(qtyonhand) as qtyonhand")
				.append(" from m_storage_detail sd ")
				.append(" inner join m_locator l  on l.m_locator_id = sd.m_locator_id")
				.append(" where l.m_warehouse_id = (select m_warehouse_id from ad_org_warehouse where ad_org_warehouse.ad_org_id = ? order by priority limit 1)")
				.append(" and m_product_id in (select m_product_id from WORK_AUTO_REPLENISHMENT)")
				.append(" group by m_product_id) agr")
				.append(" where agr.m_product_id = ar.m_product_id");

		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(sb.toString());
			stmt.setString(1, org.getId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new OBException("Cannot calculate store stock", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * Compute the required quantity. Compatation differ from the replenishment type (spoon/regular)
	 * 
	 * @param type
	 */
	public void computeNeededQuantity(ReplenishmentTypeEnum type) {
		String query;

		if (type == ReplenishmentTypeEnum.SPOON) {
			query = "update WORK_AUTO_REPLENISHMENT set required_qty=greatest(0,greatest(max_qty,displaymin)-in_stock) where in_stock<= greatest(displaymin,max_qty)";
		} else {
			query = "update WORK_AUTO_REPLENISHMENT set required_qty=greatest(0,greatest(max_qty,displaymin)-in_stock) where in_stock<= greatest(displaymin,min_qty)";
		}

		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			throw new OBException("Cannot calculate needed quantity...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * for every product that need a replenishment, calculate the quantity previously ordered We will be able to compute
	 * the quantity to order with that.
	 * 
	 * @param org
	 */
	public void computInOrderQuantity(Organization org) {
		StringBuffer sb = new StringBuffer("update WORK_AUTO_REPLENISHMENT ar")
				.append(" set qty_alrdy_order=agr.ac ")
				.append(" from")
				.append(" (")
				.append("	select m_product_id,sum(")
				.append("			case when o.em_sw_po_reference is null then (ol.qtyordered-cast(coalesce(ol.em_sw_recqty,'0') as integer)) ")
				.append("				else (ol.em_sw_confirmedqty-cast(coalesce(ol.em_sw_recqty,'0') as integer)) end")
				.append("			    ) as ac ")
				.append("	from c_orderline ol")
				.append("	join c_order o on o.c_order_id = ol.c_order_id")
				.append("	join c_doctype cd on cd.c_doctype_id=o.c_doctype_id")
				.append("	where  o.issotrx = 'N' ")
				.append("	and o.docstatus not in ('DR','VO','CL') ")
				.append("	and o.em_sw_isauto_order='Y' ")
				.append("		and o.ad_org_id = ? ")
				.append("		and ol.m_product_id in (select m_product_id from WORK_AUTO_REPLENISHMENT where required_qty>0)")
				.append("		and cd.isreturn='N'")
				.append("	group by m_product_id")
				.append("	) agr")
				.append("  where agr.m_product_id = ar.m_product_id")
				.append("   and required_qty>0");

		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(sb.toString());
			stmt.setString(1, org.getId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new OBException("Cannot compute quantity in order", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * Calculate the qty to be ordered
	 */
	public void computeToBeOrdered() {
		String query = "update WORK_AUTO_REPLENISHMENT set qty_tobe_ordered=greatest(0,required_qty-qty_alrdy_order)";

		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			throw new OBException("Cannot compute to be ordered qty...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * We need to round to a PCB for some product
	 */
	public void roundQtytoPCB() {
		String query = "update WORK_AUTO_REPLENISHMENT set qty_tobe_ordered=  pcb_qty*(div(qty_tobe_ordered,pcb_qty) +1)"
				+ " where qty_tobe_ordered%pcb_qty>0 and is_pcb and pcb_qty>0";

		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			throw new OBException("Cannot compute to be ordered qty PCB round Up...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * we also need to round to a UE
	 */
	public void roundQtytoUE() {
		String query = "update WORK_AUTO_REPLENISHMENT set qty_tobe_ordered=  ue_qty*(div(qty_tobe_ordered,ue_qty) +1)"
				+
				" where not is_pcb and ue_qty>0 and qty_tobe_ordered%ue_qty>0";
		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			throw new OBException("Cannot compute to be ordered qty UE round Up...", e);
		} finally {
			closeStatement(stmt);
		}
	}
	
	/**
	 * Set orderedQty to Zero if UE=0
	 */
	public void ignoreUEEqZeroQty() {
		String query = "update WORK_AUTO_REPLENISHMENT set qty_tobe_ordered = 0"
				+
				" where ue_qty=0 or in_stock<0 ";
		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			throw new OBException("Cannot compute to be ordered qty UE Zero...", e);
		} finally {
			closeStatement(stmt);
		}
		
	}

	public void saveOrder(List<Order> orders) {
		for (Order order : orders) {
			OBDal.getInstance().save(order);
		}
	}

	/**
	 * Get the number of product to order into the temporary table
	 * 
	 * @return Map of "store dept" as key, number of product to order as value.
	 */
	public Map<String, Integer> getNumberProductToOrder() {
		String query = "select w.brand_id,count(w.*),d.name " +
				"from WORK_AUTO_REPLENISHMENT w " +
				"inner join cl_brand d on d.cl_brand_id=w.brand_id " +
				"where w.qty_tobe_ordered>0 " +
				"group by w.brand_id,d.name order by d.name";

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			rs = stmt.executeQuery(query);

			Map<String, Integer> result = new LinkedHashMap<String, Integer>();
			while (rs.next()) { 
				result.put(rs.getString(1), rs.getInt(2));
			}

			return result;
		} catch (SQLException e) {
			throw new OBException("Cannot compute to be ordered qty...", e);
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
		}

	}

	/**
	 * Set rank on line to find order
	 */
	public void computeRank() {
		String query = "update WORK_AUTO_REPLENISHMENT ar set rk=r.rk-1 " +
				" from (" +
				"     select m_product_id, rank() over (partition by brand_id order by m_product_id) as rk " +
				"      from  WORK_AUTO_REPLENISHMENT where qty_tobe_ordered>0" +
				"      ) r" +
				" where r.m_product_id=ar.m_product_id;";

		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			throw new OBException("Cannot compute rank for line...", e);
		} finally {
			closeStatement(stmt);
		}
	}

	public void computePrice(Organization org) {
		String query = "update WORK_AUTO_REPLENISHMENT ar set cessionprice=p.em_cl_cessionprice from " +
				"   ( select m_product_id,em_cl_cessionprice from M_ProductPrice pp "
				+
				"            inner join M_PriceList_Version plv on pp.m_pricelist_version_id=plv.m_pricelist_version_id "
				+
				"            inner join m_pricelist pl on pl.m_pricelist_id=plv.m_pricelist_id " +
				"          where pl.issopricelist='Y' and pp.ad_org_id=?" +
				"        ) p " +
				" where p.m_product_id=ar.m_product_id " +
				"  and ar.qty_tobe_ordered>0 ";

		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(query);
			stmt.setString(1, org.getId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			throw new OBException("Cannot compute price", e);
		} finally {
			closeStatement(stmt);
		}
	}

	public void addOrderId(Map<CLBrand, List<Order>> orders) {
		String query = "update WORK_AUTO_REPLENISHMENT set order_id=? " +
				"  where brand_id=? and div(rk,?)=? and cessionprice is not null ";

		PreparedStatement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().prepareStatement(query);

			for (Entry<CLBrand, List<Order>> e : orders.entrySet()) {
				List<Order> os = e.getValue();
				for (int i = 0; i < os.size(); i++) {
					Order o = os.get(i);

					stmt.setString(1, o.getId());
					stmt.setString(2, e.getKey().getId());
					stmt.setInt(3, ReplenishmentUtils.NB_PRODUCT_PER_ORDER);
					stmt.setInt(4, i);

					stmt.addBatch();
				}
			}

			stmt.executeBatch();
		} catch (SQLException e) {
			throw new OBException("Cannot compute quantity in order", e);
		} finally {
			closeStatement(stmt);
		}
	}

	/**
	 * Create orderline in bulk mode.
	 * 
	 * @param orders
	 */
	public void associateOrderWithLine() {
		String query = "insert into c_orderline(c_orderline_id,"
				+ "                      ad_client_id,"
				+ "                      ad_org_id,"
				+ "                      isActive,"
				+ "                      created,"
				+ "                      createdby,"
				+ "                      updated,"
				+ "                      updatedby,"
				+ "                      dateordered,"
				+ "                      c_order_id,"
				+ "                      m_product_id,"
				+ "                      c_uom_id,"
				+ "                      c_currency_id,"
				+ "                      qtyordered,"
				+ "                      qtydelivered,"
				+ "                      qtyreserved,"
				+ "                      qtyinvoiced,"
				+ "                      pricelist,"
				+ "                      c_tax_id,"
				+ "                      grosspricelist,"
				+ "                      m_warehouse_id,"
				+ "                      line"
				+ "               )"
				+ " select get_uuid()," // c_order_line_id
				+ "        o.ad_client_id," // client id
				+ "        o.ad_org_id, " // Ord id
				+ "        'Y'," // is active
				+ "        now()," // created
				+ "        o.createdby," // createdby
				+ "        now()," // updated
				+ "        o.createdby," //updated by
				+ "        now()," // Date ordered
				+ "        o.c_order_id," // order id
				+ "        ar.m_product_id," // product id
				+ "        p.c_uom_id," // uom id
				+ "        c.c_currency_id," // Currency id
				+ "        ar.qty_tobe_ordered,"  // qty ordered
				+ "        0," // delivered
				+ "        0," // reserved
				+ "        0," // invoiced
				+ "        0," // pricelist
				+ "        t.c_tax_id," // tax id
				+ "        0," // Gross price list
				+ "        o.m_warehouse_id," // warehouse
				+ "        ar.rk" // line
				+ " from WORK_AUTO_REPLENISHMENT ar"
				+ " inner join c_order o on ar.order_id=o.c_order_id"
				+ " inner join m_product p on ar.m_product_id=p.m_product_id"
				+ " inner join ad_client c on c.ad_client_id=p.ad_client_id"
				+ " inner join (select c_taxcategory_id,c_tax_id,rank() over (partition by c_taxcategory_id order by c_tax_id) rk  from c_tax) t on t.c_taxcategory_id=p.c_taxcategory_id"
				// + " inner join c_taxcategory tc on tc.c_taxcategory_id=p.c_taxcategory_id "
				+ " where t.rk=1";

		Statement stmt = null;
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			stmt.executeUpdate(query);
		} catch (SQLException e) {
			throw new OBException("Cannot insert orderline", e);
		} finally {
			closeStatement(stmt);
		}
	}

	public Map<String, MinMaxComputed> getComputed(boolean isSpoon) {
		String query = "select w.m_product_id,"
				+ " d.name as depName,"
				+ " p.name as prName,"
				+ " p.em_cl_modelname as modName,"
				+ " p.em_cl_modelcode as modCode,"
				+ " p.em_cl_lifestage as lifeStage,"
				+ " p.em_cl_size as size,"
				+ " w.displaymin,"
				+ " w.min_qty,"
				+ " w.max_qty,"
				+ " p.em_cl_ue_qty,"
				+ " p.em_cl_pcb_qty,"
				+ " w.log_rec,"
				+ " w.in_stock,"
				+ " w.required_qty,"
				+ " w.qty_tobe_ordered,"
				+ " is_pcb,"
				+ " qty_alrdy_order"
				+ " from  WORK_AUTO_REPLENISHMENT w"
				+ " inner join m_product p on p.m_product_id=w.m_product_id"
				+ " inner join cl_brand d on d.cl_brand_id=w.brand_id";
		// + " order by d.name";

		Statement stmt = null;
		ResultSet rs = null;
		Map<String, MinMaxComputed> map = new HashMap<String, MinMaxComputed>();
		try {
			stmt = OBDal.getInstance().getConnection().createStatement();
			rs = stmt.executeQuery(query);

			while (rs.next()) {
				String prodId = rs.getString(1);
				MinMaxComputed val = new MinMaxComputed();
				val.setProductId(prodId);
				val.setDeptName(rs.getString(2));
				val.setProductName(rs.getString(3));
				val.setModelName(rs.getString(4));
				val.setModelCode(rs.getString(5));
				val.setLifeStage(rs.getString(6));
				val.setsize(rs.getString(7));
				val.setDisplayMin(rs.getInt(8));
				val.setMin(rs.getInt(9));
				val.setMax(rs.getInt(10));
				val.setUeQty(rs.getInt(11));
				val.setPcbQty(rs.getInt(12));
				val.setLogRec(rs.getString(13));
				val.setStoreStock(rs.getInt(14));
				val.setRequiredQty(rs.getInt(15));
				val.setToBeOrderedQty(rs.getInt(16));
				val.setPcb(rs.getBoolean(17));
				val.setQtyalreadyOrdered(rs.getInt(18));
				val.setSpoon(isSpoon);

				map.put(prodId, val);
			}

			return map;
		} catch (SQLException e) {
			throw new OBException("Cannot get confirmed qty...", e);
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

	public void flush() {
		OBDal.getInstance().flush();
	}

	public void refreshOrder(List<Order> ods) {
		for (Order o : ods) {
			OBDal.getInstance().refresh(o);
		}
	}

}
