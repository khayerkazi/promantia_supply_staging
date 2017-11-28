package in.decathlon.etlsync;

import in.decathlon.journalentry.StoreStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.web.WebService;

public class CreatePosInvoiceWS implements WebService {
  static Logger log4j = Logger.getLogger(CreatePosInvoiceWS.class);

  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    String orgid = null;
    String date = null;

    if (request.getParameter("orgid") != null) {
      orgid = request.getParameter("orgid");
    }

    if (request.getParameter("odate") != null) {
      date = request.getParameter("odate");
    }
    new PosSalesInvoiceCreateUpdated().generatePOSInvoices(orgid, date);
   
    response.getWriter().write("<html><body>");
    response.getWriter().write("<marquee><font face='Comic Sans MS' size='5' color='blue'>The POS Invoice Process is Successful.</font></marquee>");
    response.getWriter().write("</body></html>");
  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    String orgid = null;
    String date = null;

    if (request.getParameter("orgid") != null) {
      orgid = request.getParameter("orgid");
    }

    if (request.getParameter("odate") != null) {
      date = request.getParameter("odate");
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    Date closedate = null;

    if (date != null) {
      try {
        closedate = sdf.parse(date);
      } catch (ParseException e) {
        String message = "The order date entered is not valid. Use format yyyy-MM-DD";
        log4j.error(message);
        throw new OBException(message);
      }
    }

    OBCriteria<StoreStatus> storestatus = OBDal.getInstance().createCriteria(StoreStatus.class);
    storestatus.add(Restrictions.eq(StoreStatus.PROPERTY_STORESTATUS, "Y"));

    Organization orgPrev;
    if (orgid != null) {
      orgPrev = OBDal.getInstance().get(Organization.class, orgid);
      if (orgPrev == null) {
        String message = "@etlSync:Invalid Organizaiton : " + orgid;
        log4j.error(message);
        throw new OBException(message);
      }
      storestatus.add(Restrictions.eq(StoreStatus.PROPERTY_ORGANIZATION, orgPrev));
    }
    if (date != null) {
      storestatus.add(Restrictions.eq(StoreStatus.PROPERTY_CLOSEDDATE, closedate));
    }

    if (storestatus.list().size() == 0) {
      String message = "@etlSync:No stores to process";
      log4j.info(message);
      throw new OBException(message);
    } else {

      for (StoreStatus st : storestatus.list()) {
        String message = "@etlSync:Setting the store processed for organization:"
            + st.getOrganization() + " & closed date:" + st.getClosedDate();

        try {
          new PosSalesInvoiceCreateUpdated().createPosInvoiceProcess(st.getOrganization(), st
              .getClosedDate());

        } catch (Exception e) {
          throw new OBException("The POS Invoice Generation process failing. See message:"
              + e.getMessage());
        }
        log4j.info(message);
        st.setStoreStatus("P");
      }
    }

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
