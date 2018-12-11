package in.decathlon.ibud.masters.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;
import org.openbravo.service.json.DataToJsonConverter;
import org.openbravo.service.web.WebService;

/*
 * 
 * Push the organization detail from supply to store
 * 
 */
public class OrgnizationSyncWS implements WebService {
  private static final Logger log = Logger.getLogger(MasterWebService.class);

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
      throws Exception {

    try {
      genMaster = new GeneralMasterSerializer(response);
      String updated = request.getParameter("updated");
      String bpartnerLocation = "bpLocation";
      String bpartner = "businessPartner";
      log.debug(" date in string format taken from query string " + updated);
      int rowCount = 2;// Integer.parseInt(request.getParameter("rowCount"));
      updated = updated.replace("_", " ");

      OBContext.setAdminMode(true);
      genMaster.generateJsonWS(Client.class, updated, rowCount, "", null);
      genMaster.generateJsonWS(AcctSchema.class, updated, rowCount, "AcctSchema", null);
      genMaster.generateJsonWS(Organization.class, updated, rowCount, "Organization", null);
      genMaster.getLocation(updated);
      genMaster.generateJsonWS(Category.class, updated, rowCount, "Category", null);
      // genMaster.generateJsonWS(BusinessPartner.class, updated, rowCount, "", null);
      // pull business partner assigned in org info if they got updated
      genMaster.getBusinessPartner(updated, bpartner);
      genMaster.getBusinessPartner(updated, bpartnerLocation);
      genMaster.generateJsonWS(Costcenter.class, updated, rowCount, "", null);
      genMaster.generateJsonWS(OrganizationInformation.class, updated, rowCount,
          "OrganizationInformation", null);
      genMaster.generateJsonWS(Sequence.class, updated, rowCount, "DocSequence", null);
      genMaster.generateJsonWS(DocumentType.class, updated, rowCount, "DocumentType", null);
      genMaster.getOrgInfo(OrganizationInformation.class, updated);
      genMaster.getTreeNode(updated);
      // genMaster.generateJsonWS(Posconfig.class, updated, rowCount, "", null);
    } catch (Exception e) {
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
