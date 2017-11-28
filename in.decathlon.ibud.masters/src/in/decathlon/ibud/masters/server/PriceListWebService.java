package in.decathlon.ibud.masters.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListSchema;
import org.openbravo.model.pricing.pricelist.PriceListSchemeLine;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.service.web.WebService;

import com.sysfore.catalog.CLFOBPRICE;

/*
 * 
 * Push Pricelist data from supply to user
 * 
 */

public class PriceListWebService implements WebService {

  PriceData otherPriceData = new PriceData();
  GeneralMasterSerializer genMaster;
  private static final Logger log = Logger.getLogger(PriceListWebService.class);

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doDelete not implemented");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    getPriceListDataInJson(path, request, response);
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not Implemented");
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPut not Implemented");
  }

  private void getPriceListDataInJson(String path, HttpServletRequest request,
      HttpServletResponse response) throws Exception {

    genMaster = new GeneralMasterSerializer(response);

    try {
      int rowCount = Integer.parseInt(request.getParameter("rowCount"));
      String updated = request.getParameter("updated");
      log.debug(" date in string format taken from query string " + updated);
      updated = updated.replace("_", " ");

      genMaster.generateJsonWS(PriceList.class, updated, rowCount, "Price", null);
      genMaster.generateJsonWS(PriceListSchema.class, updated, rowCount, "Price", null);
      genMaster.generateJsonWS(PriceListSchemeLine.class, updated, rowCount, "Price", null);
      genMaster.generateJsonWS(PriceListVersion.class, updated, rowCount, "Price", null);
      genMaster.getProductPriceData(updated);
      genMaster.generateJsonWS(CLFOBPRICE.class, updated, rowCount, "Price", null);

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e);
    }
  }
}