package in.decathlon.ibud.replenishment;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.web.WebService;

public class ReplenishmentWS implements WebService {
  ReplenishmentGenerator replProcess = new ReplenishmentGenerator();
  static ReplenishmentDalUtils replDalUtils = new ReplenishmentDalUtils();

  @SuppressWarnings("static-access")
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    boolean isSpoon = false;
    Date dateToBeCompared = new Date();
    String result = "";
    PrintWriter repWriter = response.getWriter();
    String output = "<html> <body> <h1>Auto Replenishment process Completed</h1> </body></html>";
    try {
      repWriter.write(output);
      List<Organization> orgList = replDalUtils.getStoreOrganizations();

      if (orgList != null && orgList.size() > 0) {
        for (Organization org : orgList) {
          if (eligibleForSpoon(org, dateToBeCompared))
            isSpoon = true;
          else if (eligibleForRegular(org, dateToBeCompared))
            isSpoon = false;
          else
            continue;

          result = replProcess.replenishPurchaseOrders(org, isSpoon, false, false, null);
          repWriter.write(result);

        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      throw e;
    }
  }

  private boolean eligibleForSpoon(Organization org, Date dateToBeCompared) throws ParseException {

    return replDalUtils.timeCheck(org, true, dateToBeCompared);

  }

  private boolean eligibleForRegular(Organization org, Date dateToBeCompared) throws ParseException {

    return replDalUtils.timeCheck(org, false, dateToBeCompared);

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    // TODO Auto-generated method stub

  }

}
