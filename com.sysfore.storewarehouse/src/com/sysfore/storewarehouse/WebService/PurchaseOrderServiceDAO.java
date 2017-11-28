package com.sysfore.storewarehouse.WebService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import org.openbravo.service.db.CallStoredProcedure;
import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;

import com.sysfore.storewarehouse.WebService.OrderDTO;
import com.sysfore.storewarehouse.WebService.OrderlineDTO;

public class PurchaseOrderServiceDAO {

	private static Logger log = Logger.getLogger(PurchaseOrderServiceDAO.class);

	private static Connection conn = null;

	private static ResourceBundle properties = null;

	private static String successStatus = "successStatus";

	private static String success = "success";

	private static String failure = "failure";

	private static String Y = "Y";

	private static String N = "N";

	private static String poDesc = "PO creation through Web Service";

	private static String poLineDesc = "PO line creation through Web Service";

	/**
	 * Constructor used to fetch database properties
	 * 
	 * @throws Exception
	 */
	public PurchaseOrderServiceDAO() throws Exception {
		OBDal obDal = new OBDal();
		conn = obDal.getConnection();
		conn.setAutoCommit(false);
	}

	/**
	 * This method creates purchase order and purchase order lines by inserting
	 * the data into c_order and c_orderline tables
	 * 
	 * @param purchaseOrder
	 * @return orderDTO
	 */
	public OrderDTO createPO(OrderDTO purchaseOrder) {
		String sqlCOrder = "INSERT INTO c_order("
				+ "c_order_id, ad_client_id, ad_org_id, isactive, createdby,"
				+ "updatedby, issotrx, documentno, docstatus, docaction,"
				+ "processing, processed, c_doctype_id, c_doctypetarget_id, description, "
				+ "isdelivered, isinvoiced, isprinted, isselected, "
				+ "dateordered, datepromised,  dateacct, c_bpartner_id,"
				+ "billto_id, c_bpartner_location_id, isdiscountprinted,"
				+ "paymentrule, c_paymentterm_id, invoicerule, deliveryrule,"
				+ "freightcostrule, freightamt, deliveryviarule,"
				+ "chargeamt, priorityrule, totallines, grandtotal, m_warehouse_id,"
				+ "m_pricelist_id, istaxincluded, "
				+ "posted,  copyfrom,"
				+ "isselfservice,"
				+ "generatetemplate, "
				+ "copyfrompo, fin_paymentmethod_id,"
				+ "rm_pickfromshipment, rm_receivematerials, rm_createinvoice,"
				+ " rm_addorphanline,  calculate_promotions,"
				+ "convertquotation,"
				+ " em_ds_totalitemqty, em_ds_totalpriceadj, "
				+ " em_ds_grandtotalamt, "
				+ "em_ds_chargeamt, "
				+ "em_sw_postatus, c_currency_id,em_sw_isauto_order,em_cl_storedept_id)"

				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
				+ "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

		String sqlCOrderLine = "INSERT INTO c_orderline("
				+ "c_orderline_id, ad_client_id, ad_org_id, isactive, created, createdby,"
				+ "updated, updatedby, c_order_id, line, c_bpartner_id, c_bpartner_location_id,"
				+ "dateordered, datepromised,  dateinvoiced, description, m_product_id, m_warehouse_id, "
				+ "directship, c_uom_id, qtyordered, qtyreserved, qtydelivered, qtyinvoiced, "
				+ " pricelist, priceactual, pricelimit, linenetamt,  freightamt,"
				+ "chargeamt, c_tax_id, isdescription, pricestd, cancelpricead, iseditlinenetamt,"
				+ "taxbaseamt,  gross_unit_price, line_gross_amount, grosspricestd, explode, em_ds_taxamount, "
				+ "em_ds_unitqty,  em_ds_ccunitprice, em_ds_mrpprice, em_ds_linenetamt, "
				+ "em_sw_orderqty, em_sw_volpcb, em_sw_ntwtpcb, em_sw_grwtpcb, em_sw_noofparcel,"
				+ "em_sw_itemcode, em_cl_modelname, em_cl_color_id, em_cl_size, em_sw_confirmedqty,"
				+ "c_currency_id)"

				+ "VALUES (?, ?, ?, ?, ?, ?,?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
				+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

		PreparedStatement pstOrder = null, pstOrderLine = null;
		ResultSet rsOrder = null;
		String bpartnerLocationId = "";
		String cOrderID = "";
		String documentNo = "";
		String uomId = "100";
		String docTypeId = "";
		String paymenttermId = "";
		String paymentMethodId = "";
		String adClientId = "";
		String adOrgId = "";
		String cbpartnerId = "";
		String pricelistId = "";
		String pricelistVersionId = "";
		String warehouseId = "";
		String adUserId = "";
		String currencyId = "";
		int line = 10;
		String lineId = "";
		//To create Manual DC in StoreRequisition Window
		String itemCode = "";
		String deptName = "";

		try {

			pstOrder = conn
					.prepareStatement("select c_doctype_id from c_doctype where name=?");
			pstOrder.setString(1, "Purchase Order");
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				docTypeId = rsOrder.getString("c_doctype_id");
			} else {
				return logError("c_doctype_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select c_paymentterm_id from c_paymentterm where name=?");
			pstOrder.setString(1, "Immediate");
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				paymenttermId = rsOrder.getString("c_paymentterm_id");
			} else {
				return logError("c_paymentterm_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select fin_paymentmethod_id from fin_paymentmethod where name=?");
			pstOrder.setString(1, "Cash");
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				paymentMethodId = rsOrder.getString("fin_paymentmethod_id");
			} else {
				return logError("fin_paymentmethod_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select ad_client_id from ad_client where name=?");
			pstOrder.setString(1, "DSI");
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				adClientId = rsOrder.getString("ad_client_id");
			} else {
				return logError("ad_client_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select ad_org_id from ad_org where name=?");
			pstOrder.setString(1, purchaseOrder.getStoreName());
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				adOrgId = rsOrder.getString("ad_org_id");
			} else {
				return logError("ad_org_id could not be found.", purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			//pstOrder = conn.prepareStatement("select c_bpartner_id from c_bpartner where name = (select name from ad_ref_list where value = ?)");

			pstOrder = conn.prepareStatement("select c_bpartner_id from c_bpartner where name = 'Whitefield Warehouse'");

//System.out.println(purchaseOrder.getStoreName());

			//pstOrder.setString(1, "Warehouse_" + purchaseOrder.getStoreName());
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				cbpartnerId = rsOrder.getString("c_bpartner_id");
			} else {
				return logError("c_bpartner_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select c_bpartner_location_id from c_bpartner_location where c_bpartner_id=?");
			pstOrder.setString(1, cbpartnerId);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				bpartnerLocationId = rsOrder
						.getString("c_bpartner_location_id");
			} else {
				return logError("c_bpartner_location_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select m_pricelist_id from m_pricelist where ad_org_id =? order by created desc limit 1");
			pstOrder.setString(1, adOrgId);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				pricelistId = rsOrder.getString("m_pricelist_id");
			} else {
				return logError("m_pricelist_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select m_pricelist_version_id from m_pricelist_version where ad_org_id =? order by created desc limit 1");
			pstOrder.setString(1, adOrgId);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				pricelistVersionId = rsOrder
						.getString("m_pricelist_version_id");
			} else {
				return logError("m_pricelist_version_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select ad_user_id from ad_user where email =?");


			pstOrder.setString(1, purchaseOrder.getEmail());
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				adUserId = rsOrder.getString("ad_user_id");
			} else {
				return logError("ad_user_id could not be found.", purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select m_warehouse_id from m_warehouse where ad_org_id = "
							+ "(select ad_org_id from ad_org where name =(select name from c_bpartner where c_bpartner_id=? )) and em_sw_iscar='Y'");
			pstOrder.setString(1, cbpartnerId);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				warehouseId = rsOrder.getString("m_warehouse_id");
			} else {
				return logError("m_warehouse_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select c_currency_id from c_currency where iso_code=?");
			pstOrder.setString(1, "INR");
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				currencyId = rsOrder.getString("c_currency_id");
			} else {
				return logError("c_currency_id could not be found.",
						purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select *  from ad_sequence_next('C_Order', ?)");
			pstOrder.setString(1, adClientId);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				cOrderID = rsOrder.getString("p_nextno");
			} else {
				return logError("p_nextno could not be found.", purchaseOrder);
			}

			pstOrder = null;
			rsOrder = null;

			pstOrder = conn
					.prepareStatement("select * from ad_sequence_doc(?,?,?)");
			pstOrder.setString(1, "Purchase Order");
			pstOrder.setString(2, adClientId);
			pstOrder.setString(3, Y);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				documentNo = rsOrder.getString("p_documentno");
			}

			pstOrder = null;
			rsOrder = null;
			// To create Manual Dc in Store Requisition on manual order creation
			itemCode = purchaseOrder.getListOrderlineDTOs().get(0).getItemCode(); 
			pstOrder = conn
					.prepareStatement("select cl_storedept_id from cl_storedept where cl_storedept_id=" +
					"(select cl_storedept_id from cl_model where cl_model_id=" +
					"(select em_cl_model_id from m_product where name=?))");
			
			pstOrder.setString(1,itemCode);
			rsOrder = pstOrder.executeQuery();
			if(rsOrder.next())
			{
				deptName = rsOrder.getString("cl_storedept_id");
			}else{
				return logError("cl_storedept_id could not be found.", purchaseOrder);
			}
						
			pstOrder = null;
			rsOrder = null;

			pstOrder = conn.prepareStatement(sqlCOrder);
			pstOrder.setString(1, cOrderID);
			pstOrder.setString(2, adClientId);
			pstOrder.setString(3, adOrgId);
			pstOrder.setString(4, Y);
			pstOrder.setString(5, adUserId);
			pstOrder.setString(6, adUserId);
			pstOrder.setString(7, N);
			pstOrder.setString(8, documentNo);
			pstOrder.setString(9, "DR");
			pstOrder.setString(10, "CO");
			pstOrder.setString(11, N);
			pstOrder.setString(12, N);
			pstOrder.setString(13, docTypeId);
			pstOrder.setString(14, docTypeId);
			pstOrder.setString(15, poDesc);
			pstOrder.setString(16, N);
			pstOrder.setString(17, N);
			pstOrder.setString(18, N);
			pstOrder.setString(19, N);
			Timestamp currentTimeStamp = new Timestamp(new Date().getTime());
			pstOrder.setTimestamp(20, currentTimeStamp);
			pstOrder.setTimestamp(21, currentTimeStamp);
			pstOrder.setTimestamp(22, currentTimeStamp);
			pstOrder.setString(23, cbpartnerId);
			// billToId is same as bpartnerLocationId
			pstOrder.setString(24, bpartnerLocationId);
			pstOrder.setString(25, bpartnerLocationId);
			pstOrder.setString(26, N);
			pstOrder.setString(27, "P");
			pstOrder.setString(28, paymenttermId);
			pstOrder.setString(29, "D");
			pstOrder.setString(30, "A");
			pstOrder.setString(31, "I");
			pstOrder.setInt(32, 0);
			pstOrder.setString(33, "P");
			pstOrder.setInt(34, 0);
			pstOrder.setInt(35, 5);
			pstOrder.setInt(36, 0);
			pstOrder.setInt(37, 0);
			pstOrder.setString(38, warehouseId);
			pstOrder.setString(39, pricelistId);
			pstOrder.setString(40, Y);
			pstOrder.setString(41, N);
			pstOrder.setString(42, N);
			pstOrder.setString(43, N);
			pstOrder.setString(44, N);
			pstOrder.setString(45, N);
			pstOrder.setString(46, paymentMethodId);
			pstOrder.setString(47, N);
			pstOrder.setString(48, N);
			pstOrder.setString(49, N);
			pstOrder.setString(50, N);
			pstOrder.setString(51, N);
			pstOrder.setString(52, N);
			pstOrder.setInt(53, 0);
			pstOrder.setInt(54, 0);
			pstOrder.setInt(55, 0);// grand total
			pstOrder.setInt(56, 0);
			pstOrder.setString(57, "DR");
			pstOrder.setString(58, currencyId);
            pstOrder.setString(59, N);
            pstOrder.setString(60,deptName);
			pstOrder.executeUpdate();

			// Insert Puchase Order Lines
			for (OrderlineDTO orderlineDTO : purchaseOrder
					.getListOrderlineDTOs()) {
				double lineNetAmt = 0.0;
				double taxAmt = 0.0;
				double taxRate = 0;
				String modelName = "";
				String colorName = "";
				String size = "";
				String productId = "";
				String regionName = "";
				String taxId = "";

				lineNetAmt = orderlineDTO.getPriceActual()
						* orderlineDTO.getQtyOrdered();

				//TODO:change to name for m_product
				pstOrder = conn
						.prepareStatement("select em_cl_modelname,(select name from cl_color where cl_color_id =em_cl_color_id) "
								+ "as color_name, em_cl_size from m_product where name=?");
				pstOrder.setString(1, orderlineDTO.getItemCode());
				rsOrder = pstOrder.executeQuery();
				if (rsOrder.next()) {
					modelName = rsOrder.getString("em_cl_modelname");
					colorName = rsOrder.getString("color_name");
					size = rsOrder.getString("em_cl_size");
				} else {
					return logError(
							"em_cl_modelname, color_name, em_cl_size could not be found for itemCode: "
									+ orderlineDTO.getItemCode(), purchaseOrder);
				}

				pstOrder = null;
				rsOrder = null;

				//TODO:change to name for m_product
				pstOrder = conn
						.prepareStatement("select m_product_id from m_product where name=?");
				pstOrder.setString(1, orderlineDTO.getItemCode());
				rsOrder = pstOrder.executeQuery();
				if (rsOrder.next()) {
					productId = rsOrder.getString("m_product_id");
				} else {
					return logError(
							"m_product_id could not be found for itemCode: "
									+ orderlineDTO.getItemCode(), purchaseOrder);
				}

                               pstOrder = null;
				rsOrder = null;

				/*pstOrder = conn
						.prepareStatement("select  from m_product where name=?");
				pstOrder.setString(1, orderlineDTO.getItemCode());
				rsOrder = pstOrder.executeQuery();
				if (rsOrder.next()) {
					productId = rsOrder.getString("m_product_id");
				} else {
					return logError(
							"m_product_id could not be found for itemCode: "
									+ orderlineDTO.getItemCode(), purchaseOrder);
				}

				pstOrder = null;
				rsOrder = null;*/

				// pstOrder = conn
				// .prepareStatement("select rate from c_tax where c_tax_id = ?");
				// pstOrder.setString(1, orderlineDTO.getTaxId());
				// rsOrder = pstOrder.executeQuery();
				// if (rsOrder.next()) {
				// taxRate = Double.parseDouble(rsOrder.getString("rate"));
				// }
				//
				// rsOrder = null;

				pstOrder = conn
						.prepareStatement("select name from c_region where c_region_id = "
								+ "(select c_region_id from c_location where c_location_id ="
								+ "(select c_location_id from ad_orginfo where ad_org_id =?))");
				pstOrder.setString(1, adOrgId);
				rsOrder = pstOrder.executeQuery();
				if (rsOrder.next()) {
					regionName = rsOrder.getString("name");
				} else {
					return logError(
							"region name could not be found for adOrgId: "
									+ adOrgId, purchaseOrder);
				}

				pstOrder = null;
				rsOrder = null;

				//TODO:change to name from value
				if (null != regionName && regionName.equals("Karnataka")) {
					pstOrder = conn
							.prepareStatement("select c_tax_id,rate from c_tax where c_taxcategory_id =(select c_taxcategory_id from m_product where name=?)");
					pstOrder.setString(1, orderlineDTO.getItemCode());
					rsOrder = pstOrder.executeQuery();
					if (rsOrder.next()) {
                                                taxId = rsOrder.getString("c_tax_id");
						taxRate = Double.parseDouble(rsOrder.getString("rate"));
					} else {
						return logError(
								"tax rate could not be found in Karnataka region for itemCode: "
										+ orderlineDTO.getItemCode(),
								purchaseOrder);
					}
				} else {
					pstOrder = conn
							.prepareStatement("select c_tax_id,rate from c_tax where c_taxcategory_id ="
									+ "(select em_cl_taxcategory_id from m_productprice where m_product_id=? and ad_org_id=?)");
					pstOrder.setString(1, productId);
					pstOrder.setString(2, adOrgId);
					rsOrder = pstOrder.executeQuery();
					if (rsOrder.next()) {
                                                taxId = rsOrder.getString("c_tax_id");   
						taxRate = Double.parseDouble(rsOrder.getString("rate"));
					} else {
						return logError(
								"tax rate could not be found in non-Karnataka region for itemCode: "
										+ orderlineDTO.getItemCode()
										+ " and adOrgId" + adOrgId,
								purchaseOrder);
					}

				}

				rsOrder = null;

				taxAmt = (taxRate / 100) * lineNetAmt;
				lineId = SequenceIdData.getUUID();
				pstOrderLine = conn.prepareStatement(sqlCOrderLine);
				pstOrderLine.setString(1, lineId);
				pstOrderLine.setString(2, adClientId);
				pstOrderLine.setString(3, adOrgId);
				pstOrderLine.setString(4, Y);
				pstOrderLine.setTimestamp(5, currentTimeStamp);
				pstOrderLine.setString(6, adUserId);
				pstOrderLine.setTimestamp(7, currentTimeStamp);
				pstOrderLine.setString(8, adUserId);
				pstOrderLine.setString(9, cOrderID);
				pstOrderLine.setInt(10, line);
				pstOrderLine.setString(11, cbpartnerId);
				pstOrderLine.setString(12, bpartnerLocationId);
				pstOrderLine.setTimestamp(13, currentTimeStamp);
				pstOrderLine.setTimestamp(14, currentTimeStamp);
				pstOrderLine.setTimestamp(15, currentTimeStamp);
				pstOrderLine.setString(16, poLineDesc);
				pstOrderLine.setString(17, productId);
				pstOrderLine.setString(18, warehouseId);
				pstOrderLine.setString(19, N);
				pstOrderLine.setString(20, uomId);
				pstOrderLine.setInt(21, orderlineDTO.getQtyOrdered());
				pstOrderLine.setInt(22, 0);
				pstOrderLine.setInt(23, 0);
				pstOrderLine.setInt(24, 0);
				pstOrderLine.setInt(25, 0);
				pstOrderLine.setDouble(26, orderlineDTO.getPriceActual());
				pstOrderLine.setInt(27, 0);
				pstOrderLine.setDouble(28, lineNetAmt);
				pstOrderLine.setDouble(29, 0.00);
				pstOrderLine.setDouble(30, 0.00);
				pstOrderLine.setString(31, taxId);
				pstOrderLine.setString(32, N);
				pstOrderLine.setDouble(33, 0.00);
				pstOrderLine.setString(34, N);
				pstOrderLine.setString(35, N);
				pstOrderLine.setInt(36, 0);
				pstOrderLine.setInt(37, 0);
				pstOrderLine.setInt(38, 0);
				pstOrderLine.setInt(39, 0);
				pstOrderLine.setString(40, N);
				pstOrderLine.setDouble(41, taxAmt);
				pstOrderLine.setInt(42, 0);
				pstOrderLine.setDouble(43, orderlineDTO.getPriceActual());
				pstOrderLine.setDouble(44, orderlineDTO.getPriceActual());
				pstOrderLine.setInt(45, 0);
				pstOrderLine.setInt(46, orderlineDTO.getQtyOrdered());
				pstOrderLine.setInt(47, 1);
				pstOrderLine.setInt(48, 0);
				pstOrderLine.setInt(49, 0);
				pstOrderLine.setInt(50, orderlineDTO.getQtyOrdered());
				pstOrderLine.setString(51, orderlineDTO.getItemCode());
				pstOrderLine.setString(52, modelName);
				pstOrderLine.setString(53, colorName);
				pstOrderLine.setString(54, size);
				pstOrderLine.setInt(55, 0);
				pstOrderLine.setString(56, currencyId);
				pstOrderLine.executeUpdate();

				line += 10;
			}

			conn.commit();
			pstOrder = null;
			rsOrder = null;
			int confirmed = 0;

			//TODO:change to name for m_product
			pstOrder = conn
					.prepareStatement("select coalesce(em_sw_confirmedqty,0) as confirmed from c_orderline where c_orderline_id=?");
			pstOrder.setString(1, lineId);
			rsOrder = pstOrder.executeQuery();
			if (rsOrder.next()) {
				confirmed = Integer.parseInt(rsOrder.getString("confirmed"));
			} 
			purchaseOrder.getListOrderlineDTOs().get(0).setConfirmedQty(confirmed);
			
			//pstOrderLine.close();
			//pstOrder.close();
			
			
			//call the post procedure
			 final List<Object> param = new ArrayList<Object>();

System.out.println("cOrderID=="+cOrderID);

		      param.add(null);
		      param.add(cOrderID);
		      CallStoredProcedure.getInstance().call("C_ORDER_POST1", param, null, true, false);

System.out.println("C_ORDER_POST1 Executed Successfully");
			
		} catch (Exception e) {
			log.error("An error occurred while creating Purchase Order", e);
			purchaseOrder.setSuccessStatus(e.getMessage());
			return purchaseOrder;
		} finally {
			pstOrder = null;
			pstOrderLine = null;
			rsOrder = null;
		}
System.out.println("documentNo=="+documentNo);

System.out.println("success=="+success);

		purchaseOrder.setCOrderId(cOrderID);
		purchaseOrder.setDocumentNo(documentNo);
		purchaseOrder.setSuccessStatus(success);
		return purchaseOrder;

	}

	/**
	 * This method is used to close the connection with database
	 * 
	 * @throws SQLException
	 */
	public void closeConnection() throws SQLException {
		conn.close();
		conn = null;
	}

	/**
	 * This method logs the given error message and returns an error status
	 * 
	 * @param errorMessage
	 * @param purchaseOrder
	 * @return purchaseOrder
	 */
	private static OrderDTO logError(String errorMessage, OrderDTO purchaseOrder) {
		log.error("An error occurred in PurchaseOrderServiceDAO: "
				+ errorMessage);
		purchaseOrder.setSuccessStatus(errorMessage);
		return purchaseOrder;
	}
}
