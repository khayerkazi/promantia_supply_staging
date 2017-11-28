package in.decathlon.ibud.replenishment.implantation;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;
import in.decathlon.ibud.replenishment.ReplenishmentDalUtils;
import in.decathlon.ibud.replenishment.bulk.ReplenishmentService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;

import au.com.bytecode.opencsv.CSVWriter;

import com.sysfore.catalog.CLBrand;
	
	public class ImplantationDAO {

		public void updateOrderLineId() {
			
			 String query = "update WORK_AUTO_IMPLANTATION set c_orderline_id=get_uuid()"
	                 		+ " where c_order_id is not null";
			 
			 Statement stmt = null;
			 
			  try {
			      stmt = OBDal.getInstance().getConnection().createStatement();
			      stmt.execute(query);
			  } catch (SQLException e) {
			      throw new OBException("Cannot create and update orderline id...", e);
			  } finally {
			    closeStatement(stmt);
			  }
		}

		public void updateOrderId() {
			
			
			String insertTemp = "insert into WORK_TEMP (cl_brand_id, c_order_id,gb) select cl_brand_id,get_uuid(),gb " +
								"from WORK_AUTO_IMPLANTATION where valOrder='Y' group by cl_brand_id,gb";

			String updateOrderId = "update WORK_AUTO_IMPLANTATION work set c_order_id = temp.c_order_id from " +
								   "(select cl_brand_id, c_order_id,gb from WORK_TEMP) temp where work.cl_brand_id = temp.cl_brand_id and work.gb = temp.gb " +
								   "and valOrder = 'Y'";
	 
			 Statement stmt = null;
			 
			  try {
				  
			      stmt = OBDal.getInstance().getConnection().createStatement();
			      stmt.execute(insertTemp);
			      stmt.execute(updateOrderId);
			      
			  } catch (SQLException e) {
			      throw new OBException("Cannot update order id...", e);
			  } finally {
			    closeStatement(stmt);
			  }
			
		}

		public void insertValuesInTable(Organization org) {
			
			String orgId = org.getId();
			
			StringBuffer sb = new StringBuffer("insert into WORK_AUTO_IMPLANTATION")
			.append(" (m_product_id,cl_brand_id,cl_model_id,imp_qty,blkd_qty,itemcode,modelname) ")
			.append(" select cl.m_product_id, clm.cl_brand_id, clm.cl_model_id, cl.implantation_qty, ")
			.append(" cl.blocked_qty, mp.name, clm.name ")
			.append(" from cl_implantation cl inner join m_product mp on cl.m_product_id = mp.m_product_id ")
			.append(" inner join cl_model clm on mp.em_cl_model_id = clm.cl_model_id ")
			.append(" where store_implanted = ? and isimplanted='N' ");

			PreparedStatement stmt = null;
			
			try {
				stmt = OBDal.getInstance().getConnection().prepareStatement(sb.toString());
				stmt.setString(1, orgId);
				stmt.executeUpdate();
			} catch (SQLException e) {
				throw new OBException("cannot find implantation product list for stores...", e);
			} finally {
				closeStatement(stmt);
			}
		}

		public void createTempTables() {

				String creation = "create global temporary table if not exists WORK_AUTO_IMPLANTATION ("
						+ " m_product_id varchar(32),"
						+ " cl_brand_id varchar(32),"
						+ " cl_model_id varchar(32),"
						+ " imp_qty numeric(4),"
						+ " blkd_qty numeric(4),"
						+ " qtyTobeOrd numeric(4),"
						+ " valOrder boolean,"
						+ " itemcode varchar(32),"
						+ " modelname varchar(200),"
						+ " c_order_id varchar(32),"
						+ " c_orderline_id varchar(32),"
						+ " rk numeric(5) default -1,"
						+ " gb numeric default 0,"
						+ " constraint PK_TEMP_WORK_AUTO_IMPLANTATION primary key (m_product_id)"
						+ " ) on commit delete rows";
				
				String creationtemp = "create global temporary table if not exists WORK_TEMP ("
						+ " cl_brand_id varchar(32),"
						+ " c_order_id varchar(32),"
						+ " gb numeric default 0"
						+ " ) on commit delete rows";

				Statement stmt = null;
				try {
					stmt = OBDal.getInstance().getConnection().createStatement();
					stmt.execute(creation);
					stmt.execute(creationtemp);
				} catch (SQLException e) {
					throw new OBException("Temporary table exception...", e);
				} finally {
					closeStatement(stmt);
				}
			
			
		}

		public void closeStatement(Statement stmt) {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception e) {
					// Nothing to do
				}
			}
		}

		public void updateRecords() {
			
			String query = "update WORK_AUTO_IMPLANTATION set valOrder='Y',qtytobeord=(imp_qty-blkd_qty)"
	         		+ " where imp_qty > blkd_qty";
	 
			 Statement stmt = null;
			 
			  try {
			      stmt = OBDal.getInstance().getConnection().createStatement();
			      stmt.execute(query);
			  } catch (SQLException e) {
			      throw new OBException("Cannot update valOrder", e);
			  } finally {
			    closeStatement(stmt);
			  }
			  
			
		}
		
		private void closeResultSet(ResultSet rs) {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					// Nothing to do
				}
			}
		}
		
		public void saveOrder(List<Order> orders) {
			for (Order order : orders) {
				OBDal.getInstance().save(order);
			}
		}

		public List<Order> fetchvalues(Organization org, Map<String, Integer> mapCatNbProd) {
			
			ReplenishmentService replserv = new ReplenishmentService();
			User openBravoUser = OBDal.getInstance().get(User.class, SOConstants.UserId);
			Warehouse wr = BusinessEntityMapper.getOrgWarehouse(org.getId()).getWarehouse();
			Client dsi = ReplenishmentDalUtils.getClient(SOConstants.Client);
			
			DocumentType transactionDocTyp = BusinessEntityMapper.getTrasactionDocumentType(org);
			PriceList pricelist = BusinessEntityMapper.getPriceList(SOConstants.POPriceList);
			FIN_PaymentMethod paymentMethod = ReplenishmentDalUtils.getPaymentMethod(SOConstants.PaymentMethod);
			PaymentTerm paymentTerm = ReplenishmentDalUtils.getPaymentTerm(SOConstants.PaymentTerm);
			
			Sequence sequence = transactionDocTyp.getDocumentSequence();
			int nextNumSeq = 0;	
			String docNo = "";
						
			List<Order> orderlist = new ArrayList<Order>();
			
			String partnerId = BusinessEntityMapper.getSupplyBPartner(org);
			 BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, partnerId);
			if (bp == null) {
				throw new OBException("No business Partner for the supply Organization");
			}
			
			
			String query = "SELECT distinct c_order_id,cl_brand_id,count(*) FROM WORK_AUTO_IMPLANTATION WHERE valOrder='Y' group by c_order_id,cl_brand_id";

			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = OBDal.getInstance().getConnection().createStatement();
				rs = stmt.executeQuery(query);

				while (rs.next()) {
					
					Map<String, Integer> mapCatNbPro = new LinkedHashMap<String, Integer>();
					mapCatNbPro.put(rs.getString(2), rs.getInt(3));
					
					nextNumSeq = replserv.orderNumSeqBulk(mapCatNbProd, sequence);
					docNo = sequence.getPrefix() + nextNumSeq;
					nextNumSeq++;
					
					createheader(org,rs.getString(1), rs.getString(2),docNo,openBravoUser,bp,wr,dsi,transactionDocTyp, pricelist, paymentMethod, paymentTerm,orderlist);
				
				}

				return orderlist;
				
			} catch (SQLException e) {
				throw new OBException("Cannot order and brand...", e);
			} finally {
				closeResultSet(rs);
				closeStatement(stmt);
			}

		
			
			
		}

		private void createheader(Organization org, String orderId, String brandId, String docNo, User openBravoUser, BusinessPartner bp, Warehouse wr, Client dsi, DocumentType transactionDocTyp, PriceList pricelist, FIN_PaymentMethod paymentMethod, PaymentTerm paymentTerm, List<Order> orderlist) {
			
			
			CLBrand brand = OBDal.getInstance().get(CLBrand.class, brandId);
			
			Order newOrder = OBProvider.getInstance().get(Order.class);
			newOrder.setNewOBObject(true);
			newOrder.setId(orderId);
			newOrder.setClient(OBContext.getOBContext().getCurrentClient());
			newOrder.setOrganization(org);
			newOrder.setActive(true);
			newOrder.setCreationDate(new Date());
			newOrder.setCreatedBy(openBravoUser);
			newOrder.setUpdatedBy(openBravoUser);
			newOrder.setUpdated(new Date());
			newOrder.setBusinessPartner(bp);
			newOrder.setPartnerAddress(bp.getBusinessPartnerLocationList().get(0));
			newOrder.setPaymentMethod(paymentMethod);
			newOrder.setPaymentTerms(paymentTerm);
			newOrder.setOrderDate(new Date());
			newOrder.setScheduledDeliveryDate(new Date());
			newOrder.setDocumentStatus(SOConstants.DraftDocumentStatus);
			newOrder.setProcessed(false);
			newOrder.setClBrand(brand);
			newOrder.setTransactionDocument(transactionDocTyp);
			newOrder.setSwIsautoOrder(true);
			newOrder.setPriceList(pricelist);
			newOrder.setDocumentType(transactionDocTyp);
			newOrder.setAccountingDate(new Date());
			newOrder.setSalesTransaction(false);
			newOrder.setCurrency(dsi.getCurrency());
			newOrder.setWarehouse(wr);
			newOrder.setSwIsimplantation(true);
			newOrder.setDocumentNo(docNo);
			OBDal.getInstance().save(newOrder);
			
			orderlist.add(newOrder);

		}

		public void associateOrderLines() {
			
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
						+ " select c_orderline_id," // c_order_line_id
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
						+ "        ar.qtytobeord,"  // qty ordered
						+ "        0," // delivered
						+ "        0," // reserved
						+ "        0," // invoiced
						+ "        0," // pricelist
						+ "        t.c_tax_id," // tax id
						+ "        0," // Gross price list
						+ "        o.m_warehouse_id," // warehouse
						+ "        ar.rk" // line
						+ " from WORK_AUTO_IMPLANTATION ar"
						+ " inner join c_order o on ar.c_order_id=o.c_order_id"
						+ " inner join m_product p on ar.m_product_id=p.m_product_id"
						+ " inner join ad_client c on c.ad_client_id=p.ad_client_id"
						+ " inner join (select c_taxcategory_id,c_tax_id,rank() over (partition by c_taxcategory_id order by c_tax_id) rk  from c_tax) t on t.c_taxcategory_id=p.c_taxcategory_id"
						+ " where t.rk=1";

				Statement stmt = null;
				try {
					stmt = OBDal.getInstance().getConnection().createStatement();
					stmt.executeUpdate(query);
					OBDal.getInstance().flush();
				} catch (SQLException e) {
					throw new OBException("Cannot insert orderline", e);
				} finally {
					closeStatement(stmt);
				}
			}

		
		public Map<String, Integer> getNumberProductToOrder() {
			String query = "select w.cl_brand_id,count(w.*),d.name " +
					"from WORK_AUTO_IMPLANTATION w " +
					"inner join cl_brand d on d.cl_brand_id=w.cl_brand_id " +
					"where w.valOrder='Y' " +
					"group by w.cl_brand_id,d.name order by d.name";

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

		
		
		public Map<String, String> computeOrderQty(Organization org) {
			
			
			String query = "SELECT m_product_id,(imp_qty-blkd_qty) FROM WORK_AUTO_IMPLANTATION WHERE valOrder='Y'";

			Statement stmt = null;
			ResultSet rs = null;
			try {
				stmt = OBDal.getInstance().getConnection().createStatement();
				rs = stmt.executeQuery(query);

				Map<String, String> map = new HashMap<String, String>();
				
				while (rs.next()) { 
					map.put(rs.getString(1), rs.getString(2));
				}

				return map;
				
			} catch (SQLException e) {
				throw new OBException("Cannot order and brand...", e);
			} finally {
				closeResultSet(rs);
				closeStatement(stmt);
			}

			
			}

		public void computerank() {
			
			String query = "update WORK_AUTO_IMPLANTATION ar set rk=r.rank from (select m_product_id, " +
						   "rank() over (partition by cl_brand_id order by m_product_id) as rank " +
						   "from  WORK_AUTO_IMPLANTATION where valOrder='Y') r " +
						   "where r.m_product_id=ar.m_product_id;";

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

		public void csvdata(CSVWriter writer, Organization org) {

			String query = "select cl.name as brand,itemcode,modelname,imp_qty as implantationqty,blkd_qty as blockedqty,qtyTobeOrd as requestedqty," +
						   "coalesce(col.em_sw_confirmedqty,'0') as confirmedqty from  WORK_AUTO_IMPLANTATION temp " +
						   "inner join cl_brand cl on temp.cl_brand_id = cl.cl_brand_id " +
						   "left join c_orderline col on col.c_orderline_id = temp.c_orderline_id " +
						   "where valOrder = 'Y'";
			
			/*String query = "select cl.name,mp.name,mp.em_cl_modelname,requiredqty,openorder,ordergenerated,confirmedqty " +
						   "from ibdrep_logs log join cl_brand cl on log.cl_brand_id = cl.cl_brand_id " +
						   "join c_orderline col on log.c_orderline_id = col.c_orderline_id " +
						   "join m_product mp on log.m_product_id = mp.m_product_id" +
						   "where replenshytype='Implantation' " +
						   "and ad_org_id = '"+org.getId()+"' and created >= now()+1-1 ";*/
			
			Statement stmt = null;
			ResultSet rs = null;
			
			try {
				stmt = OBDal.getInstance().getConnection().createStatement();
				rs = stmt.executeQuery(query);

				while (rs.next()) {
										
					writer.writeNext(new String[] { rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7)});
									
				}
						
		} catch (SQLException e) {
			throw new OBException("Cannot geenerate csv data...", e);
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
		} 
	}

			public void refreshOrder(List<Order> ods) {
				for (Order o : ods) {
					OBDal.getInstance().refresh(o.getOrderLineList());
					OBDal.getInstance().refresh(o);
				}
			}

			public void insertIntoLogTable(Organization org) {

				String query = "insert into ibdrep_logs(ibdrep_logs_id,ad_org_id,ad_client_id,cl_brand_id,m_product_id," +
							   "requiredqty,openorder,ordergenerated,confirmedqty,documentno,c_orderline_id,replenshytype) " +
							   "select get_uuid(),'"+org.getId()+"','187D8FC945A5481CB41B3EE767F80DBB',cl_brand_id,m_product_id,imp_qty," +
							   "blkd_qty,qtyTobeOrd,coalesce(col.em_sw_confirmedqty,'0'),co.documentno,c_orderline_id,'Implantation' from " +
							   "WORK_AUTO_IMPLANTATION temp left join c_orderline col on temp.c_orderline_id = col.c_orderline_id" +
							   "join c_order co on co.c_order_id = col.c_order_id where valOrder = 'Y'";
                                               
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

			public void updateBlockedQty(Organization org) {

				String query = "update cl_implantation cl set updated=now(),blocked_qty = ar.finalblk_qty " +
							   "from (select temp.m_product_id,(temp.blkd_qty + TO_NUMBER(coalesce(col.em_sw_confirmedqty,'0'))) " +
							   "as finalblk_qty from  WORK_AUTO_IMPLANTATION temp left join c_orderline col on col.c_orderline_id = temp.c_orderline_id" +
							   " where valOrder = 'Y') ar where cl.m_product_id = ar.m_product_id and " +
							   "store_implanted='"+ org.getId() +"' and isimplanted='N'";

			Statement stmt = null;
			try {
				stmt = OBDal.getInstance().getConnection().createStatement();
				stmt.execute(query);
			} catch (SQLException e) {
				throw new OBException("Cannot update blockedqty", e);
			} finally {
				closeStatement(stmt);
			}
			
			updateIsImplantation(org);
			
			}
			
			
			public void updateIsImplantation(Organization org) {

				String query = "update cl_implantation set updated=now(),isimplanted='Y' where implantation_qty <= blocked_qty " +
							   "and isimplanted='N' and store_implanted='"+ org.getId() +"'";

			Statement stmt = null;
			try {
				stmt = OBDal.getInstance().getConnection().createStatement();
				stmt.execute(query);
			} catch (SQLException e) {
				throw new OBException("Cannot update isimplantation flag", e);
			} finally {
				closeStatement(stmt);
			}
			
			}


			public String fetchconfirmed(Organization org) {

				String query = "select sum(col.em_sw_confirmedqty) from c_order co, c_orderline col " +
								"where co.ad_org_id='"+org.getId()+"' and co.c_order_id = col.c_order_id and co.em_sw_isimplantation='Y' " +
								"and co.createdby='100' and co.dateordered >= now()+1-1 and co.docstatus not in ('VO','DR') group by co.ad_org_id";

				Statement stmt = null;
				ResultSet rs = null;
				try {
					
					stmt = OBDal.getInstance().getConnection().createStatement();
					rs = stmt.executeQuery(query);

					while (rs.next()) { 
						return (rs.getString(1));
					}

					return "0";
					
				} catch (SQLException e) {
					throw new OBException("Cannot get confirmedQty...", e);
				} finally {
					closeResultSet(rs);
					closeStatement(stmt);
				}
			}

			public void computegb() {

				String query = "update WORK_AUTO_IMPLANTATION set gb=ceil(rk/200) where valOrder='Y'";

				Statement stmt = null;
				try {
					stmt = OBDal.getInstance().getConnection().createStatement();
					stmt.execute(query);
				} catch (SQLException e) {
					throw new OBException("Cannot update computegb field", e);
				} finally {
					closeStatement(stmt);
				}
			}
 
}
