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

import com.sysfore.catalog.CLColor;

public class ColorSerializer {

  private static final Logger log = Logger.getLogger(ColorSerializer.class);

  public List<JSONObject> generateJsonWS(String updatedTime, int rowCount) throws JSONException,
      ParseException {

    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;

    newDate = formater.parse(updated);

    OBCriteria<CLColor> colorCriteria = OBDal.getInstance().createCriteria(CLColor.class);
    colorCriteria.add(Restrictions.ge(Product.PROPERTY_UPDATED, newDate));
    colorCriteria.setMaxResults(rowCount);

    List<CLColor> colorList = colorCriteria.list();
    log.debug(" There are " + colorList.size() + " Color created since " + updated);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(colorList);

    return jsonObjects;

  }

}
