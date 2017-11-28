package in.decathlon.retail.api.omniCommerce.ad_webservice;

import in.decathlon.retail.api.omniCommerce.util.OmniCommerceAPIUtility;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.web.WebService;

public class StoreWiseProductsWithPrice implements WebService {

  private static final Logger LOG = Logger.getLogger(StoreWiseProductsWithPrice.class);
  private static final OmniCommerceAPIUtility OmniCommerceAPIUtility = new OmniCommerceAPIUtility();

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    OBContext.setAdminMode();

    final String sampleRequest = request.getReader().readLine();

    final JSONObject requestObject = new JSONObject(sampleRequest);
    final String requestedStores = requestObject.getString("stores");

    final JSONObject storeWiseJsonObj = new JSONObject();
    final List<Organization> requestedStoreList = OmniCommerceAPIUtility
        .getRequestedStoresList(requestedStores);

    for (Organization store : requestedStoreList) {
      LOG.info("Store " + store.getName());
      final JSONArray itemPriceInfoJSONArray = new JSONArray();

      final OBQuery<ProductPrice> productPriceObQuery = OBDal
          .getInstance()
          .createQuery(
              ProductPrice.class,
              "as pp WHERE pp.organization.id='"
                  + store.getId()
                  + "' AND pp.product.id IN (select p.id from Product p where p.clLifestage in ('New','Active'))");
      productPriceObQuery.setFilterOnReadableOrganization(false);
      LOG.info("product count " + productPriceObQuery.count());
      if (productPriceObQuery.count() > 0) {
        for (ProductPrice productPrice : productPriceObQuery.list()) {

          final JSONObject itemPriceInfoJsonObj = new JSONObject();
          itemPriceInfoJsonObj.put("itemcode", productPrice.getProduct().getName());
          itemPriceInfoJsonObj.put("MRP", productPrice.getClMrpprice());
          itemPriceInfoJsonObj.put("Cession", productPrice.getClCessionprice());
          itemPriceInfoJsonObj.put("ccUNIT", productPrice.getClCcunitprice());
          itemPriceInfoJsonObj.put("ccUE", productPrice.getClCcueprice());
          itemPriceInfoJsonObj.put("ccPCB", productPrice.getClCcpcbprice());
          itemPriceInfoJSONArray.put(itemPriceInfoJsonObj);
        }
        // System.out.println("itemPriceInfoJSONArray->" + itemPriceInfoJSONArray.toString());
        storeWiseJsonObj.put(store.getName(), itemPriceInfoJSONArray);
      } else {
        storeWiseJsonObj.put(store.getName(), "");
      }
    }

    OBContext.restorePreviousMode();

    // Response
    response.setContentType("application/json");
    // Get the printwriter object from response to write the required json object to the output
    // stream
    PrintWriter out = response.getWriter();
    // Assuming your json object is **jsonObject**, perform the following, it will return your json
    // object
    out.print(storeWiseJsonObj);
    out.flush();
    out.close();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new UnsupportedOperationException();
  }

}
