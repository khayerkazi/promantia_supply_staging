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
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.service.json.DataToJsonConverter;

import com.sysfore.catalog.CLModel;

public class ModelDataSerializer {
  DataToJsonConverter dataToJsonConverter = new DataToJsonConverter();
  public static final Logger log = Logger.getLogger(ProductCategory.class);

  public List<JSONObject> generateJsonWS(String updatedTime, int rowCount) throws JSONException,
      ParseException {

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    newDate = formater.parse(updated);

    OBCriteria<CLModel> modelCriteria = OBDal.getInstance().createCriteria(CLModel.class);
    modelCriteria.add(Restrictions.ge(Product.PROPERTY_UPDATED, newDate));
    modelCriteria.setMaxResults(rowCount);

    List<CLModel> modelCatList = modelCriteria.list();
    log.debug(" There are " + modelCatList.size() + " product categories created since " + updated);
    // List<? extends BaseOBObject> boblist = productList;
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(modelCatList);

    return jsonObjects;
  }

}
