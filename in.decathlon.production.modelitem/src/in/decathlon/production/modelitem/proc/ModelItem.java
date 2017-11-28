package in.decathlon.production.modelitem.proc;

import in.decathlon.production.modelitem.data.PrmiComponentLabel;
import in.decathlon.production.modelitem.data.PrmiProcess;
import in.decathlon.production.modelitem.data.Subcontractor;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sysfore.catalog.CLModel;

public class ModelItem extends IdlServiceJava {

  private static final String SUBCONTRACTOR = "Subcontractor";

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Model Code", Parameter.STRING),
        new Parameter("Supplier 1", Parameter.STRING),
        new Parameter("Supplier 1 Code", Parameter.STRING),
        new Parameter("Supplier 2", Parameter.STRING),
        new Parameter("Supplier 2 Code", Parameter.STRING),
        new Parameter("Supplier 3", Parameter.STRING),
        new Parameter("Supplier 3 Code", Parameter.STRING),
        new Parameter("Process", Parameter.STRING),
        new Parameter("Process Code", Parameter.STRING),
        new Parameter("Component Label", Parameter.STRING),
        new Parameter("Component Label Code", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    validator.checkNotNull(validator.checkString(values[0], 32), "SearchKey");
    validator.checkNotNull(validator.checkString(values[1], 60));

    validator.checkNotNull(validator.checkString(values[2], 40), "SearchKey");
    validator.checkString(values[3], 40);
    validator.checkString(values[4], 60);
    validator.checkString(values[5], 40);
    validator.checkString(values[6], 40);
    validator.checkNotNull(validator.checkString(values[7], 60));
    validator.checkNotNull(validator.checkString(values[8], 40));
    validator.checkNotNull(validator.checkString(values[9], 60));

    validator.checkNotNull(validator.checkString(values[10], 40));

    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createImportProduction((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6],
        (String) values[7], (String) values[8], (String) values[9], (String) values[10]);
  }

  public BaseOBObject createImportProduction(final String modelCode, final String supplier1,
      final String supplier1Code, final String supplier2, final String supplier2Code,
      final String supplier3, final String supplier3Code, final String process,
      final String processCode, final String componentLabel, final String componentLabelCode)
      throws Exception {
    Vector<BusinessPartner> bpVector = new Vector();

    CLModel clm = findDALInstance(false, CLModel.class, new Value("modelCode", modelCode));

    if ((modelCode.equals("")) || (clm == null)) {
      throw new OBException("ModelCode " + modelCode + " not valid");
    }
    if (clm.getPrmiSubcontractorList().size() != 0) {
      throw new OBException("Supplier  already loaded for this " + modelCode);
    }
    if (clm.getPrmiProcess() != null) {
      throw new OBException("Process Code " + clm.getPrmiProcess() + " already set  for model "
          + modelCode);
    }
    if (clm.getPrmiComponentlabel() != null) {
      throw new OBException("ComponentLabel Code " + componentLabelCode
          + " already set  for model " + modelCode);
    }

    BusinessPartner bp;

    if ((supplier1Code != null) && (!supplier1Code.trim().equals(""))) {

      bp = getBusinessPartner(supplier1Code, supplier1);
      bpVector.add(bp);

    }
    if ((supplier2Code != null) && (!supplier2Code.trim().equals(""))) {
      bp = getBusinessPartner(supplier2Code, supplier2);
      bpVector.add(bp);
    }

    if ((supplier3Code != null) && (!supplier3Code.trim().equals(""))) {
      bp = getBusinessPartner(supplier3Code, supplier3);
      bpVector.add(bp);
    }

    if ((processCode != null) && (!processCode.trim().equals(""))) {
      setprmiProcess(process, processCode, clm);
    }

    if ((componentLabelCode != null) && (!componentLabelCode.trim().equals(""))) {
      setprmiComponentLabel(componentLabel, componentLabelCode, clm);
    }

    int line = 10;

    for (Enumeration e = bpVector.elements(); e.hasMoreElements();) {
      saveSubcontractor((BusinessPartner) e.nextElement(), clm, line);

      line = line + 10;

    }

    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    return clm;

  }

  private void setprmiComponentLabel(final String componentLabel, final String componentLabelCode,
      CLModel clm) {
    PrmiComponentLabel pcl;
    PrmiComponentLabel pclExist = findDALInstance(false, PrmiComponentLabel.class, new Value(
        "searchKey", componentLabelCode));

    if (pclExist != null) {
      pcl = pclExist;
    } else {
      if ((componentLabel == null) || (componentLabel.trim().equals(""))) {
        throw new OBException("Component Label Name not provided for Model " + clm.getModelCode());
      }

      pcl = OBProvider.getInstance().get(PrmiComponentLabel.class);

      pcl.setClient(OBContext.getOBContext().getCurrentClient());
      pcl.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      pcl.setCreatedBy(OBContext.getOBContext().getUser());
      pcl.setUpdatedBy(OBContext.getOBContext().getUser());
      pcl.setCreationDate(new Date());
      pcl.setActive(true);
      pcl.setCommercialName(componentLabel);
      pcl.setSearchKey(componentLabelCode);
      OBDal.getInstance().save(pcl);

    }
    clm.setPrmiComponentlabel(pcl);
    OBDal.getInstance().save(clm);
  }

  private void setprmiProcess(final String process, final String processCode, CLModel clm) {
    PrmiProcess pps;
    PrmiProcess ppsExist = findDALInstance(false, PrmiProcess.class, new Value("searchKey",
        processCode));
    if (ppsExist != null) {
      pps = ppsExist;
    } else {
      if ((process == null) || (process.trim().equals(""))) {
        throw new OBException("Process Name not provided for Model " + clm.getModelCode());
      }

      pps = OBProvider.getInstance().get(PrmiProcess.class);

      pps.setClient(OBContext.getOBContext().getCurrentClient());
      pps.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      pps.setCreatedBy(OBContext.getOBContext().getUser());
      pps.setUpdatedBy(OBContext.getOBContext().getUser());
      pps.setCreationDate(new Date());

      pps.setActive(true);
      pps.setCommercialName(process);
      pps.setSearchKey(processCode);

      OBDal.getInstance().save(pps);

    }
    clm.setPrmiProcess(pps);
    OBDal.getInstance().save(clm);
  }

  public BusinessPartner getBusinessPartner(String searchkey, String name) {

    BusinessPartner bpExist = findDALInstance(false, BusinessPartner.class, new Value("searchKey",
        "SC" + "/" + searchkey));
    if (bpExist != null) {
      return bpExist;

    } else {
      BusinessPartner bp = OBProvider.getInstance().get(BusinessPartner.class);

      bp.setClient(OBContext.getOBContext().getCurrentClient());
      bp.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      bp.setCreatedBy(OBContext.getOBContext().getUser());
      bp.setUpdatedBy(OBContext.getOBContext().getUser());
      bp.setCreationDate(new Date());
      bp.setActive(true);
      bp.setName(name);
      bp.setSearchKey("SC" + "/" + searchkey);
      setCategory(bp);
      bp.setVendor(false);
      bp.setCustomer(false);
      bp.setPrmiIssubcontractor(true);

      OBDal.getInstance().save(bp);
      return bp;
    }
  }

  private void setCategory(BusinessPartner bp) {
    Category category = findDALInstance(false, Category.class, new Value(
        Category.PROPERTY_SEARCHKEY, SUBCONTRACTOR));
    if (category == null) {
      Category cat = OBProvider.getInstance().get(Category.class);
      cat.setClient(OBContext.getOBContext().getCurrentClient());
      cat.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      cat.setCreatedBy(OBContext.getOBContext().getUser());
      cat.setUpdatedBy(OBContext.getOBContext().getUser());
      cat.setCreationDate(new Date());
      cat.setUpdated(new Date());
      cat.setActive(true);
      cat.setName(SUBCONTRACTOR);
      cat.setSearchKey(SUBCONTRACTOR);
      OBDal.getInstance().save(cat);
      OBDal.getInstance().flush();
      category = cat;

    }
    bp.setBusinessPartnerCategory(category);

  }

  public Subcontractor saveSubcontractor(BusinessPartner bp, CLModel clm, int line) {

    // create an OBCriteria object and add a filter
    final OBCriteria<Subcontractor> obCriteria = OBDal.getInstance().createCriteria(
        Subcontractor.class);

    obCriteria.add(Restrictions.eq(Subcontractor.PROPERTY_BUSINESSPARTNER, bp));
    obCriteria.add(Restrictions.eq(Subcontractor.PROPERTY_MODEL, clm));

    if (obCriteria.list().size() > 0) {
      throw new OBException("Information for subcontractor  is already exist");
      // return obCriteria.list().get(0);
    } else {

      Subcontractor sb = OBProvider.getInstance().get(Subcontractor.class);
      sb.setActive(true);
      sb.setClient(OBContext.getOBContext().getCurrentClient());
      sb.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      sb.setCreatedBy(OBContext.getOBContext().getUser());
      sb.setUpdatedBy(OBContext.getOBContext().getUser());
      sb.setBusinessPartner(bp);
      sb.setModel(clm);

      // sb.setLine(String.valueOf(line));
      // sb.setLine(String.valueOf(line));
      sb.setLineNo(String.valueOf(line));
      OBDal.getInstance().save(sb);
      return sb;

    }
  }

  @Override
  protected String getEntityName() {
    // TODO Auto-generated method stub
    return "Production Model Details";
  }

}
