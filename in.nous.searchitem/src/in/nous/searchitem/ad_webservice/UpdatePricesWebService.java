/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package in.nous.searchitem.ad_webservice;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.apache.log4j.Logger;

import java.util.Iterator;

import org.openbravo.model.ad.access.User;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.pricing.levelpricing.LevelProductPrice;
import org.openbravo.pricing.levelpricing.LevelPricingRange;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.web.WebService;

/**
 * Post the stock of the store and create a PI
 * 
 * @author shreyas
 */
public class UpdatePricesWebService implements WebService {

	private static final long serialVersionUID = 1L;
	private static Logger log = Logger.getLogger(UpdatePricesWebService.class);

	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// do some checking of parameters
		try {
		PriceModel priceMode = new PriceModel();
		StringWriter sw = new StringWriter();
		Boolean updateAllVal = false;
		String hql = "";
		Query query = null;
		String message = null;
		String userid = null;
		String code = request.getParameter("code");
		final Session session = OBDal.getInstance().getSession();
		if (code == null) {
			throw new IllegalArgumentException(
					"The code parameter is mandatory");
		}
		priceMode.setItemcode(code);
		String email = request.getParameter("email");
		if (email == null) {
			throw new IllegalArgumentException(
					"The email parameter is mandatory");
		}
		String storename = request.getParameter("storename");
		if (storename == null) {
			throw new IllegalArgumentException(
					"The storename parameter is mandatory");
		}
		priceMode.setStorename(storename);
		String displaymin = request.getParameter("displaymin");

		if (displaymin == null) {
			String updateAll = request.getParameter("updateAll");
			if (updateAll == null) {
				throw new IllegalArgumentException(
						"The updateAll parameter is mandatory");
			} else {
				updateAllVal = Boolean.parseBoolean(updateAll);
				priceMode.setUpdateAllVal(updateAllVal);
			}
			String unitprice = request.getParameter("unitprice");
			if (unitprice == null) {
				throw new IllegalArgumentException(
						"The unitprice parameter is mandatory");
			}
			priceMode.setUnitprice(unitprice);
			String ueprice = request.getParameter("ueprice");
			if (ueprice == null) {
				throw new IllegalArgumentException(
						"The ueprice parameter is mandatory");
			}
			priceMode.setUeprice(ueprice);
			String pcbprice = request.getParameter("pcbprice");
			if (pcbprice == null) {
				throw new IllegalArgumentException(
						"The pcbprice parameter is mandatory");
			}
			priceMode.setPcbprice(pcbprice);
			String strqty = request.getParameter("strqty");
			if (strqty == null) {
				throw new IllegalArgumentException(
						"The strqty parameter is mandatory");
			}
			priceMode.setStrqty(strqty);
			String boxqty = request.getParameter("boxqty");
			if (boxqty == null) {
				throw new IllegalArgumentException(
						"The boxqty parameter is mandatory");
			}
			priceMode.setBoxqty(boxqty);
			// Added for new Margin fields
			String unitmargin = request.getParameter("unitmargin");
			if (unitmargin == null) {
				throw new IllegalArgumentException(
						"The unit margin parameter is mandatory");
			}
			priceMode.setUnitmargin(unitmargin);
			String uemargin = request.getParameter("uemargin");
			if (uemargin == null) {
				throw new IllegalArgumentException(
						"The ue margin parameter is mandatory");
			}
			priceMode.setUemargin(uemargin);
			String pcbmargin = request.getParameter("pcbmargin");
			if (pcbmargin == null) {
				throw new IllegalArgumentException(
						"The pcb margin parameter is mandatory");
			}
			priceMode.setPcbmargin(pcbmargin);

			// System.out.println("clUnitmarginamount="+unitmargin);

			try {
				hql = "select COALESCE(id,'NA') from ADUser where email='"
						+ email + "'";
				query = session.createQuery(hql);
				userid = query.list().get(0).toString();
				if (userid.equals("NA")) {
					message = "Error in getting the store stock: User has no email";
				} else {
					priceMode.setUserId(userid);
					query = null;
					if (updateAllVal) {
						hql = "select name from Product where clModelcode=(select clModelcode from Product where name='"
								+ code + "')  order by name desc";
						query = session.createQuery(hql);
						List list = query.list();
						List rowList;
						final StringBuilder resp = new StringBuilder();
						for (Object rows : list) {
							hql = "select id from PricingProductPrice where product.name='"
									+ rows.toString()
									+ "' and organization.name='"
									+ storename
									+ "' order by creationDate desc";
							query = session.createQuery(hql);
							query.setMaxResults(1);
							rowList = query.list();
							if (rowList.size() > 0) {
								int result = updatePriceOfProduct(session, priceMode, rowList.get(0).toString());
								if(result == 0)
									resp.append(rows.toString()+",");
							} else {
								updateAllVal = false;
							}
						}
						
						if (updateAllVal) {
							if(resp.length() == 0) {
								sw.append("<message>Success in updating the prices</message>");
							} else {
								resp.deleteCharAt(resp.length()-1);
								sw.append("<message>Success in updating the prices except "+resp.toString()+" </message>");
							}
						} else {
							sw.append("<message>Unable to update Prices-Incomplete Data</message>");
						}
					}
					if (query == null) {
						hql = "select id from PricingProductPrice where product.name='"
								+ code
								+ "' and organization.name='"
								+ storename + "' order by creationDate desc";
						query = session.createQuery(hql);
						query.setMaxResults(1);
						List list = query.list();
						if (list.size() > 0) {
							String productid = list.get(0).toString();
							int result = updatePriceOfProduct(session, priceMode, productid);
							if (result > 0) {
								sw.append("<message>Success in updating the prices</message>");
							}

						} else {

							sw.append("<message>Unable to update Prices-Incomplete Data</message>");
						}

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				SessionHandler.getInstance().rollback();
				log.error("Error occured due to: " + e.getMessage());
				message = "Error in updating the prices";
				sw.append("<message>" + message + "</message>");
			} finally {
				SessionHandler.getInstance().commitAndClose();
			}
		} else {
			try {
				hql = "select COALESCE(id,'NA') from ADUser where email='"
						+ email + "'";
				query = OBDal.getInstance().getSession().createQuery(hql);
				userid = query.list().get(0).toString();
				if (userid.equals("NA")) {
					message = "Error in getting the store stock: User has no email";
				} else {
					hql = "select id from CL_Minmax where product.name='"
							+ code + "' and organization.name = '" + storename
							+ "' order by creationDate desc";
					query = OBDal.getInstance().getSession().createQuery(hql);
					query.setMaxResults(1);
					List list = query.list();
					if (list.size() > 0) {
						String productid = list.get(0).toString();
						hql = "update CL_Minmax " + "set displaymin="
								+ displaymin + ", " + "updatedby='" + userid
								+ "', " + "updated=now()" + "where id='"
								+ productid + "'";
						query = OBDal.getInstance().getSession()
								.createQuery(hql);
						int result = query.executeUpdate();
						if (result > 0) {
							sw.append("<message>Success in updating the displaymin</message>");
						}

					} else {
						sw.append("<message>Unable to update DisplayMin-Incomplete Data</message>");
					}

				}
			} catch (Exception e) {
				// e.printStackTrace();
				log.error("Error occured due to: " + e.getMessage());
				message = "Error in updating the prices";
				sw.append("<message>" + message + "</message>");
			}

		}
		OBDal.getInstance().commitAndClose();

		// sw.append("<message>"+message+"</message>");
		// and get the result
		final String xml = sw.toString();

		// write to the response
		response.setContentType("text/xml");
		response.setCharacterEncoding("utf-8");
		final Writer w = response.getWriter();
		w.write(xml);
		w.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int updatePriceOfProduct(Session session, PriceModel priceModel, String productid){
		
		String rangeBoxId = null;
		String rangeLotId = null;
		int result =0;
		rangeBoxId = getRangeBoxId(session);
		rangeLotId = getRangeLotId(session);
		
		boolean isalgorithm = false;
		/*
		 * Calculating Lot 
		 */
		if(priceModel.getStrqty() != null && Integer.parseInt(priceModel.getStrqty()) > 1){
			
			//Inserting new Record in lvlpr_levelproductprice table.
			LevelProductPrice levelPrice = null;
			final OBCriteria<LevelProductPrice> levelProductPriceObCriteria = OBDal.getInstance().createCriteria(LevelProductPrice.class);
			levelProductPriceObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_PRODUCTPRICE, OBDal.getInstance().get(ProductPrice.class, productid)));
			levelProductPriceObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_LVLPRRANGE, OBDal.getInstance().get(LevelPricingRange.class, rangeLotId)));
			
			final OBCriteria<LevelProductPrice> levelProductPriceqtyObCriteria = OBDal.getInstance().createCriteria(LevelProductPrice.class);
			levelProductPriceqtyObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_PRODUCTPRICE, OBDal.getInstance().get(ProductPrice.class, productid)));
			levelProductPriceqtyObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_QUANTITY, new BigDecimal(priceModel.getStrqty())));
			
			
			if(levelProductPriceObCriteria.count() > 0) {
				levelPrice = levelProductPriceObCriteria.list().get(0);
				
				levelPrice.setPrice(new BigDecimal(priceModel.getUeprice()));
				levelPrice.setQuantity(new BigDecimal(priceModel.getStrqty()));
				
			} else if(levelProductPriceqtyObCriteria.count() > 0) {
				levelPrice = levelProductPriceqtyObCriteria.list().get(0);
				
				levelPrice.setPrice(new BigDecimal(priceModel.getUeprice()));
				levelPrice.setQuantity(new BigDecimal(priceModel.getStrqty()));
				levelPrice.setLvlprRange(OBDal.getInstance().get(LevelPricingRange.class, rangeLotId));
			} else {
				
				
				levelPrice = OBProvider.getInstance().get(LevelProductPrice.class);
				
				levelPrice.setClient(OBContext.getOBContext().getCurrentClient());
				levelPrice.setOrganization(OBContext.getOBContext().getCurrentOrganization());
				levelPrice.setCreatedBy(OBContext.getOBContext().getUser());
				levelPrice.setUpdatedBy(OBContext.getOBContext().getUser());
				levelPrice.setPrice(new BigDecimal(priceModel.getUeprice()));
				levelPrice.setQuantity(new BigDecimal(priceModel.getStrqty()));
				levelPrice.setProductPrice(OBDal.getInstance().get(ProductPrice.class, productid));
				levelPrice.setLvlprRange(OBDal.getInstance().get(LevelPricingRange.class, rangeLotId));
			}
			
			
			OBDal.getInstance().save(levelPrice);
			OBDal.getInstance().flush();
			SessionHandler.getInstance().commitAndStart();
			isalgorithm = true;
		}

		/*
		 * Calculating Box
		 */
		if((priceModel.getBoxqty() != null && Integer.parseInt(priceModel.getBoxqty()) > 1)){

			if((priceModel.getBoxqty() != null && Integer.parseInt(priceModel.getBoxqty()) > 1) &&
					(Float.parseFloat(priceModel.getBoxqty()) != Float.parseFloat(priceModel.getStrqty()))){
				//Inserting new Record in lvlpr_levelproductprice table

				LevelProductPrice levelPrice = null;
				final OBCriteria<LevelProductPrice> levelProductPriceObCriteria = OBDal.getInstance().createCriteria(LevelProductPrice.class);
				levelProductPriceObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_PRODUCTPRICE, OBDal.getInstance().get(ProductPrice.class, productid)));
				levelProductPriceObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_LVLPRRANGE, OBDal.getInstance().get(LevelPricingRange.class, rangeBoxId)));
				
				final OBCriteria<LevelProductPrice> levelProductPriceqtyObCriteria = OBDal.getInstance().createCriteria(LevelProductPrice.class);
				levelProductPriceqtyObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_PRODUCTPRICE, OBDal.getInstance().get(ProductPrice.class, productid)));
				levelProductPriceqtyObCriteria.add(Restrictions.eq(LevelProductPrice.PROPERTY_QUANTITY, new BigDecimal(priceModel.getStrqty())));
				
				if(levelProductPriceObCriteria.count() > 0) {
					levelPrice = levelProductPriceObCriteria.list().get(0);
					
					levelPrice.setPrice(new BigDecimal(priceModel.getPcbprice()));
					levelPrice.setQuantity(new BigDecimal(priceModel.getBoxqty()));
					
				} else if(levelProductPriceqtyObCriteria.count() > 0) {
					levelPrice = levelProductPriceqtyObCriteria.list().get(0);
					
					levelPrice.setPrice(new BigDecimal(priceModel.getPcbprice()));
					levelPrice.setQuantity(new BigDecimal(priceModel.getBoxqty()));
					levelPrice.setLvlprRange(OBDal.getInstance().get(LevelPricingRange.class, rangeLotId));
				} else {
					levelPrice = OBProvider.getInstance().get(LevelProductPrice.class);
					
					levelPrice.setClient(OBContext.getOBContext().getCurrentClient());
					levelPrice.setOrganization(OBContext.getOBContext().getCurrentOrganization());
					levelPrice.setCreatedBy(OBContext.getOBContext().getUser());
					levelPrice.setUpdatedBy(OBContext.getOBContext().getUser());
					levelPrice.setPrice(new BigDecimal(priceModel.getPcbprice()));
					levelPrice.setQuantity(new BigDecimal(priceModel.getBoxqty()));
					levelPrice.setProductPrice(OBDal.getInstance().get(ProductPrice.class, productid));
					levelPrice.setLvlprRange(OBDal.getInstance().get(LevelPricingRange.class, rangeBoxId));
				}
				
				
				OBDal.getInstance().save(levelPrice);
				OBDal.getInstance().flush();
				SessionHandler.getInstance().commitAndStart();
				isalgorithm = true;
			} else if(Integer.parseInt(priceModel.getBoxqty()) > 1 && getLevelProductPriceId(session,productid)){
				//Update lvlpr_levelproductprice table
				
			}
		}
		
		if(isalgorithm) {
			result=1;
			final ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, productid);
			productPrice.setAlgorithm("SLP_algorithm");
			productPrice.setClCcunitprice(new BigDecimal(priceModel.getUnitprice()));
			productPrice.setClCcueprice(new BigDecimal(priceModel.getUeprice()));
			productPrice.setClCcpcbprice(new BigDecimal(priceModel.getPcbprice()));
			productPrice.setCLEMClSuqty(Long.parseLong(priceModel.getStrqty()));
			productPrice.setCLEMClSboxqty(Long.parseLong(priceModel.getBoxqty()));
			productPrice.setUpdatedBy(OBDal.getInstance().get(User.class, priceModel.getUserId()));
			productPrice.setClUnitmarginpercentage(new BigDecimal(priceModel.getUnitmargin()));
			productPrice.setClUemarginpercentage(new BigDecimal(priceModel.getUemargin()));
			productPrice.setClPcbmarginpercentage(new BigDecimal(priceModel.getPcbmargin()));
			productPrice.setUpdated(new Timestamp(new Date().getTime()));
			productPrice.setListPrice(new BigDecimal(priceModel.getUnitprice()));
			productPrice.setStandardPrice(new BigDecimal(priceModel.getUnitprice()));
			productPrice.setClFollowcatalog(false);
			
			OBDal.getInstance().save(productPrice);
			
			OBDal.getInstance().flush();
			SessionHandler.getInstance().commitAndStart();
		} else {
			
			result=1;
			final ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, productid);
			productPrice.setAlgorithm("S");
			productPrice.setClCcunitprice(new BigDecimal(priceModel.getUnitprice()));
			productPrice.setClCcueprice(new BigDecimal(priceModel.getUeprice()));
			productPrice.setClCcpcbprice(new BigDecimal(priceModel.getPcbprice()));
			productPrice.setCLEMClSuqty(Long.parseLong(priceModel.getStrqty()));
			productPrice.setCLEMClSboxqty(Long.parseLong(priceModel.getBoxqty()));
			productPrice.setUpdatedBy(OBDal.getInstance().get(User.class, priceModel.getUserId()));
			productPrice.setClUnitmarginpercentage(new BigDecimal(priceModel.getUnitmargin()));
			productPrice.setClUemarginpercentage(new BigDecimal(priceModel.getUemargin()));
			productPrice.setClPcbmarginpercentage(new BigDecimal(priceModel.getPcbmargin()));
			productPrice.setUpdated(new Timestamp(new Date().getTime()));
			productPrice.setListPrice(new BigDecimal(priceModel.getUnitprice()));
			productPrice.setStandardPrice(new BigDecimal(priceModel.getUnitprice()));
			productPrice.setClFollowcatalog(false);
			
			OBDal.getInstance().save(productPrice);
			
			OBDal.getInstance().flush();
			SessionHandler.getInstance().commitAndStart();
		}
		
		
		return result;
	}
	
	private void deleteQtyLevelProductPrice(Session session){
		Query query = null;
		String hql = "delete from lvlpr_levelproductprice where quantity > 1";
		query = session.createQuery(hql);
		query.executeUpdate();
	}
	
	private boolean getLevelProductPriceId(Session session, String productId){
		Query query = null;
		String id = null;
		String hql = "select id from lvlpr_levelproductprice where productPrice.id = '"+productId+"'";
		query = session.createQuery(hql);
		query.setMaxResults(1);
		List list = query.list();
		if (list.size() > 0) {
			return true;
		}
		return false;
	}
	
	private String getRangeBoxId(Session session){
		Query query = null;
		String id = null;
		String hql = "select id from LVLPR_range where name = 'BOX'";
		query = session.createQuery(hql);
		query.setMaxResults(1);
		List list = query.list();
		if (list.size() > 0) {
			id = list.get(0).toString();
		}
		return id;
	}
	private String getRangeLotId(Session session){
		Query query = null;
		String id = null;
		String hql = "select id from LVLPR_range where name = 'LOT'";
		query = session.createQuery(hql);
		query.setMaxResults(1);
		List list = query.list();
		if (list.size() > 0) {
			id = list.get(0).toString();
		}
		return id;
	}
	
	private String getOrganizationId(Session session, String storename){
		Query query = null;
		String hql = "select id from Organization where name = '"+storename+"'";
		query = session.createQuery(hql);
	    query.setMaxResults(1);
	    List list = query.list();
	    if(list.size()>0)
	      	return list.get(0).toString();
	    else 
	    	return "";
	}
	
	
	

	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
	}

	class PriceModel {
		private String productId;
		private String itemcode;
		private String storename;
		private Boolean updateAllVal;
		private String unitprice;
		private String ueprice;
		private String pcbprice;
		private String strqty;
		private String boxqty;
		private String unitmargin;
		private String uemargin;
		private String pcbmargin;
		private String userId;

		public String getProductId() {
			return productId;
		}

		public void setProductId(String productId) {
			this.productId = productId;
		}

		public String getItemcode() {
			return itemcode;
		}

		public void setItemcode(String itemcode) {
			this.itemcode = itemcode;
		}

		public String getStorename() {
			return storename;
		}

		public void setStorename(String storename) {
			this.storename = storename;
		}

		public Boolean getUpdateAllVal() {
			return updateAllVal;
		}

		public void setUpdateAllVal(Boolean updateAllVal) {
			this.updateAllVal = updateAllVal;
		}

		public String getUnitprice() {
			return unitprice;
		}

		public void setUnitprice(String unitprice) {
			this.unitprice = unitprice;
		}

		public String getUeprice() {
			return ueprice;
		}

		public void setUeprice(String ueprice) {
			this.ueprice = ueprice;
		}

		public String getPcbprice() {
			return pcbprice;
		}

		public void setPcbprice(String pcbprice) {
			this.pcbprice = pcbprice;
		}

		public String getStrqty() {
			return strqty;
		}

		public void setStrqty(String strqty) {
			this.strqty = strqty;
		}

		public String getBoxqty() {
			return boxqty;
		}

		public void setBoxqty(String boxqty) {
			this.boxqty = boxqty;
		}

		public String getUnitmargin() {
			return unitmargin;
		}

		public void setUnitmargin(String unitmargin) {
			this.unitmargin = unitmargin;
		}

		public String getUemargin() {
			return uemargin;
		}

		public void setUemargin(String uemargin) {
			this.uemargin = uemargin;
		}

		public String getPcbmargin() {
			return pcbmargin;
		}

		public void setPcbmargin(String pcbmargin) {
			this.pcbmargin = pcbmargin;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

	}

}
