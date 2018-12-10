package in.decathlon.ibud.masters.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.hibernate.HibernateException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.ViewRoleAccess;
import org.openbravo.client.myob.WidgetClassAccess;
import org.openbravo.model.ad.access.FieldAccess;
import org.openbravo.model.ad.access.FormAccess;
import org.openbravo.model.ad.access.ProcessAccess;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.TabAccess;
import org.openbravo.model.ad.access.WindowAccess;
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
    int rowCount = Integer.parseInt(request.getParameter("rowCount"));
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
    genMaster.generateJsonWS(Role.class, updated, rowCount, "", null);
    // genMaster.generateJsonWS(User.class, updated, rowCount, "User", null);
    // genMaster.getUserDataJson(Role.class, updated, "Role", isRequestFromSL, false);
    genMaster.getUserData(updated, false, "User");

    genMaster.generateJsonWS(Role.class, updated, rowCount, "", null);
    genMaster.getUserAccess(RoleOrganization.class, updated);
    genMaster.getUserAccess(WindowAccess.class, updated);
    genMaster.generateJsonWS(TabAccess.class, updated, rowCount, "", null);
    genMaster.generateJsonWS(FieldAccess.class, updated, rowCount, "", null);
    genMaster.getUserAccess(ProcessAccess.class, updated);
    genMaster.getUserAccess(FormAccess.class, updated);
    genMaster.getUserAccess(WidgetClassAccess.class, updated);
    genMaster.getUserAccess(ViewRoleAccess.class, updated);
    genMaster.getUserAccess(org.openbravo.client.application.ProcessAccess.class, updated);
  }
}