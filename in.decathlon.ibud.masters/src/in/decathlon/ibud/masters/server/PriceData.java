package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.commons.JSONHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListSchema;
import org.openbravo.model.pricing.pricelist.PriceListSchemeLine;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;

import com.sysfore.catalog.CLFOBPRICE;

public class PriceData {

  private Date getDate(String updatedTime) throws ParseException {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    newDate = formater.parse(updated);
    return newDate;
  }

  public List<JSONObject> getPriceListJson(String updatedTime1, int rowCount)
      throws ParseException, JSONException {

    OBCriteria<PriceList> priceCriteria = OBDal.getInstance().createCriteria(PriceList.class);
    priceCriteria.add(Restrictions.ge(PriceList.PROPERTY_UPDATED, getDate(updatedTime1)));

    List<PriceList> priceList = priceCriteria.list();
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(priceList);

    return jsonObjects;

  }

  public List<JSONObject> getPriceListVersionJson(String updatedTime1, int rowCount)
      throws ParseException, JSONException {

    OBCriteria<PriceListVersion> priceVersionCriteria = OBDal.getInstance().createCriteria(
        PriceListVersion.class);
    priceVersionCriteria.add(Restrictions.ge(PriceListVersion.PROPERTY_UPDATED,
        getDate(updatedTime1)));

    List<PriceListVersion> priceVersionList = priceVersionCriteria.list();
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(priceVersionList);

    return jsonObjects;

  }

  public PriceListVersion getPriceListVersion() {

    OBCriteria<PriceListVersion> priceVersionCriteria = OBDal.getInstance().createCriteria(
        PriceListVersion.class);
    priceVersionCriteria.add(Restrictions.ge(PriceListVersion.PROPERTY_NAME, "DMI CATALOGUE"));

    List<PriceListVersion> priceVersionList = priceVersionCriteria.list();
    if (priceVersionList != null && priceVersionList.size() > 0)
      return priceVersionList.get(0);
    else
      throw new OBException("No DMI Catalogue Pricelist");

  }

  PriceListVersion priceVer = getPriceListVersion();

  public List<JSONObject> getProductPriceJson(String updatedTime1, int rowCount)
      throws ParseException, JSONException {
    OBCriteria<ProductPrice> productPriceCriteria = OBDal.getInstance().createCriteria(
        ProductPrice.class);
    productPriceCriteria.add(Restrictions.ge(ProductPrice.PROPERTY_UPDATED, getDate(updatedTime1)));
    productPriceCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION, priceVer));

    List<ProductPrice> productPriceList = productPriceCriteria.list();
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(productPriceList);

    return jsonObjects;

  }

  public List<JSONObject> getClFobPriceJson(String updatedTime1, int rowCount)
      throws ParseException, JSONException {
    OBCriteria<CLFOBPRICE> clFobPriceCriteria = OBDal.getInstance()
        .createCriteria(CLFOBPRICE.class);
    clFobPriceCriteria.add(Restrictions.ge(CLFOBPRICE.PROPERTY_UPDATED, getDate(updatedTime1)));
    clFobPriceCriteria.add(Restrictions.eq(CLFOBPRICE.PROPERTY_PRICELISTVERSION, priceVer));

    List<CLFOBPRICE> clFobPriceList = clFobPriceCriteria.list();
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(clFobPriceList);

    return jsonObjects;

  }

  public List<JSONObject> getDiscountSchemaJson(String updatedTime1, int rowCount)
      throws ParseException, JSONException {
    OBCriteria<PriceListSchema> discountSchemaCriteria = OBDal.getInstance().createCriteria(
        PriceListSchema.class);
    discountSchemaCriteria.add(Restrictions.ge(PriceListSchema.PROPERTY_UPDATED,
        getDate(updatedTime1)));

    List<PriceListSchema> productPriceList = discountSchemaCriteria.list();
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(productPriceList);

    return jsonObjects;

  }

  public List<JSONObject> getDiscountSchemaLineJson(String updatedTime1, int rowCount)
      throws ParseException, JSONException {
    OBCriteria<PriceListSchemeLine> discountSchemaLineCriteria = OBDal.getInstance()
        .createCriteria(PriceListSchemeLine.class);
    discountSchemaLineCriteria.add(Restrictions.ge(PriceListSchemeLine.PROPERTY_UPDATED,
        getDate(updatedTime1)));

    List<PriceListSchemeLine> productPriceList = discountSchemaLineCriteria.list();
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(productPriceList);

    return jsonObjects;

  }
}
