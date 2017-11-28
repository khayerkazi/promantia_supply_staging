package org.openbravo.localization.india.ingst.master.ad_actionbutton;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.localization.india.ingst.master.data.INGSTGSTProductCode;
import org.openbravo.localization.india.ingst.master.data.ingstRateException;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.tax.TaxZone;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class GSTProductException extends DalBaseProcess {
  OBError message = new OBError();
  private static String GST_PRODUCTVALUE;
  private static final Logger log = Logger.getLogger(GSTProductException.class);
  private boolean isReprocess = false;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    String gstProductBundleId = (String) bundle.getParams().get("Ingst_Gstproductcode_ID");
    INGSTGSTProductCode gstProductObj = OBDal.getInstance().get(INGSTGSTProductCode.class,
        gstProductBundleId);

    TaxCategory taxCategoryObj = gstProductObj.getTaxCategory();
    if (taxCategoryObj == null) {
      message = setErrorMessage("GST Tax Category not Found! ");
    } else {
      try {
        List<ingstRateException> gstRateExceptionObjList = getTaxExceptionListObj(gstProductObj);

        if (gstRateExceptionObjList.size() == 0) {
          message = setErrorMessage("Non Processed Exception Rate Found!");

        } else {
          GST_PRODUCTVALUE = gstProductObj.getValue().replaceAll("\\s", "");
          TaxCategory copyOfTaxCategoryObj = saveCopyOfTaxCategoryData(taxCategoryObj,
              gstProductObj);
          if (!(copyOfTaxCategoryObj == null)) {
            if (isReprocess) { // it's running only one time for copyOf record
              saveCopyOfTaxRateAndZone(taxCategoryObj, copyOfTaxCategoryObj);
            }
            for (ingstRateException gstRateExceptionObj : gstRateExceptionObjList) {
              TaxRate gstSGSTCopyOfTaxRateObj = null;
              List<TaxRate> gstSGSTCopyTaxRateObjList = getSGSTTaxRateObjList(copyOfTaxCategoryObj);
              if (gstSGSTCopyTaxRateObjList.size() > 0) {
                gstSGSTCopyOfTaxRateObj = gstSGSTCopyTaxRateObjList.get(0);

                TaxRate gstParentTaxRateExceptionObj = saveParentTaxRateException(
                    gstRateExceptionObj, gstSGSTCopyOfTaxRateObj.getParentTaxRate());
                if (gstParentTaxRateExceptionObj != null) {
                  OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(
                      TaxRate.class);
                  gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,
                      copyOfTaxCategoryObj));
                  gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_PARENTTAXRATE,
                      gstSGSTCopyOfTaxRateObj.getParentTaxRate()));

                  List<TaxRate> gstTaxCategoryObjList = gstTaxRateCriteria.list();
                  for (TaxRate taxRateObj : gstTaxCategoryObjList) {

                    saveSGSTRateExceptionData(copyOfTaxCategoryObj, gstRateExceptionObj,
                        taxRateObj, gstParentTaxRateExceptionObj);
                  }

                }
              }

              deleteTaxZoneRegionRecord(gstRateExceptionObj,
                  gstSGSTCopyOfTaxRateObj.getParentTaxRate());
              gstRateExceptionObj.setProcessed(true);
              OBDal.getInstance().save(gstRateExceptionObj);
              OBDal.getInstance().flush();

            }

            gstProductObj.setException(true);
            gstProductObj.setTaxCategory(copyOfTaxCategoryObj);
            OBDal.getInstance().save(gstProductObj);
            OBDal.getInstance().flush();

            message = setSuccessMessage();
          }
        }
      } catch (Exception e) {
        message = setErrorMessage(e.getMessage());

      }
    }

    bundle.setResult(message);
  }

  private TaxRate saveParentTaxRateException(ingstRateException gstRateExceptionObj,
      TaxRate gstSGSTParentTaxRateObj) {
    TaxRate copyOfParentTaxRateObj = null;

    OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,
        gstSGSTParentTaxRateObj.getTaxCategory()));

    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_DESTINATIONREGION,
        gstRateExceptionObj.getRegisteredState()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_REGION,
        gstRateExceptionObj.getRegisteredState()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_COUNTRY, getCountryObj()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_DESTINATIONCOUNTRY, getCountryObj()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_SUMMARYLEVEL, true));

    gstTaxRateCriteria.add(Restrictions.isNull(TaxRate.PROPERTY_INTXINDIANTAXCATEGORY));
    gstTaxRateCriteria.add(Restrictions.isNull(TaxRate.PROPERTY_PARENTTAXRATE));

    List<TaxRate> gstTaxCategoryObjList = gstTaxRateCriteria.list();
    if (gstTaxCategoryObjList.size() > 0) {
      copyOfParentTaxRateObj = gstTaxCategoryObjList.get(0);

      copyOfParentTaxRateObj.setUpdated(new Date());

    } else {

      copyOfParentTaxRateObj = OBProvider.getInstance().get(TaxRate.class);

      copyOfParentTaxRateObj.setUpdated(new Date());
      copyOfParentTaxRateObj.setUpdatedBy(OBContext.getOBContext().getUser());
      copyOfParentTaxRateObj.setCreatedBy(OBContext.getOBContext().getUser());
      copyOfParentTaxRateObj.setClient(OBContext.getOBContext().getCurrentClient());
      copyOfParentTaxRateObj.setCreationDate(new Date());
      copyOfParentTaxRateObj.setOrganization(gstSGSTParentTaxRateObj.getOrganization());
      copyOfParentTaxRateObj.setActive(true);

      copyOfParentTaxRateObj.setName(gstSGSTParentTaxRateObj.getName() + " - "
          + gstRateExceptionObj.getRegisteredState().getSearchKey() + " - "
          + gstRateExceptionObj.getRegisteredState().getIngstStatecode());

      copyOfParentTaxRateObj.setDescription(gstSGSTParentTaxRateObj.getDescription());
      copyOfParentTaxRateObj.setIngstLink(gstSGSTParentTaxRateObj);
      copyOfParentTaxRateObj.setTaxCategory(gstSGSTParentTaxRateObj.getTaxCategory());
      copyOfParentTaxRateObj.setValidFromDate(gstSGSTParentTaxRateObj.getValidFromDate());
      copyOfParentTaxRateObj.setSummaryLevel(gstSGSTParentTaxRateObj.isSummaryLevel());
      // copyOfParentTaxRateObj.setRate(gstRateExceptionObj.getRate());
      copyOfParentTaxRateObj.setRate(BigDecimal.ZERO);
      copyOfParentTaxRateObj.setCountry(getCountryObj());
      copyOfParentTaxRateObj.setRegion(gstRateExceptionObj.getRegisteredState());
      copyOfParentTaxRateObj.setDocTaxAmount(gstSGSTParentTaxRateObj.getDocTaxAmount());
      copyOfParentTaxRateObj.setBusinessPartnerTaxCategory(gstSGSTParentTaxRateObj
          .getBusinessPartnerTaxCategory());
      copyOfParentTaxRateObj.setDeductableRate(gstSGSTParentTaxRateObj.getDeductableRate());
      copyOfParentTaxRateObj.setCascade(gstSGSTParentTaxRateObj.isCascade());
      copyOfParentTaxRateObj.setLineNo(gstSGSTParentTaxRateObj.getLineNo());
      copyOfParentTaxRateObj.setDeductableRate(gstSGSTParentTaxRateObj.getDeductableRate());
      if (gstSGSTParentTaxRateObj.isTaxExempt())
        copyOfParentTaxRateObj.isTaxExempt();
      if (gstSGSTParentTaxRateObj.isNotTaxable())
        copyOfParentTaxRateObj.isNotTaxable();
      if (gstSGSTParentTaxRateObj.isWithholdingTax())
        copyOfParentTaxRateObj.isWithholdingTax();
      if (gstSGSTParentTaxRateObj.isTaxdeductable())
        copyOfParentTaxRateObj.isTaxdeductable();
      if (gstSGSTParentTaxRateObj.isNoVAT())
        copyOfParentTaxRateObj.isNoVAT();
      if (gstSGSTParentTaxRateObj.isDefault())
        copyOfParentTaxRateObj.isDefault();
      if (gstSGSTParentTaxRateObj.isCashVAT())
        copyOfParentTaxRateObj.isCashVAT();

      copyOfParentTaxRateObj.setIntxIndianTaxcategory(gstSGSTParentTaxRateObj
          .getIntxIndianTaxcategory());
      copyOfParentTaxRateObj.setTaxBase(gstSGSTParentTaxRateObj.getTaxBase());
      copyOfParentTaxRateObj.setBaseAmount(gstSGSTParentTaxRateObj.getBaseAmount());
      copyOfParentTaxRateObj.setDestinationCountry(getCountryObj());
      copyOfParentTaxRateObj.setDestinationRegion(gstRateExceptionObj.getRegisteredState());
      copyOfParentTaxRateObj.setSalesPurchaseType(gstSGSTParentTaxRateObj.getSalesPurchaseType());
      copyOfParentTaxRateObj.setTaxSearchKey(gstSGSTParentTaxRateObj.getTaxSearchKey());
      copyOfParentTaxRateObj.setOriginalRate(gstSGSTParentTaxRateObj.getOriginalRate());
      copyOfParentTaxRateObj.setNotTaxable(gstSGSTParentTaxRateObj.isNotTaxable());
    }
    OBDal.getInstance().save(copyOfParentTaxRateObj);
    OBDal.getInstance().flush();

    return copyOfParentTaxRateObj;
  }

  private boolean taxNameFlag(String name, TaxRate taxRate, TaxRate gstParentTaxRateExceptionObj) {
    boolean flag = true;
    OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(TaxRate.class);

    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_PARENTTAXRATE,
        gstParentTaxRateExceptionObj));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_NAME, name));
    gstTaxRateCriteria
        .add(Restrictions.eq(TaxRate.PROPERTY_ORGANIZATION, taxRate.getOrganization()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_CLIENT, taxRate.getClient()));
    List<TaxRate> gstTaxCategoryObjList = gstTaxRateCriteria.list();
    for (TaxRate taxRateObj : gstTaxCategoryObjList) {
      flag = false;

    }
    return flag;
  }

  private void deleteTaxZoneRegionRecord(ingstRateException gstRateExceptionObj,
      TaxRate gstParentTaxRateObj) {
    OBCriteria<TaxZone> gstTaxZoneCriteria = OBDal.getInstance().createCriteria(TaxZone.class);
    gstTaxZoneCriteria.add(Restrictions.eq(TaxZone.PROPERTY_TAX, gstParentTaxRateObj));
    gstTaxZoneCriteria.add(Restrictions.eq(TaxZone.PROPERTY_DESTINATIONREGION,
        gstRateExceptionObj.getRegisteredState()));

    gstTaxZoneCriteria.add(Restrictions.eq(TaxZone.PROPERTY_FROMREGION,
        gstRateExceptionObj.getRegisteredState()));

    List<TaxZone> gstTaxZoneObjList = gstTaxZoneCriteria.list();
    for (TaxZone gstTaxZoneObj : gstTaxZoneObjList) {

      OBDal.getInstance().getSession().delete(gstTaxZoneObj);
      OBDal.getInstance().flush();
    }
  }

  private List<TaxRate> getSGSTTaxRateObjList(TaxCategory copyOfTaxCategoryObj) {
    OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
    /*
     * gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, copyOfTaxCategoryObj));
     * 
     * gstTaxRateCriteria.add(Restrictions.or(
     * Restrictions.eq(TaxRate.PROPERTY_INTXINDIANTAXCATEGORY, "INGST_SGST"),
     * Restrictions.eq(TaxRate.PROPERTY_INTXINDIANTAXCATEGORY, "INGST_CGST")));
     * gstTaxRateCriteria.add(Restrictions.isNotNull(TaxRate.PROPERTY_PARENTTAXRATE)); List<TaxRate>
     * gstTaxCategoryObjList = gstTaxRateCriteria.list(); // return gstTaxCategoryObjList;
     */
    String hql = " from FinancialMgmtTaxRate rate where rate.ingstLink IN (select id from FinancialMgmtTaxRate where ingstLink is null) "
        + " and rate.intxIndianTaxcategory ='INGST_SGST' and rate.parentTaxRate IS NOT NULL and taxCategory=:taxCategory";

    Session session = OBDal.getInstance().getSession();
    org.hibernate.Query query = session.createQuery(hql);
    query.setParameter("taxCategory", copyOfTaxCategoryObj);

    return query.list();

  }

  private TaxRate saveSGSTRateExceptionData(TaxCategory copyOfTaxCategoryObj,
      ingstRateException gstRateExceptionObj, TaxRate taxRateObj,
      TaxRate gstParentTaxRateExceptionObj) {

    OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, copyOfTaxCategoryObj));

    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_DESTINATIONREGION,
        gstRateExceptionObj.getRegisteredState()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_REGION,
        gstRateExceptionObj.getRegisteredState()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_COUNTRY, getCountryObj()));
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_DESTINATIONCOUNTRY, getCountryObj()));
    if (taxRateObj.getIntxIndianTaxcategory().equals("INGST_SGST")) {
      gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_VALIDFROMDATE,
          gstRateExceptionObj.getValidFromDate()));
    }
    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_INTXINDIANTAXCATEGORY,
        taxRateObj.getIntxIndianTaxcategory()));

    TaxRate sgstTaxRateObj = null;
    List<TaxRate> gstTaxCategoryObjList = gstTaxRateCriteria.list();
    if (gstTaxCategoryObjList.size() > 0) {
      sgstTaxRateObj = gstTaxCategoryObjList.get(0);
      if (taxRateObj.getIntxIndianTaxcategory().equals("INGST_SGST")) {
        sgstTaxRateObj.setRate(gstRateExceptionObj.getRate());
      }
      sgstTaxRateObj.setUpdated(new Date());

    } else {
      sgstTaxRateObj = OBProvider.getInstance().get(TaxRate.class);

      saveTaxRate(copyOfTaxCategoryObj, gstRateExceptionObj, sgstTaxRateObj, taxRateObj,
          gstParentTaxRateExceptionObj);

    }
    OBDal.getInstance().save(sgstTaxRateObj);
    OBDal.getInstance().flush();
    return sgstTaxRateObj;
  }

  private void saveTaxRate(TaxCategory copyOfTaxCategoryObj,
      ingstRateException gstRateExceptionObj, TaxRate sgstTaxRateObj, TaxRate taxRateObj,
      TaxRate gstParentTaxRateExceptionObj) {
    sgstTaxRateObj.setUpdated(new Date());
    sgstTaxRateObj.setUpdatedBy(taxRateObj.getUpdatedBy());
    sgstTaxRateObj.setCreatedBy(taxRateObj.getCreatedBy());
    sgstTaxRateObj.setClient(taxRateObj.getClient());
    sgstTaxRateObj.setCreationDate(new Date());
    sgstTaxRateObj.setOrganization(taxRateObj.getOrganization());
    sgstTaxRateObj.setActive(true);
    String[] taxsplit = taxRateObj.getIntxIndianTaxcategory().split("_");

    boolean flag = taxNameFlag(taxsplit[1] + " - " + GST_PRODUCTVALUE + " - "
        + gstRateExceptionObj.getRegisteredState().getSearchKey()
        + gstRateExceptionObj.getRegisteredState().getIngstStatecode(), taxRateObj,
        gstParentTaxRateExceptionObj);
    if (flag) {
      sgstTaxRateObj.setName(taxsplit[1] + " - " + GST_PRODUCTVALUE + " - "
          + gstRateExceptionObj.getRegisteredState().getSearchKey()
          + gstRateExceptionObj.getRegisteredState().getIngstStatecode());
    } else {
      DateFormat df2 = new SimpleDateFormat("ddMMMyy");
      String dateToString = df2.format(gstRateExceptionObj.getValidFromDate());
      sgstTaxRateObj.setName(taxsplit[1] + " - " + GST_PRODUCTVALUE + " - "
          + gstRateExceptionObj.getRegisteredState().getSearchKey()
          + gstRateExceptionObj.getRegisteredState().getIngstStatecode() + "- " + dateToString);
    }

    if (taxsplit[1].equals("SGST")) {
      sgstTaxRateObj.setValidFromDate(gstRateExceptionObj.getValidFromDate());
      sgstTaxRateObj.setRate(gstRateExceptionObj.getRate());

    } else {
      sgstTaxRateObj.setValidFromDate(taxRateObj.getValidFromDate());
      sgstTaxRateObj.setRate(taxRateObj.getRate());

    }
    sgstTaxRateObj.setValidFromDate(gstRateExceptionObj.getValidFromDate());

    sgstTaxRateObj.setDestinationRegion(gstRateExceptionObj.getRegisteredState());
    sgstTaxRateObj.setRegion(gstRateExceptionObj.getRegisteredState());
    sgstTaxRateObj.setDestinationCountry(getCountryObj());
    sgstTaxRateObj.setCountry(getCountryObj());
    sgstTaxRateObj.setTaxCategory(copyOfTaxCategoryObj);
    sgstTaxRateObj.setIntxIndianTaxcategory(taxRateObj.getIntxIndianTaxcategory());
    sgstTaxRateObj.setParentTaxRate(gstParentTaxRateExceptionObj);
    sgstTaxRateObj.setIngstLink(gstParentTaxRateExceptionObj);

    /*
     * sgstTaxRateObj.setDescription(gstTaxRateObj.getDescription());
     * 
     * sgstTaxRateObj.setSummaryLevel(gstTaxRateObj.isSummaryLevel());
     * sgstTaxRateObj.setCountry(getCountryObj());
     * sgstTaxRateObj.setDocTaxAmount(gstTaxRateObj.getDocTaxAmount());
     * sgstTaxRateObj.setBusinessPartnerTaxCategory (gstTaxRateObj.getBusinessPartnerTaxCategory());
     * sgstTaxRateObj.setDeductableRate(gstTaxRateObj.getDeductableRate());
     * sgstTaxRateObj.setCascade(gstTaxRateObj.isCascade());
     * sgstTaxRateObj.setLineNo(gstTaxRateObj.getLineNo());
     * sgstTaxRateObj.setDeductableRate(gstTaxRateObj.getDeductableRate()); if
     * (gstTaxRateObj.isTaxExempt()) sgstTaxRateObj.isTaxExempt(); if (gstTaxRateObj.isNotTaxable())
     * sgstTaxRateObj.isNotTaxable(); if (gstTaxRateObj.isWithholdingTax())
     * sgstTaxRateObj.isWithholdingTax(); if (gstTaxRateObj.isTaxdeductable())
     * sgstTaxRateObj.isTaxdeductable(); if (gstTaxRateObj.isNoVAT()) sgstTaxRateObj.isNoVAT(); if
     * (gstTaxRateObj.isDefault()) sgstTaxRateObj.isDefault(); if (gstTaxRateObj.isCashVAT())
     * sgstTaxRateObj.isCashVAT();
     * 
     * sgstTaxRateObj.setIntxIndianTaxcategory(gstTaxRateObj.getIntxIndianTaxcategory());
     * sgstTaxRateObj.setTaxBase(gstTaxRateObj.getTaxBase());
     * sgstTaxRateObj.setBaseAmount(gstTaxRateObj.getBaseAmount());
     * sgstTaxRateObj.setDestinationCountry(getCountryObj());
     * sgstTaxRateObj.setSalesPurchaseType(gstTaxRateObj.getSalesPurchaseType());
     * sgstTaxRateObj.setTaxSearchKey(gstTaxRateObj.getTaxSearchKey());
     * sgstTaxRateObj.setOriginalRate(gstTaxRateObj.getOriginalRate());
     * sgstTaxRateObj.setNotTaxable(gstTaxRateObj.isNotTaxable());
     */
  }

  private List<ingstRateException> getTaxExceptionListObj(INGSTGSTProductCode gstProductObj) {

    OBCriteria<ingstRateException> gstTaxRateCriteria = OBDal.getInstance().createCriteria(
        ingstRateException.class);

    gstTaxRateCriteria.add(Restrictions.eq(ingstRateException.PROPERTY_INGSTGSTPRODUCTCODE,
        gstProductObj));

    gstTaxRateCriteria.add(Restrictions.eq(ingstRateException.PROPERTY_PROCESSED, false));
    List<ingstRateException> gstTaxRateObjList = gstTaxRateCriteria.list();

    return gstTaxRateObjList;
  }

  private void saveCopyOfTaxRateAndZone(TaxCategory taxCategoryObj, TaxCategory copyOfTaxCategoryObj) {

    try {

      OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(TaxRate.class);
      gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, taxCategoryObj));
      gstTaxRateCriteria.addOrder(Order.desc(TaxRate.PROPERTY_PARENTTAXRATE));

      List<TaxRate> gstTaxRateObjList = gstTaxRateCriteria.list();
      for (TaxRate gstTaxRateObj : gstTaxRateObjList) {

        TaxRate copyOfTaxRateObj = saveCopyOfTaxRateData(copyOfTaxCategoryObj, gstTaxRateObj);

        OBCriteria<TaxZone> gstTaxZoneCriteria = OBDal.getInstance().createCriteria(TaxZone.class);
        gstTaxZoneCriteria.add(Restrictions.eq(TaxZone.PROPERTY_TAX, gstTaxRateObj));
        List<TaxZone> gstTaxZoneObjList = gstTaxZoneCriteria.list();
        for (TaxZone gstTaxZoneObj : gstTaxZoneObjList) {
          saveCopyOfTaxZoneData(copyOfTaxRateObj, gstTaxZoneObj);
        }
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }
  }

  private void saveCopyOfTaxZoneData(TaxRate copyOfTaxRateObj, TaxZone gstTaxTaxZoneObj) {

    TaxZone copyOfTaxZoneObj = OBProvider.getInstance().get(TaxZone.class);

    copyOfTaxZoneObj.setUpdated(new Date());
    copyOfTaxZoneObj.setUpdatedBy(OBContext.getOBContext().getUser());
    copyOfTaxZoneObj.setCreatedBy(OBContext.getOBContext().getUser());
    copyOfTaxZoneObj.setClient(OBContext.getOBContext().getCurrentClient());
    copyOfTaxZoneObj.setCreationDate(new Date());
    copyOfTaxZoneObj.setOrganization(gstTaxTaxZoneObj.getOrganization());
    copyOfTaxZoneObj.setActive(true);

    copyOfTaxZoneObj.setDestinationCountry(gstTaxTaxZoneObj.getDestinationCountry());
    copyOfTaxZoneObj.setDestinationRegion(gstTaxTaxZoneObj.getDestinationRegion());
    copyOfTaxZoneObj.setTax(copyOfTaxRateObj);
    copyOfTaxZoneObj.setFromCountry(gstTaxTaxZoneObj.getFromCountry());
    copyOfTaxZoneObj.setFromRegion(gstTaxTaxZoneObj.getFromRegion());

    OBDal.getInstance().save(copyOfTaxZoneObj);
    OBDal.getInstance().flush();

  }

  private TaxRate saveCopyOfTaxRateData(TaxCategory copyOfTaxCategoryObj, TaxRate gstTaxRateObj) {

    TaxRate copyOfTaxRateObj = OBProvider.getInstance().get(TaxRate.class);

    copyOfTaxRateObj.setUpdated(new Date());
    copyOfTaxRateObj.setUpdatedBy(OBContext.getOBContext().getUser());
    copyOfTaxRateObj.setCreatedBy(OBContext.getOBContext().getUser());
    copyOfTaxRateObj.setClient(OBContext.getOBContext().getCurrentClient());
    copyOfTaxRateObj.setCreationDate(new Date());
    copyOfTaxRateObj.setOrganization(gstTaxRateObj.getOrganization());
    copyOfTaxRateObj.setActive(true);
    copyOfTaxRateObj.setIngstLink(gstTaxRateObj);
    copyOfTaxRateObj.setName(gstTaxRateObj.getName() + " - " + GST_PRODUCTVALUE);
    copyOfTaxRateObj.setDescription(gstTaxRateObj.getDescription());
    copyOfTaxRateObj.setIngstLink(gstTaxRateObj);
    copyOfTaxRateObj.setTaxCategory(copyOfTaxCategoryObj);

    copyOfTaxRateObj.setValidFromDate(gstTaxRateObj.getValidFromDate());
    copyOfTaxRateObj.setRate(gstTaxRateObj.getRate());

    copyOfTaxRateObj.setSummaryLevel(gstTaxRateObj.isSummaryLevel());
    if (gstTaxRateObj.getParentTaxRate() != null)
      copyOfTaxRateObj
          .setParentTaxRate(getCopyOfParentTaxRateObj(gstTaxRateObj.getParentTaxRate()));
    copyOfTaxRateObj.setCountry(getCountryObj());
    copyOfTaxRateObj.setRegion(gstTaxRateObj.getRegion());
    copyOfTaxRateObj.setDocTaxAmount(gstTaxRateObj.getDocTaxAmount());
    copyOfTaxRateObj.setBusinessPartnerTaxCategory(gstTaxRateObj.getBusinessPartnerTaxCategory());
    copyOfTaxRateObj.setDeductableRate(gstTaxRateObj.getDeductableRate());
    copyOfTaxRateObj.setCascade(gstTaxRateObj.isCascade());
    copyOfTaxRateObj.setLineNo(gstTaxRateObj.getLineNo());
    copyOfTaxRateObj.setDeductableRate(gstTaxRateObj.getDeductableRate());
    if (gstTaxRateObj.isTaxExempt())
      copyOfTaxRateObj.isTaxExempt();
    if (gstTaxRateObj.isNotTaxable())
      copyOfTaxRateObj.isNotTaxable();
    if (gstTaxRateObj.isWithholdingTax())
      copyOfTaxRateObj.isWithholdingTax();
    if (gstTaxRateObj.isTaxdeductable())
      copyOfTaxRateObj.isTaxdeductable();
    if (gstTaxRateObj.isNoVAT())
      copyOfTaxRateObj.isNoVAT();
    if (gstTaxRateObj.isDefault())
      copyOfTaxRateObj.isDefault();
    if (gstTaxRateObj.isCashVAT())
      copyOfTaxRateObj.isCashVAT();

    copyOfTaxRateObj.setIntxIndianTaxcategory(gstTaxRateObj.getIntxIndianTaxcategory());
    copyOfTaxRateObj.setTaxBase(gstTaxRateObj.getTaxBase());
    copyOfTaxRateObj.setBaseAmount(gstTaxRateObj.getBaseAmount());
    copyOfTaxRateObj.setDestinationCountry(getCountryObj());
    copyOfTaxRateObj.setDestinationRegion(gstTaxRateObj.getDestinationRegion());
    copyOfTaxRateObj.setSalesPurchaseType(gstTaxRateObj.getSalesPurchaseType());
    copyOfTaxRateObj.setTaxSearchKey(gstTaxRateObj.getTaxSearchKey());
    copyOfTaxRateObj.setOriginalRate(gstTaxRateObj.getOriginalRate());
    copyOfTaxRateObj.setNotTaxable(gstTaxRateObj.isNotTaxable());

    OBDal.getInstance().save(copyOfTaxRateObj);
    OBDal.getInstance().flush();

    return copyOfTaxRateObj;
  }

  private TaxRate getCopyOfParentTaxRateObj(TaxRate parentTaxRate) {

    TaxRate copyOfTaxRateObj = null;
    OBCriteria<TaxRate> gstTaxRateCriteria = OBDal.getInstance().createCriteria(TaxRate.class);

    gstTaxRateCriteria.add(Restrictions.eq(TaxRate.PROPERTY_INGSTLINK, parentTaxRate));

    List<TaxRate> gstTaxRateObjList = gstTaxRateCriteria.list();
    if (gstTaxRateObjList.size() > 0) {
      copyOfTaxRateObj = gstTaxRateObjList.get(0);
    }

    return copyOfTaxRateObj;
  }

  private TaxCategory saveCopyOfTaxCategoryData(TaxCategory taxCategoryObj,
      INGSTGSTProductCode gstProductObj) {

    TaxCategory copyOfTaxCategoryObj = null;
    try {
      OBCriteria<TaxCategory> gstTaxCategoryCriteria = OBDal.getInstance().createCriteria(
          TaxCategory.class);

      gstTaxCategoryCriteria.add(Restrictions.eq(TaxCategory.PROPERTY_INGSTGSTPRODUCTCODE,
          gstProductObj));
      gstTaxCategoryCriteria.add(Restrictions.eq(TaxCategory.PROPERTY_ID, taxCategoryObj.getId()));

      List<TaxCategory> gstTaxCategoryObjList = gstTaxCategoryCriteria.list();
      if (gstTaxCategoryObjList.size() > 0) {
        copyOfTaxCategoryObj = gstTaxCategoryObjList.get(0);
        copyOfTaxCategoryObj.setUpdated(new Date());

      } else {
        copyOfTaxCategoryObj = OBProvider.getInstance().get(TaxCategory.class);
        isReprocess = true;

        copyOfTaxCategoryObj.setUpdated(new Date());
        copyOfTaxCategoryObj.setUpdatedBy(OBContext.getOBContext().getUser());
        copyOfTaxCategoryObj.setCreatedBy(OBContext.getOBContext().getUser());
        copyOfTaxCategoryObj.setClient(OBContext.getOBContext().getCurrentClient());
        copyOfTaxCategoryObj.setCreationDate(new Date());
        copyOfTaxCategoryObj.setOrganization(taxCategoryObj.getOrganization());
        copyOfTaxCategoryObj.setActive(true);
        copyOfTaxCategoryObj.setName(taxCategoryObj.getName() + " - " + GST_PRODUCTVALUE);
        copyOfTaxCategoryObj.setDescription(taxCategoryObj.getDescription());
        copyOfTaxCategoryObj.setDefault(taxCategoryObj.isDefault());
       // copyOfTaxCategoryObj.setAsbom(taxCategoryObj.isAsbom());
        copyOfTaxCategoryObj.setINGSTGSTProductCode(gstProductObj);

      }
      OBDal.getInstance().save(copyOfTaxCategoryObj);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.debug(e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }
    return copyOfTaxCategoryObj;
  }

  private OBError setSuccessMessage() {
    message.setTitle("Success");
    message.setType("Success");

    message.setMessage("Process completed successfully");
    return message;
  }

  private OBError setErrorMessage(String errorString) {
    message.setTitle("Error");
    message.setType("Error");
    message.setMessage(errorString);
    return message;
  }

  private static Country getCountryObj() {
    Country gstCountryObj = null;
    OBCriteria<Country> gstCountryCriteria = OBDal.getInstance().createCriteria(Country.class);
    gstCountryCriteria.add(Restrictions.eq(Country.PROPERTY_NAME, "India"));

    List<Country> gstCountryObjList = gstCountryCriteria.list();
    if (gstCountryObjList.size() > 0) {
      gstCountryObj = gstCountryObjList.get(0);
    }
    return gstCountryObj;
  }

}
