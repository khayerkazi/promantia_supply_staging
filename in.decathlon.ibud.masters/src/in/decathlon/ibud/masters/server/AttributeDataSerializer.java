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
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.AttributeUse;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.pricing.pricelist.PriceList;

public class AttributeDataSerializer {

  private static final Logger log = Logger.getLogger(AttributeDataSerializer.class);

  private Date getDate(String updatedTime) throws ParseException {
    String updated = updatedTime;
    updated = updated.replace("_", " ");
    SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date newDate = null;
    newDate = formater.parse(updated);
    return newDate;
  }

  public List<JSONObject> getAttribute(String updatedTime1, int rowCount) throws ParseException,
      JSONException {

    OBCriteria<Attribute> attributeCriteria = OBDal.getInstance().createCriteria(Attribute.class);
    attributeCriteria.add(Restrictions.ge(PriceList.PROPERTY_UPDATED, getDate(updatedTime1)));
    attributeCriteria.setMaxResults(rowCount);

    List<Attribute> attributeList = attributeCriteria.list();
    log.debug(" There are " + attributeList.size() + " Attribute created since " + updatedTime1);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(attributeList);

    return jsonObjects;

  }

  public List<JSONObject> getAttributeSetInstance(String updatedTime1, int rowCount)
      throws JSONException, ParseException {

    StringBuffer hql = new StringBuffer("");
    hql.append("1=1");
    OBQuery<AttributeSetInstance> attSetInsQry = OBDal.getInstance().createQuery(
        AttributeSetInstance.class, hql.toString());

    List<AttributeSetInstance> attSetInsList = attSetInsQry.list();

    log.debug(" There are " + attSetInsList.size() + " AttributeSetInstance created since "
        + updatedTime1);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(attSetInsList);

    return jsonObjects;
  }

  public List<JSONObject> getAttributeSet(String updatedTime1, int rowCount) throws ParseException,
      JSONException {
    OBCriteria<AttributeSet> attrSetCrit = OBDal.getInstance().createCriteria(AttributeSet.class);
    attrSetCrit.add(Restrictions.ge(AttributeSet.PROPERTY_UPDATED, getDate(updatedTime1)));
    attrSetCrit.setMaxResults(rowCount);
    List<AttributeSet> attrSetList = attrSetCrit.list();
    log.debug(" There are " + attrSetList.size() + " AttributeSet created since " + updatedTime1);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(attrSetList);
    return jsonObjects;
  }

  public List<JSONObject> getAttributeInstance(String updatedTime1, int rowCount)
      throws ParseException, JSONException {
    OBCriteria<AttributeInstance> attrInsCrit = OBDal.getInstance().createCriteria(
        AttributeInstance.class);
    attrInsCrit.add(Restrictions.ge(AttributeInstance.PROPERTY_UPDATED, getDate(updatedTime1)));
    attrInsCrit.setMaxResults(rowCount);
    List<AttributeInstance> attrInsList = attrInsCrit.list();
    log.debug(" There are " + attrInsList.size() + " AttributeInstance created since "
        + updatedTime1);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(attrInsList);
    return jsonObjects;

  }

  public List<JSONObject> getAttributeUse(String updatedTime1, int rowCount) throws JSONException,
      ParseException {
    OBCriteria<AttributeUse> attrUseCrit = OBDal.getInstance().createCriteria(AttributeUse.class);
    attrUseCrit.add(Restrictions.ge(AttributeUse.PROPERTY_UPDATED, getDate(updatedTime1)));
    attrUseCrit.setMaxResults(rowCount);

    List<AttributeUse> attrUseList = attrUseCrit.list();
    log.debug(" There are " + attrUseList.size() + " AttributeUse created since " + updatedTime1);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(attrUseList);

    return jsonObjects;
  }

  public List<JSONObject> getAttributeValue(String updatedTime1, int rowCount)
      throws ParseException, JSONException {
    OBCriteria<AttributeValue> attrValueCrit = OBDal.getInstance().createCriteria(
        AttributeValue.class);
    attrValueCrit.add(Restrictions.ge(AttributeValue.PROPERTY_UPDATED, getDate(updatedTime1)));
    attrValueCrit.setMaxResults(rowCount);
    List<AttributeValue> attrValueList = attrValueCrit.list();
    log.debug(" There are " + attrValueList.size() + " AttributeValue created since "
        + updatedTime1);
    final List<JSONObject> jsonObjects = JSONHelper.convertBobListToJsonList(attrValueList);
    return jsonObjects;
  }
}
