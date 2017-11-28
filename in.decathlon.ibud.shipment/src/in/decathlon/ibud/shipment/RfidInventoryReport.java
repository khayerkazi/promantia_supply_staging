package in.decathlon.ibud.shipment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utility.commons.csv.CSVGenerator;

public class RfidInventoryReport extends DalBaseProcess {

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode(true);
      String webDirName = getWebDirName();

      String mWarehouseId = (String) bundle.getParams().get("mWarehouseId");
      String locator = (String) bundle.getParams().get("locatorName");
      String cycle = (String) bundle.getParams().get("cycle");
      int cycleInt = Integer.parseInt(cycle);
      File f = new File(webDirName);
      CSVGenerator csvg = new CSVGenerator(f, "InventoryUpload");

      String qry = "select mw.name as Warehouse, mp.name as Product, coalesce(mp.uPCEAN,'NA') as EAN, ml.searchKey as Location, coalesce(mt.lotName,'') as Box, round(msd.quantityOnHand) as Quantity, round(msd.reservedQty) as Reserved from MaterialMgmtStorageDetail msd join msd.product mp join msd.storageBin ml join msd.attributeSetValue mt join ml.warehouse mw where ml.searchKey like '" + locator + "%' and msd.quantityOnHand > 0 and not exists (from MaterialMgmtInventoryCount mi join mi.materialMgmtInventoryCountLineList mil where mil.product.id = msd.product.id and mil.storageBin.id = msd.storageBin.id and mil.attributeSetValue.id = msd.attributeSetValue.id and mil.creationDate > now()- " + cycleInt + " and mi.processed = 'Y') and mw.id = '" + mWarehouseId + "'  order by ml.searchKey";
      Query query = OBDal.getInstance().getSession().createQuery(qry);
      File csvfile = csvg.generateCSV(query);
      String newfile = csvfile.getName();
      String fullFileName = webDirName + "/" + newfile;

      final OBError msg = new OBError();
      msg.setType("Success");
      String message = "Success Download from " + getDownloadUrl(newfile);
      msg.setMessage(message);
      // bundle.se
      bundle.setResult(msg);

    } catch (final Exception e) {

      OBDal.getInstance().rollbackAndClose();
      // log4j.error("Error creating copy of product in sequence", e);
      final OBError msg = new OBError();
      msg.setType("Error");
      if (e instanceof org.hibernate.exception.GenericJDBCException) {
        msg.setMessage(((org.hibernate.exception.GenericJDBCException) e).getSQLException()
            .getNextException().getMessage());
      } else if (e instanceof org.hibernate.exception.ConstraintViolationException) {
        msg.setMessage(((org.hibernate.exception.ConstraintViolationException) e).getSQLException()
            .getNextException().getMessage());
      } else {
        msg.setMessage(e.getMessage());
      }
      bundle.setResult(msg);

    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    final VariablesSecureApp vars = new VariablesSecureApp(request);
    // post(vars, request, response);
  }

  private String getWebDirName() {
    final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
    File f = new File(url.getPath());

    do {
      f = f.getParentFile();
    } while (!(f.getName().equals("/")) && (!f.getName().equals("classes")));

    if (f.getName().equals("classes")) {
      if (f.getParentFile().getName().equals("build")) {
        return f.getParentFile().getParent() + "/WebContent/web";
      } else {
        return f.getParentFile().getParent() + "/web";
      }
    }
    throw new OBException("Cannot set path for download for " + url.getPath());
  }

  private String getDownloadUrl(String fullFileName) {
    //String relativeUrl = fullFileName.split("/web/")[1];
	  String erpUrl= OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ERPInventoryContextUrl");
    return "<a href="+erpUrl + "/DownloadInventoryReport?param="+fullFileName+" "+"target="+"_blank"+">"+"Download "
    		+ "Inventory Report" +"</a>";
  }

  private static void downloadUsingNIO(String urlStr, String file) throws IOException {
    URL url = new URL(urlStr);
    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
    FileOutputStream fos = new FileOutputStream(file);
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    fos.close();
    rbc.close();
  }

}
