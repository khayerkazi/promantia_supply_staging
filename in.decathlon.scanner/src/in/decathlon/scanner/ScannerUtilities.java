package in.decathlon.scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.RDBMSIndependent;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;
import org.openbravo.model.ad.process.Parameter;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;

public class ScannerUtilities {
  static Logger log4j = Logger.getLogger(ScannerUtilities.class);

  public static List<Locator> getLocators(String value) {
    OBCriteria<Warehouse> sdCrit = OBDal.getInstance().createCriteria(Warehouse.class);
    sdCrit.add(Restrictions.eq(Warehouse.PROPERTY_SEARCHKEY, value));
    if (sdCrit.list().size() == 0) {
      throw new OBException("No Warehouse: " + value);
    }
    Warehouse wh = sdCrit.list().get(0);

    return wh.getLocatorList();
  }

  public static Locator getLocatorFromBarcode(String barCode) {
    OBCriteria<Locator> locCrit = OBDal.getInstance().createCriteria(Locator.class);
    locCrit.add(Restrictions.eq(Locator.PROPERTY_BARCODE, barCode));
    if (locCrit.list().size() == 0) {
      throw new OBException("No Locator with barcode: " + barCode);
    }
    Locator l = locCrit.list().get(0);

    return l;
  }

  public static Warehouse getWarehouse(String searchKey) {
    OBCriteria<Warehouse> sdCrit = OBDal.getInstance().createCriteria(Warehouse.class);
    sdCrit.add(Restrictions.eq(Warehouse.PROPERTY_SEARCHKEY, searchKey));
    if (sdCrit.list().size() == 0) {
      return null;
    }
    return sdCrit.list().get(0);
  }

  public static List<Locator> getCacLocator() {
    OBCriteria<Warehouse> sdCrit = OBDal.getInstance().createCriteria(Warehouse.class);
    sdCrit.add(Restrictions.eq(Warehouse.PROPERTY_NAME, "Saleable Whitefield"));
    if (sdCrit.list().size() == 0) {
      throw new OBException("No CAC bin");
    }
    Warehouse wh = sdCrit.list().get(0);
    return wh.getLocatorList();
  }

  public static List<ReservationStock> getReservations(StorageDetail sd, String shipName) {
    OBCriteria<ReservationStock> resvCrit = OBDal.getInstance().createCriteria(
        ReservationStock.class);
    Criteria alias = resvCrit.createAlias(ReservationStock.PROPERTY_RESERVATION, "resv");
    alias.add(Restrictions.eq("resv.rESStatus", "CO"));
    if (shipName != null) {
      alias.add(Restrictions.eq("resv.dscShipmentname", shipName));
    }
    log4j.debug("Reservation for sd: " + sd + " is " + resvCrit.list());

    return resvCrit.list();
  }

  public static BigDecimal getTotalResvdQty(StorageDetail sd) {
    BigDecimal resvdQty = new BigDecimal(0);
    List<ReservationStock> resvList = getReservations(sd, null);
    if (resvList.size() == 0) {
      // No reservations
      log4j.debug("No reservations");
      return new BigDecimal(0);
    }

    for (ReservationStock resv : resvList) {
      resvdQty = resvdQty.add(resv.getQuantity());
    }
    log4j.debug("Total Resv Qty: " + resvdQty);
    return resvdQty;
  }

  public static Product getProduct(String name) {

    // Get the product from the product name
    OBCriteria<Product> prodCrit = OBDal.getInstance().createCriteria(Product.class);
    prodCrit.add(Restrictions.eq(Product.PROPERTY_NAME, name));
    if (prodCrit.list().size() == 0) {
      throw new OBException(name + ": Product not found");
    }
    return prodCrit.list().get(0);
  }

  public static InternalMovement createMovement(Organization org, Client client) {
    InternalMovement mvmt = OBProvider.getInstance().get(InternalMovement.class);

    mvmt.setActive(true);
    mvmt.setOrganization(org);
    mvmt.setClient(client);
    DocumentType docType = getDocumentType();
    String documentNo = getDocumentNo(new DalConnectionProvider(), client.getId(),
        InternalMovement.TABLE_NAME, docType.getId(), docType.getId(), false, true);

    mvmt.setDocumentNo(documentNo);
    mvmt.setMovementDate(new Date());

    log4j.debug("Created movement");

    return mvmt;
  }

  public static void processMovement(String mvmtId) {
    final String procedureName = "m_movement_post";
    try {
      final List<Object> parameters = new ArrayList<Object>();
      OBContext.setAdminMode();
      String pInstanceId = createPInstance(mvmtId);
      OBContext.restorePreviousMode();
      parameters.add(pInstanceId);
      CallStoredProcedure.getInstance().call(procedureName, parameters, null);
    } catch (Exception e) {
      log4j.debug("Stored procedure M_Movement_Post: Caught exception " + e.getMessage());
    }
  }

  public static InternalMovementLine createMovementLine(Organization org, Client client,
      Product prod, AttributeSetInstance attr, InternalMovement mvmt, Locator from, Locator to,
      BigDecimal qty) {
    InternalMovementLine mline = OBProvider.getInstance().get(InternalMovementLine.class);

    mline.setActive(true);
    mline.setOrganization(org);
    mline.setClient(client);
    mline.setMovement(mvmt);
    mline.setProduct(prod);
    mline.setMovementQuantity(qty);
    mline.setStorageBin(from);
    mline.setNewStorageBin(to);
    mline.setUOM(prod.getUOM());

    // Set the boxNo as the attribute set value
    if (attr != null) {
      mline.setAttributeSetValue(attr);
    }
    return mline;
  }

  public static AttributeSetInstance createAttributeSetInstance(Product prod, String boxNo) {
    // Create an attribute set instance
    AttributeSetInstance attr = OBProvider.getInstance().get(AttributeSetInstance.class);
    attr.setActive(true);
    attr.setAttributeSet(prod.getAttributeSet());
    attr.setLotName(boxNo);
    attr.setDescription("L" + boxNo);
    OBDal.getInstance().save(attr);
    return attr;
  }

  private static String getDocumentNo(ConnectionProvider conn, String adClientID, String tableName,
      String cDocTypeTargetID, String cDocTypeID, boolean onlyDocType, boolean updateNext) {

    if (adClientID == null || adClientID == "")
      throw new IllegalArgumentException("Utility.getDocumentNo - client ID missing");
    if (tableName == null || tableName.length() == 0)
      throw new IllegalArgumentException("Utility.getDocumentNo - table name missing");

    final String docTypeID = ((cDocTypeTargetID == "") ? cDocTypeID : cDocTypeTargetID);
    // Use the table name to generate the document no, if document type is not
    // present
    if (docTypeID == "")
      return Utility.getDocumentNo(conn, adClientID, tableName, updateNext);

    if (adClientID.equals("0"))
      throw new UnsupportedOperationException("Cannot add System records");

    String response = null;
    try {
      response = nextDocType(conn, docTypeID, adClientID, (updateNext ? "Y" : "N"));
    } catch (final ServletException e) {
      log4j.debug("Caught exception in nextDocType" + e.getMessage());
    }

    if (response == null || response.equals("")) {
      if (!onlyDocType)
        return Utility.getDocumentNo(conn, adClientID, tableName, updateNext);
      else
        return "0";
    } else
      return response;
  }

  private static String nextDocType(ConnectionProvider connectionProvider, String cDocTypeId,
      String adClientId, String updateNext) throws ServletException {
    String strSql = "";
    strSql = strSql + "        CALL AD_Sequence_DocType(?,?,?,?)";

    String response;
    CallableStatement st = null;
    if (connectionProvider.getRDBMS().equalsIgnoreCase("ORACLE")) {

      int iParameter = 0;
      try {
        st = connectionProvider.getCallableStatement(strSql);
        iParameter++;
        UtilSql.setValue(st, iParameter, 12, null, cDocTypeId);
        iParameter++;
        UtilSql.setValue(st, iParameter, 12, null, adClientId);
        iParameter++;
        UtilSql.setValue(st, iParameter, 12, null, updateNext);
        int iParameterrazon = iParameter + 1;
        iParameter++;
        st.registerOutParameter(iParameter, 12);

        st.execute();
        response = UtilSql.getStringCallableStatement(st, iParameterrazon);
      } catch (SQLException e) {
        log4j.error("SQL error in query: " + strSql + "Exception:" + e);
        throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
            + e.getMessage());
      } catch (Exception ex) {
        log4j.error("Exception in query: " + strSql + "Exception:" + ex);
        throw new ServletException("@CODE=@" + ex.getMessage());
      } finally {
        try {
          connectionProvider.releasePreparedStatement(st);
        } catch (Exception ignore) {
          ignore.printStackTrace();
        }
      }
    } else {
      Vector<String> parametersData = new Vector<String>();
      Vector<String> parametersTypes = new Vector<String>();
      parametersData.addElement(cDocTypeId);
      parametersTypes.addElement("in");
      parametersData.addElement(adClientId);
      parametersTypes.addElement("in");
      parametersData.addElement(updateNext);
      parametersTypes.addElement("in");
      parametersData.addElement("razon");
      parametersTypes.addElement("out");
      Vector<String> vecTotal = new Vector<String>();
      try {
        vecTotal = RDBMSIndependent.getCallableResult(null, connectionProvider, strSql,
            parametersData, parametersTypes, 1);
        response = (String) vecTotal.elementAt(0);
      } catch (SQLException e) {
        log4j.error("SQL error in query: " + strSql + "Exception:" + e);
        throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
            + e.getMessage());
      } catch (NoConnectionAvailableException ec) {
        log4j.error("Connection error in query: " + strSql + "Exception:" + ec);
        throw new ServletException("@CODE=NoConnectionAvailable");
      } catch (PoolNotFoundException ep) {
        log4j.error("Pool error in query: " + strSql + "Exception:" + ep);
        throw new ServletException("@CODE=NoConnectionAvailable");
      } catch (Exception ex) {
        log4j.error("Exception in query: " + strSql + "Exception:" + ex);
        throw new ServletException("@CODE=@" + ex.getMessage());
      }
    }
    return (response);
  }

  private static String createPInstance(String recordId) {
    // get the process, we know that 122 is the m_movement_post process
    final org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(
        org.openbravo.model.ad.ui.Process.class, "122");

    // Create the pInstance
    final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
    // sets its process
    pInstance.setProcess(process);
    // must be set to true
    pInstance.setActive(true);
    pInstance.setRecordID(recordId);
    // get the user from the context
    pInstance.setUserContact(OBContext.getOBContext().getUser());

    // now create a parameter and set its values
    final Parameter parameter = OBProvider.getInstance().get(Parameter.class);
    parameter.setSequenceNumber("1");
    parameter.setParameterName("Selection");
    parameter.setString("Y");

    // set both sides of the bidirectional association
    pInstance.getADParameterList().add(parameter);
    parameter.setProcessInstance(pInstance);

    // persist to the db
    OBDal.getInstance().save(pInstance);

    // flush, this gives pInstance an ID
    OBDal.getInstance().flush();

    log4j.debug("PInstance ID: " + pInstance.getId());
    return pInstance.getId();
  }

  private static DocumentType getDocumentType() {
    OBCriteria<DocumentType> docCrit = OBDal.getInstance().createCriteria(DocumentType.class);
    docCrit.add(Restrictions.eq(DocumentType.PROPERTY_NAME, "Inventory Move"));
    if (docCrit.list().size() == 0) {
      throw new OBException("Could not find document type for Movement");
    }
    return docCrit.list().get(0);
  }

  public static String getBody(HttpServletRequest request) throws IOException {

    String body = null;
    StringBuilder stringBuilder = new StringBuilder();
    BufferedReader bufferedReader = null;

    try {
      InputStream inputStream = request.getInputStream();
      if (inputStream != null) {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        char[] charBuffer = new char[128];
        int bytesRead = -1;
        while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
          stringBuilder.append(charBuffer, 0, bytesRead);
        }
      } else {
        stringBuilder.append("");
      }
    } catch (IOException ex) {
      throw ex;
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException ex) {
          throw ex;
        }
      }
    }

    body = stringBuilder.toString();
    return body;
  }

}
