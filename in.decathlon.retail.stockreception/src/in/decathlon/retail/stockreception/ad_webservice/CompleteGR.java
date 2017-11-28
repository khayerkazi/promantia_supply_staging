package in.decathlon.retail.stockreception.ad_webservice;

import in.decathlon.ibud.transfer.ad_process.EnhancedProcessGoods;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.web.WebService;

public class CompleteGR extends Thread implements WebService {
  static Logger log4j = Logger.getLogger(CompleteGR.class);
  static HashSet<ShipmentInOut> shipmentInOuts = null;

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    String strRead = "";
    String flag = "false";
    JSONArray jsonArray = new JSONArray();
    JSONObject jsonObject = new JSONObject();
    String BoxWS = "";
    ProcessLogger logger;
    log4j.info("Entering Into DC Complete Part");
    try {

      InputStreamReader isReader = new InputStreamReader(request.getInputStream());
      BufferedReader bReader = new BufferedReader(isReader);
      StringBuilder strBuilder = new StringBuilder();
      strRead = bReader.readLine();
      while (strRead != null) {
        strBuilder.append(strRead);
        strRead = bReader.readLine();
        log4j.info("Parsing Success");
      }

      String[] valueFromWS = strBuilder.toString().split("@@&");
      BoxWS = valueFromWS[0];
      String email = valueFromWS[1];
      BoxWS = StringUtils.substringBetween(BoxWS, "{", "}"); // remove curly brackets
      String[] keyValuePairs = BoxWS.split(","); // split the string to creat key-value pairs
      Map<String, String> mapValue = new HashMap<String, String>();
      for (String pair : keyValuePairs) // iterate over the pais
      {
        String[] entry = pair.split("="); // split the pairs to get key and value
        mapValue.put(entry[0].trim(), entry[1].trim()); // add them to the hashmap
      }
      List<String> Boxes = new ArrayList<String>();
      for (Entry<String, String> entry : mapValue.entrySet()) {
        String boxNumber = entry.getKey();
        String value = entry.getValue();
        List<ShipmentInOut> inoutList = null;
        ShipmentInOut inoutObj;
        List<ShipmentInOut> ShipInOutList = new ArrayList<ShipmentInOut>();
        OBCriteria<ShipmentInOut> criteriashipmentinout = OBDal.getInstance().createCriteria(
            ShipmentInOut.class);
        criteriashipmentinout.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "DR"));
        criteriashipmentinout.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, boxNumber));
        inoutList = criteriashipmentinout.list();
        Integer inoutCount1 = inoutList.size();
        if (inoutCount1 > 0) {
          log4j.info("Dcs are Completed ");
          for (ShipmentInOut InoutIterator : inoutList) {
            try {
              inoutObj = InoutIterator;
              System.out.println(inoutObj.getDocumentNo());
              ShipInOutList.add(inoutObj);

              EnhancedProcessGoods epg = new EnhancedProcessGoods();
              epg.processReceipt(inoutObj);
              flag = "true";

            }

            catch (Exception e) {
              flag = "false";
              log4j.info("Error during closing of" + boxNumber);
              e.printStackTrace();

            }
          }
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      flag = "false";
      log4j.info("Error In completing Dc ");
    }

    jsonObject.put("status", flag);
    jsonArray.put(jsonObject);
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(jsonArray.toString());
    w.close();

  }

  private static ShipmentInOut checkGrnExists(String documentNo) {
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

  private static ShipmentInOutLine checkGrnLineExistsForProduct(ShipmentInOut goodsReceipt,
      String itemCode) {

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

  private static Product getProduct(String itemCode) {
    OBCriteria<Product> criteriaOnPrd = OBDal.getInstance().createCriteria(Product.class);
    criteriaOnPrd.add(Restrictions.eq(Product.PROPERTY_NAME, itemCode));

    List<Product> prdList = criteriaOnPrd.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    }
    throw new OBException(" Product with item code " + itemCode + "  does not exists");
  }

  private User getUser(String email) {

    OBCriteria<User> userCrit = OBDal.getInstance().createCriteria(User.class);
    userCrit.add(Restrictions.eq(User.PROPERTY_EMAIL, email));
    List<User> userCritList = userCrit.list();
    if (userCritList != null && userCritList.size() > 0) {
      return userCritList.get(0);
    } else {
      throw new OBException("user not found");
    }
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
