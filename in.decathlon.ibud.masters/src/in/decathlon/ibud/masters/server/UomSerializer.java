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
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.service.json.DataToJsonConverter;

public class UomSerializer {

  DataToJsonConverter dataToJsonConverter = new DataToJsonConverter();
  final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
      DataToJsonConverter.class);
  public static final Logger log = Logger.getLogger(UomSerializer.class);

  public List<JSONObject> generateJsonWS(String updatedTime, int rowCount) throws JSONException,
      ParseException {

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    newDate = formater.parse(updated);

    OBCriteria<UOM> uomCriteria = OBDal.getInstance().createCriteria(UOM.class);
    uomCriteria.add(Restrictions.ge(Product.PROPERTY_UPDATED, newDate));
    uomCriteria.setMaxResults(rowCount);

    List<UOM> uomList = uomCriteria.list();
    log.debug(" There are " + uomList.size() + " UOM created since " + updated);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(uomList);

    return jsonObjects;

  }

}
