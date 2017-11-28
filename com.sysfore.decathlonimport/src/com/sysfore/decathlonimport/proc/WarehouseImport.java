package com.sysfore.decathlonimport.proc;

import org.hibernate.criterion.Restrictions;
import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

public class WarehouseImport extends IdlServiceJava {
  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("OrganizationKey", Parameter.STRING),
        new Parameter("SearchKey", Parameter.STRING), new Parameter("Warehouse", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    validator.checkNotNull(validator.checkString(values[0], 40));
    validator.checkNotNull(validator.checkString(values[1], 40), "SearchKey");
    validator.checkNotNull(validator.checkString(values[2], 40));

    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createBins((String) values[0], (String) values[1], (String) values[2]);
  }

  public BaseOBObject createBins(final String OrganizationKey, final String searchkey,
      final String warehouse) throws Exception {
    Locator locator = OBProvider.getInstance().get(Locator.class);
    Locator locatorExist = findDALInstance(false, Locator.class, new Value("searchKey", searchkey));
    if (locatorExist != null) {

      org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WarehouseImport.class);
      log.info("Bin already exists: " + searchkey);
      // logRecordError("Bin already exists: " + searchkey, searchkey);
      return locatorExist;
      /*
       * throw new OBException(Utility.messageBD(conn, "WH_Bin_Exists", vars.getLanguage()) +
       * searchkey);
       */
    } else {

      locator.setActive(true);

      final OBCriteria<Organization> obCriteria = OBDal.getInstance().createCriteria(
          Organization.class);

      obCriteria.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY, OrganizationKey));
      obCriteria.setMaxResults(1);

      Organization org = obCriteria.list().get(0);
      locator.setOrganization(org);
      locator.setSearchKey(searchkey);
      locator.setDefault(Boolean.parseBoolean("true"));

      Warehouse Warehouse = findDALInstance(false, Warehouse.class, new Value("searchKey",
          warehouse));
      if (Warehouse != null)
        locator.setWarehouse(Warehouse);
      else {
        throw new OBException(Utility.messageBD(conn, "IDL_PR_EXISTS", vars.getLanguage())
            + warehouse);
      }

      int length = searchkey.length();

      String levelZ = searchkey.substring(2, length - 3);
      String stackY = searchkey.substring(length - 1, length);
      String rowX = searchkey.substring(5, length - 1);
      if (levelZ.contains("N"))
        locator.setRelativePriority(Long.parseLong("4"));
      else
        locator.setRelativePriority(Long.parseLong("50"));

      locator.setRowX(rowX);
      locator.setStackY(stackY);
      locator.setLevelZ(levelZ);

      locator.setBarcode(searchkey);

      /*
       * Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 0);
       * cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
       * 
       * pListSchema.setValidFromDate(cal.getTime());
       */

      OBDal.getInstance().save(locator);
      OBDal.getInstance().flush();

      // End process
      OBDal.getInstance().commitAndClose();
      return locator;
    }

  }

  @Override
  protected String getEntityName() {
    // TODO Auto-generated method stub
    return null;
  }

}
