package in.decathlon.pnl;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.web.WebService;

public class PnlUsers implements WebService {

  public static final Logger logger = Logger.getLogger(PnlUsers.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

    String email = null;

    if (request.getParameter("email") == null) {
      throw new Exception("email is empty");
    } else {
      email = request.getParameter("email");
      JSONObject jsonResponseObj = getUserDefaultRoles(email);
      writeResult(response, jsonResponseObj.toString());

    }

  }

  private JSONObject getUserDefaultRoles(String email) throws JSONException {

    JSONObject usersRoleObj = new JSONObject();
    JSONObject userjsonObj = new JSONObject();

    OBQuery<User> userQuery = OBDal.getInstance().createQuery(User.class,
        "as e where e.email = :email");

    userQuery.setNamedParameter("email", email);
    User user = userQuery.uniqueResult();

    if (user != null) {
      usersRoleObj.put("userId", user.getId());
      usersRoleObj.put("orgId", user.getDefaultOrganization().getId());
      usersRoleObj.put("org", user.getDefaultOrganization().getName());
      usersRoleObj.put("role", user.getDefaultRole().getName());
      userjsonObj.put("data", usersRoleObj);

    } else {
      logger.info("UserList is empty" + user);
    }
    return userjsonObj;

  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
