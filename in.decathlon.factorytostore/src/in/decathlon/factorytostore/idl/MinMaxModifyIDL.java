package in.decathlon.factorytostore.idl;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sysfore.catalog.CLMinmax;

public class MinMaxModifyIDL extends IdlServiceJava {

  @Override
  protected String getEntityName() {
    return "Min Max Modify";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Organization", Parameter.STRING),
        new Parameter("Item Code", Parameter.STRING), new Parameter("Delivery", Parameter.STRING),
        new Parameter("Coefficient", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    validator.checkOrganization(values[0]);
    validator.checkNotNull(validator.checkString(values[1], 32), "Item Code");
    validator.checkBoolean(values[2]);
    validator.checkLong(values[3]);

    getCLMinMax(values[0], values[1]);
    return values;
  }

  @Override
  protected BaseOBObject internalProcess(Object... values) throws Exception {

    return modifyMinMax((String) values[0], (String) values[1], (String) values[2],
        (String) values[3]);
  }

  private BaseOBObject modifyMinMax(String org, String itemCOde, String delivery, String coefficient) {

    CLMinmax clExist = getCLMinMax(org, itemCOde);

    // If exists UPDATE the object

    clExist.setFacstIsDirectDelivery(delivery.equals("Y") ? true : false);
    clExist.setFacstLeadCoefficent(Long.parseLong(coefficient));

    return clExist;

  }

  private Product getProduct(String itemCode) {
    OBCriteria<Product> criteriaOnPrd = OBDal.getInstance().createCriteria(Product.class);
    criteriaOnPrd.add(Restrictions.eq(Product.PROPERTY_NAME, itemCode));

    List<Product> prdList = criteriaOnPrd.list();
    if (prdList != null && prdList.size() > 0) {
      return prdList.get(0);
    }
    throw new OBException(" Product with item code " + itemCode + "  Not exists");
  }

  private Organization getOrg(String org) {
    OBCriteria<Organization> criteriaOnOrg = OBDal.getInstance().createCriteria(Organization.class);
    criteriaOnOrg.add(Restrictions.eq(Organization.PROPERTY_NAME, org));
    List<Organization> orgList = criteriaOnOrg.list();
    if (orgList != null && orgList.size() > 0) {
      return orgList.get(0);
    }
    throw new OBException("Organization " + org + " does not exist");
  }

  private CLMinmax getCLMinMax(String org, String itemCOde) {

    Organization orgnization = getOrg(org);
    Product product = getProduct(itemCOde);

    OBCriteria<CLMinmax> criteriaOnCLMinmax = OBDal.getInstance().createCriteria(CLMinmax.class);
    criteriaOnCLMinmax.add(Restrictions.eq(CLMinmax.PROPERTY_ORGANIZATION, orgnization));
    criteriaOnCLMinmax.add(Restrictions.eq(CLMinmax.PROPERTY_PRODUCT, product));
    List<CLMinmax> clList = criteriaOnCLMinmax.list();
    if (clList != null && clList.size() > 0) {
      return clList.get(0);
    } else {
      throw new OBException("Product and Org " + itemCOde + "&" + org + "Not Match");
    }
  }

}
