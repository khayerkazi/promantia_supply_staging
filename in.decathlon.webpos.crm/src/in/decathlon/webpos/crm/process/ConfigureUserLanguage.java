package in.decathlon.webpos.crm.process;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;

public class ConfigureUserLanguage extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    OBError message = new OBError();
    final String orgId = (String) bundle.getParams().get("AD_Org_ID");
    Organization orgObj = OBDal.getInstance().get(Organization.class, orgId);
    OrganizationInformation orgInfoObj = orgObj.getOrganizationInformationList().get(0);
    if (orgInfoObj.getDwcrmLanguage() == null) {
      throw new OBException("Please Configure Language In the Organization Information TAB");
    }
    try {
      User userObj = setDefaultLanguageforUser(orgObj, orgInfoObj);
      message = setSuccessMessage(userObj);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      message = setErrorMessage(e);

    } finally {
      bundle.setResult(message);
    }

  }

  private OBError setErrorMessage(Exception e) {
    OBError message = new OBError();
    Throwable ex = DbUtility.getUnderlyingSQLException(e);
    String errormsg = OBMessageUtils.translateError(ex.getMessage()).getMessage();
    message.setTitle("Error");
    message.setType("Error");
    message.setMessage(errormsg);
    return message;
  }

  private OBError setSuccessMessage(User userObj) {
    OBError message = new OBError();
    message.setTitle("Success");
    message.setType("Success");
    message.setMessage("Configuartion Done Successfully");
    return message;
  }

  private User setDefaultLanguageforUser(Organization orgObj, OrganizationInformation orgInfoObj) {
    User userObj = null;
    List<User> userList = getUsers(orgObj);

    for (User eachUserObj : userList) {
      userObj = eachUserObj;
      userObj.setDefaultLanguage(orgInfoObj.getDwcrmLanguage());
      OBDal.getInstance().save(userObj);
      OBDal.getInstance().flush();

    }
    return userObj;
  }

  private List<User> getUsers(Organization orgObj) {
    OBCriteria<User> UserList = OBDal.getInstance().createCriteria(User.class);

    UserList.add(Restrictions.eq(User.PROPERTY_ORGANIZATION, orgObj));

    if (UserList.list().size() > 0) {
      return UserList.list();
    } else {
      throw new OBException("There Are No Users Under  " + orgObj.getName() + "  Organization");
    }
  }

}
