package org.openbravo.localization.india.ingst.master.ad_Process;

import java.util.Date;

import jxl.common.Logger;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.localization.india.ingst.master.data.INGSTGSTProductCode;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.module.idljava.proc.IdlServiceJava;

public class GSTProductIDL extends IdlServiceJava {
  private static Logger log = Logger.getLogger(GSTProductIDL.class);

  @Override
  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("GSTProductCode", Parameter.STRING),
        new Parameter("Name", Parameter.STRING), new Parameter("HSN/SACType", Parameter.STRING),
        new Parameter("TaxCategory", Parameter.STRING) };
  }

  @Override
  protected String getEntityName() {
    // TODO Auto-generated method stub
    return "GSTProduct_IDL";
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    // TODO Auto-generated method stub

    validator.checkNotNull(validator.checkString(values[0], 60), "GSTProductCode");
    validator.checkNotNull(validator.checkString(values[1], 120), "Name");
    validator.checkNotNull(validator.checkString(values[2], 60), "HSN/SACType");
    validator.checkNotNull(validator.checkString(values[3], 60), "TaxCategory");

    return values;

  }

  @Override
  protected BaseOBObject internalProcess(Object... values) throws Exception {
    // TODO Auto-generated method stub
    return createGSTProductCode((String) values[0], (String) values[1], (String) values[2],
        (String) values[3]);

  }

  private BaseOBObject createGSTProductCode(String gstProductCode, String name, String hsn,
      String taxCategory) {
    try {
      INGSTGSTProductCode ingstProductCode = OBProvider.getInstance()
          .get(INGSTGSTProductCode.class);

      /*
       * OBCriteria<Organization> orgCriteria =
       * OBDal.getInstance().createCriteria(Organization.class);
       * orgCriteria.add(Restrictions.eq(Organization.PROPERTY_NAME, organization)); if
       * (orgCriteria.list().size() > 0) {
       * ingstProductCode.setOrganization(orgCriteria.list().get(0)); } else {
       * log.error("Organization does not exist"); throw new OBException(organization +
       * "Organization is not present."); }
       */

      ingstProductCode.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      ingstProductCode.setActive(true);
      ingstProductCode.setClient(OBContext.getOBContext().getCurrentClient());
      ingstProductCode.setCreatedBy(OBContext.getOBContext().getUser());
      ingstProductCode.setCreationDate(new Date());
      ingstProductCode.setUpdated(new Date());
      ingstProductCode.setUpdatedBy(OBContext.getOBContext().getUser());
      ingstProductCode.setValue(gstProductCode);
      ingstProductCode.setName(name);

      OBCriteria<TaxCategory> taxCategoryCriteria = OBDal.getInstance().createCriteria(
          TaxCategory.class);
      taxCategoryCriteria.add(Restrictions.eq(TaxCategory.PROPERTY_NAME, taxCategory));
      if (taxCategoryCriteria.list().size() > 0) {
        ingstProductCode.setTaxCategory(taxCategoryCriteria.list().get(0));
      } else {
        log.error("Tax Category does not exists");
        throw new OBException(taxCategory + "Tax Category does not exists");

      }
      if (hsn.equalsIgnoreCase("HSN Section")) {

        ingstProductCode.setHSNSACType("SEC");

      } else if (hsn.equalsIgnoreCase("HSN Chapter")) {
        ingstProductCode.setHSNSACType("CHP");

      } else if (hsn.equalsIgnoreCase("HSN Heading")) {
        ingstProductCode.setHSNSACType("HDG");
      } else if (hsn.equalsIgnoreCase("HSN-Sub-Heading")) {
        ingstProductCode.setHSNSACType("HSH");

      } else if (hsn.equalsIgnoreCase("HSN Code")) {
        ingstProductCode.setHSNSACType("HSN");

      } else if (hsn.equalsIgnoreCase("SAC Code")) {
        ingstProductCode.setHSNSACType("SAC");
      } else {
        throw new OBException(hsn + "hsn does not exists");

      }

      OBDal.getInstance().save(ingstProductCode);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log.debug("Problem with Imported Fields" + e);
      e.printStackTrace();
      throw new OBException(e);

    }
    // End Process
    OBDal.getInstance().commitAndClose();

    // TODO Auto-generated method stub
    return null;
  }

}
