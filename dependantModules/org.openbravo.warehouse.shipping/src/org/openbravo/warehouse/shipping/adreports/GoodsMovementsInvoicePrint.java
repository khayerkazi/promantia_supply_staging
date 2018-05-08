package org.openbravo.warehouse.shipping.adreports;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

public class GoodsMovementsInvoicePrint extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strMovementId = vars.getSessionValue("Goods_Movements_Invoice_Print.inpmMovementId");
      // GoodsMovementsInvoicePrint
      log4j.debug("strShippingId------------>" + strMovementId);
      if (strMovementId.equals(""))
        strMovementId = vars.getSessionValue("Goods_Movements_Invoice_Print.inpmMovementId");

      String trimmedmovementID = strMovementId.substring(2, strMovementId.length() - 2);

      InternalMovement movementObj = OBDal.getInstance().get(InternalMovement.class,
          trimmedmovementID);
      try {
        if (!movementObj.getSWMovementtypegm().equalsIgnoreCase("Saleable Fixture WH-WH"))
          throw new OBException(
              "Print Button works only with the 'Saleable Fixture WH-WH' " + " Movement Type");
        // strcBpartnerId = vars.getSessionValue("PrintRfQ.inpcBpartnerId");
        else
          printPagePartePDF(response, vars, strMovementId);
      } catch (Exception e) {
        throw new OBException(
            "Print Button works only with the 'Saleable Fixture WH-WH' " + " Movement Type");
      }
    } else
      pageError(response);
  }

  private void printPagePartePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strShippingId) throws IOException, ServletException {

    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    JasperReport jasperReportLines;
    try {
      JasperDesign jasperDesignLines = JRXmlLoader.load(
          strBaseDesign + "/org/openbravo/warehouse/shipping/adreports/FixtureInvoiceRe.jrxml");
      jasperReportLines = JasperCompileManager.compileReport(jasperDesignLines);
    } catch (JRException e) {
      e.printStackTrace();
      throw new ServletException(e.getMessage());
    }

    strShippingId = strShippingId.replaceAll("\\(|\\)|'", "");
    parameters.put("BASE_DESIGN", strBaseDesign);
    parameters.put("DOCUMENT_ID", strShippingId);
    renderJR(vars, response, null, "pdf", parameters, null, null);
  }

  public String getServletInfo() {
    return "Servlet that presents the RptMRequisitions seeker";
  } // End of getServletInfo() method
}