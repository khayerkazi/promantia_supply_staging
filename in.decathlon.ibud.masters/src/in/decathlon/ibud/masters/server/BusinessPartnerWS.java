package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.commons.JSONHelper;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.web.WebService;

public class BusinessPartnerWS implements WebService {

  private static final Logger log = Logger.getLogger(BusinessPartnerWS.class);

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("Dodelete not implemented ");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    String dataToJson = processRequest(path, request, response);
    writeResult(response, dataToJson);
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("Do Post not implemented");
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("Do Put not implemented");
  }

  private String processRequest(String path, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      final JSONObject jsonResponse = new JSONObject();
      List<JSONObject> partnerJson = new ArrayList<JSONObject>();
      List<JSONObject> partnerGroupJson = new ArrayList<JSONObject>();
      String updated = request.getParameter("updated");
      log.debug(" date in string format taken from query string " + updated);
      int rowCount = Integer.parseInt(request.getParameter("rowCount"));
      updated = updated.replace("_", " ");
      SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Date newDate = null;
      newDate = formater.parse(updated);
      log.debug(" date recieved from store formatted simple dateformat" + newDate);

      OBCriteria<BusinessPartner> partnerCriteria = OBDal.getInstance().createCriteria(
          BusinessPartner.class);
      partnerCriteria.add(Restrictions.ge(BusinessPartner.PROPERTY_UPDATED, newDate));
      partnerCriteria.setMaxResults(rowCount);

      List<BusinessPartner> partnerList = getUpdatedPartners(newDate, rowCount);
      log.debug(" There are " + partnerList.size() + " partners created since " + updated);

      List<Category> partnerGroupList = getUpdatedPartnerGroup(newDate, rowCount);
      log.debug("There are" + partnerGroupList.size() + " partner groups created since" + updated);

      if (partnerList.size() > 0) {
        partnerJson = JSONHelper.convertBobListToJsonList(partnerList);
      }
      if (partnerGroupList.size() > 0) {
        partnerGroupJson = JSONHelper.convertBobListToJsonList(partnerGroupList);
      }

      jsonResponse.put(JsonConstants.RESPONSE_DATA, partnerJson);
      jsonResponse.put("partnerGroup", partnerGroupJson);
      String finalResult = jsonResponse.toString();
      return finalResult;
    } catch (Exception e) {
      log.error(e.getMessage());
      return "";
    }

  }

  private List<Category> getUpdatedPartnerGroup(Date newDate, int rowCount) {
    OBCriteria<Category> catCrit = OBDal.getInstance().createCriteria(Category.class);
    catCrit.add(Restrictions.ge(Category.PROPERTY_UPDATED, newDate));
    catCrit.setMaxResults(rowCount);

    return catCrit.list();
  }

  private List<BusinessPartner> getUpdatedPartners(Date newDate, int rowCount) {
    OBCriteria<BusinessPartner> partnerCriteria = OBDal.getInstance().createCriteria(
        BusinessPartner.class);
    partnerCriteria.add(Restrictions.ge(BusinessPartner.PROPERTY_UPDATED, newDate));
    partnerCriteria.setMaxResults(rowCount);
    return partnerCriteria.list();

  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

}
