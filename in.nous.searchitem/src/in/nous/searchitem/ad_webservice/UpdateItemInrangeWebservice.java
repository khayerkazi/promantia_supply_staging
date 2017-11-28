package in.nous.searchitem.ad_webservice;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.web.WebService;

import com.sysfore.catalog.CLMinmax;

public class UpdateItemInrangeWebservice implements WebService {

  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(UpdateItemInrangeWebservice.class);

  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    Connection conn = null;
    conn = OBDal.getInstance().getConnection();
    PreparedStatement pst = null;
    ResultSet rs = null;
    String message = "";
    String hql = "";
    Query query = null;
    String productId = null;
    String adOrgId = null;
    boolean isinRange;
    // do some checking of parameters
    StringWriter sw = new StringWriter();
    String code = request.getParameter("code");
    String storename = request.getParameter("storename");
    String inrange = request.getParameter("inrange");
    String email = request.getParameter("email");

    if (code == null) {
      throw new IllegalArgumentException("The code parameter is mandatory");
    }
    if (storename == null) {
      throw new IllegalArgumentException("The storename parameter is mandatory");
    }
    if (inrange == null) {
      throw new IllegalArgumentException("The inrange parameter is mandatory");
    }
    if (email == null) {
      throw new IllegalArgumentException("The email parameter is mandatory");
    }
    sw.append("<items>");
    try {
      hql = "select p.id from Product p where p.name='" + code + "'";
      query = OBDal.getInstance().getSession().createQuery(hql);
      query.setMaxResults(1);
      List pList = query.list();
      productId = pList.get(0).toString();
      log.info("ProductID->" + productId);

      hql = "select org.id from Organization org where org.name = '" + storename + "'";
      query = OBDal.getInstance().getSession().createQuery(hql);
      query.setMaxResults(1);
      List storeIdList = query.list();
      adOrgId = storeIdList.get(0).toString();
      log.info("OrgID->" + adOrgId);

      // fetching user information
      User currentUser = null;
      final OBCriteria<User> userObCriteria = OBDal.getInstance().createCriteria(User.class);
      userObCriteria.add(Restrictions.eq(User.PROPERTY_EMAIL, email));
      if (userObCriteria.count() > 0) {
        currentUser = userObCriteria.list().get(0);
      } else {
        throw new IllegalArgumentException("User is not found for given email parameter");
      }

      if (inrange.equals("true")) {
        isinRange = true;
      } else if (inrange.equals("false")) {
        isinRange = false;
      } else {
        throw new IllegalArgumentException("The inrange parameter must be true or false");
      }
      hql = "select mm.id from CL_Minmax mm where product.name='" + code
          + "' and organization.name = '" + storename + "' order by updated desc";
      query = OBDal.getInstance().getSession().createQuery(hql);
      query.setMaxResults(1);
      List list = query.list();
      if (!list.isEmpty()) {// update cl_minmax
        hql = "select mm.id from CL_Minmax mm  where mm.product.id in (select p.id from Product p "
            + " where p.clModel.id in (select pd.clModel.id from Product pd where pd.name='" + code
            + "')) and mm.organization.name = '" + storename + "'";
        query = OBDal.getInstance().getSession().createQuery(hql);
        List plist = query.list();
        for (int i = 0; i < plist.size(); i++) {
          String id = plist.get(i).toString();
          // System.out.println("i=" + i + "id->" + id);
          log.info("CLMinMaxId->" + id);
          CLMinmax clmm = OBDal.getInstance().get(CLMinmax.class, id);
          clmm.setInrange(isinRange);
          OBDal.getInstance().save(clmm);
          OBDal.getInstance().flush();
        }
        // updated successfully
        if (isinRange) {
          message = "The product is in Range now";
        } else {
          message = "The product is out of Range now";
        }
      } else {// insert into cl_minmax
        Organization org = OBDal.getInstance().get(Organization.class, adOrgId);
        Product p = OBDal.getInstance().get(Product.class, productId);
        CLMinmax clmm = OBProvider.getInstance().get(CLMinmax.class);
        clmm.setClient(currentUser.getClient());
        clmm.setOrganization(org);
        clmm.setActive(true);
        clmm.setCreationDate(new Date());
        clmm.setUpdated(new Date());
        clmm.setCreatedBy(currentUser);
        clmm.setUpdatedBy(currentUser);
        clmm.setProduct(p);
        clmm.setMinQty((long) 0);
        clmm.setMaxQty((long) 0);
        clmm.setDisplaymin((long) 0);
        clmm.setInrange(isinRange);
        OBDal.getInstance().save(clmm);
        OBDal.getInstance().flush();
        // inserted successfully
        if (isinRange) {
          message = "The product is in Range now";
        } else {
          message = "The product is out of Range now";
        }
      }
      log.info(message);
      sw.append("<message>" + message + "</message>");
    } catch (Exception e) {
      e.printStackTrace();
      log.error("Error occured due to: " + e);
      String msg = "Error occurred->" + e;
      sw.append("<message>" + msg + "</message>");
    }
    sw.append("</items>");
    conn.close();
    // and get the result
    final String xml = sw.toString();

    // write to the response
    response.setContentType("text/xml");
    response.setCharacterEncoding("utf-8");
    final Writer w = response.getWriter();
    w.write(xml);
    w.close();
  }

  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }
}
