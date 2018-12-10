package in.decathlon.ibud.masters.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.HibernateException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.model.common.businesspartner.Greeting;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.web.WebService;

/*
 * 
 * Push User related data from supply to store
 * 
 */
public class UserSyncWS implements WebService {

  DataToJsonConverter dataToJsonConverter = new DataToJsonConverter();
  final DataToJsonConverter toJsonConverter = OBProvider.getInstance().get(
      DataToJsonConverter.class);

  GeneralMasterSerializer genMaster;

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not implemented");
  }

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    processRequest(path, request, response);
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPost not Implemented");
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    throw new NotImplementedException("doPut not Implemented");

  }

  private void processRequest(String path, HttpServletRequest request, HttpServletResponse response)
      throws HibernateException, Exception {
    genMaster = new GeneralMasterSerializer(response);
    String updated = request.getParameter("updated");

    // int rowCount = Integer.parseInt(request.getParameter("rowCount"));
    updated = updated.replace("_", " ");

    /*
     * dont pull Organization,client, business partner,location
     * genMaster.generateJsonWS(Organization.class, updated, rowCount, "Organization", null);
     * genMaster.generateJsonWS(Client.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(BusinessPartner.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(Location.class, updated, rowCount, "", null);
     * genMaster.generateJsonWS(org.openbravo.model.common.businesspartner.Location.class, updated,
     * rowCount, "", null);
     */
    genMaster.getUserDataJson(Greeting.class, updated, "Greeting", false, false);

    // genMaster.getUserDataJson(Role.class, updated, "Role", isRequestFromSL, false);
    genMaster.getUserData(updated, false);
    // genMaster.getUserDataJson(User.class, updated, "User", isRequestFromSL, false);
    // genMaster.getUserDataJson(RoleOrganization.class, updated, "", isRequestFromSL, true);
    // genMaster.getUserDataJson(WindowAccess.class, updated, "", isRequestFromSL, true);
    // genMaster.getUserDataJson(TabAccess.class, updated, "TabAccess", isRequestFromSL, false);
    // genMaster.getUserDataJson(FieldAccess.class, updated, "FieldAccess", isRequestFromSL, false);
    // genMaster.getUserDataJson(ProcessAccess.class, updated, "", isRequestFromSL, true);
    // genMaster.getUserDataJson(FormAccess.class, updated, "", isRequestFromSL, true);
    // genMaster.getUserDataJson(WidgetClassAccess.class, updated, "", isRequestFromSL, true);
    // genMaster.getUserDataJson(ViewRoleAccess.class, updated, "", isRequestFromSL, true);
    // genMaster.getUserDataJson(org.openbravo.client.application.ProcessAccess.class, updated, "",
    // isRequestFromSL, true);
  }
}