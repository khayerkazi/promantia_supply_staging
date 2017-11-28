package in.decathlon.ibud.masters.server;

import in.decathlon.ibud.commons.JSONHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxCategory;

public class TaxCategorySerializer {
  public static final Logger log = Logger.getLogger(TaxCategorySerializer.class);

  public List<JSONObject> generateJsonWS(String updatedTime, int rowCount) throws JSONException,
      ParseException {

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    newDate = formater.parse(updated);
    OBCriteria<TaxCategory> taxCatCriteria = OBDal.getInstance().createCriteria(TaxCategory.class);
    taxCatCriteria.add(Restrictions.ge(Product.PROPERTY_UPDATED, newDate));
    taxCatCriteria.setMaxResults(rowCount);

    List<TaxCategory> taxList = taxCatCriteria.list();
    log.debug(" There are " + taxList.size() + " Tax categories created since " + updated);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(taxList);

    return jsonObjects;

  }

}
