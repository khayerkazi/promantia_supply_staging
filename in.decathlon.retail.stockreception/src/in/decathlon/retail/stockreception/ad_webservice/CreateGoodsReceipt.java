package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.transfer.ad_process.Transfer;
import in.decathlon.integration.PassiveDB;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.web.WebService;

public class CreateGoodsReceipt implements WebService {
	 static Logger log4j = Logger.getLogger(CreateGoodsReceipt.class);
	 HashSet<ShipmentInOut> shipmentInOuts = null;
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		ShipmentInOut goodsReceipt=null;
		Map<String, List<String[]>> mapValue = new HashMap<String, List<String[]>>();
		 String strRead;
		    String BoxDetailsFromWS = "";
		    String dcnumber = "";
		    Connection con;
		    ResultSet getDocumentResultSet = null;
		    String getdocumentno;
		    String documentNo=null;
		    String itemcode=null;
		    String qtyreceived="0";
		    int flag = 0;
		    try{
		    InputStreamReader isReader = new InputStreamReader(request.getInputStream());
		    BufferedReader bReader = new BufferedReader(isReader);
		    StringBuilder strbuilder = new StringBuilder();
		    strRead = bReader.readLine();
		    while (strRead != null) {
		      strbuilder.append(strRead);
		      strRead = bReader.readLine();
		      log4j.info("Parsing box number from play Success");
		    }
		    BoxDetailsFromWS = strbuilder.toString();
		    log4j.info("Actual String received from play is:" + BoxDetailsFromWS);
		    JSONArray docdata = new JSONArray(BoxDetailsFromWS);
		      int n = docdata.length();
		      for (int i = 0; i < n; i++) {
		        final JSONObject documentNumberObj = docdata.getJSONObject(i);
		    con = PassiveDB.getInstance().getSupplyDBConnection();
		    PreparedStatement pst = null;
		    log4j.info("Searching Document number based on box number field");
		    getdocumentno = "select minout.documentno,mprod.name as itemcode from m_inout minout, m_inoutline minoutline, m_product mprod where minout.m_inout_id=minoutline.m_inout_id and mprod.m_product_id=minoutline.m_product_id and minout.documentno=?";
		    pst = con.prepareStatement(getdocumentno);
		    pst.setString(1, documentNumberObj.getString("dcnumber"));
		    getDocumentResultSet = pst.executeQuery();
		    while (getDocumentResultSet.next()) {
		    	documentNo=getDocumentResultSet.getString("documentno");
		    	itemcode=getDocumentResultSet.getString("itemcode");
		    
		    	if (!mapValue.containsKey(documentNo))
			    	  mapValue.put(documentNo, new ArrayList<String[]>());
			      
		    	  mapValue.get(documentNo).add(new String[]{itemcode,qtyreceived});
		    }
		      }
			 for(Entry<String, List<String[]>> entry: mapValue.entrySet())
			 {
				String documentno= entry.getKey();
				List<String[]> elements = new ArrayList<String[]>();
				elements.addAll(entry.getValue());			
			
					goodsReceipt = checkGrnExists(documentno);
					shipmentInOuts= new HashSet<ShipmentInOut>();
				 shipmentInOuts.add(goodsReceipt);
			      Iterator<ShipmentInOut> shipmentIterator = shipmentInOuts.iterator();

			      while (shipmentIterator.hasNext()) {
			        ShipmentInOut shipmentInOut = (ShipmentInOut) shipmentIterator.next();
			        System.out.println(shipmentInOut.getDocumentNo());
			        AddStoreStock stock = new AddStoreStock();
			        try {
			          stock.add(shipmentInOut);
			        } catch (Exception e) {
			          e.printStackTrace();
			          throw e;
			        }
			      }
			 }
		      

		    }
		    catch(Exception e)
		    {
		    	e.printStackTrace();
		    }

	}
			 private ShipmentInOutLine checkGrnLineExistsForProduct(ShipmentInOut goodsReceipt, String itemCode) {

				    Product Product = getProduct(itemCode);

				    OBCriteria<ShipmentInOutLine> criteriaOnGrn = OBDal.getInstance().createCriteria(
				        ShipmentInOutLine.class);
				    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, goodsReceipt));
				    criteriaOnGrn.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_PRODUCT, Product));

				    List<ShipmentInOutLine> inOutLines = criteriaOnGrn.list();
				    if (inOutLines != null && inOutLines.size() > 0) {
				      if (inOutLines.size() > 1)
				        throw new OBException("More than one Line for same GRN " + goodsReceipt.getDocumentNo());
				      return inOutLines.get(0);
				    }
				    throw new OBException("Grn line with Product " + Product
				        + " in Goods receipt for docuement no " + goodsReceipt.getDocumentNo() + " does not exists");
				  }
				private Product getProduct(String itemCode) {
				    OBCriteria<Product> criteriaOnPrd = OBDal.getInstance().createCriteria(Product.class);
				    criteriaOnPrd.add(Restrictions.eq(Product.PROPERTY_NAME, itemCode));

				    List<Product> prdList = criteriaOnPrd.list();
				    if (prdList != null && prdList.size() > 0) {
				      return prdList.get(0);
				    }
				    throw new OBException(" Product with item code " + itemCode + "  does not exists");
				  }
				private ShipmentInOut checkGrnExists(String documentNo) {
				    OBCriteria<ShipmentInOut> criteriaOnGrn = OBDal.getInstance().createCriteria(
				        ShipmentInOut.class);
				    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, documentNo.trim()));
				    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "CO"));
				    criteriaOnGrn.add(Restrictions.eq(ShipmentInOut.PROPERTY_IBODTRISAUTOMATIC, true));
				    List<ShipmentInOut> inOuts = criteriaOnGrn.list();
				    if (inOuts != null && inOuts.size() > 0) {
				      if (inOuts.size() > 1)
				        throw new OBException("More than one Goods receipt for same document no " + documentNo);
				      return inOuts.get(0);
				    }
				    throw new OBException("Goods receipt for document no " + documentNo
				        + "Not exists/Not Completed/Not an automatic receipt");
				  }

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub

	}

}
