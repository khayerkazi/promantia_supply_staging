package org.openbravo.warehouse.shipping.print;

import static java.lang.System.out;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;

public class DownloadExcelFile extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final int BUFFER_SIZE = 4096;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DownloadExcelFile() {
    super();
    // TODO Auto-generated constructor stub
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String webDirName = getWebDirName();
    webDirName = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");
    String filename = request.getParameter("param");
    // filename = filename + ".xlsx";
    final File file = new File(webDirName.trim() + "/" + filename);
    InputStream in = null;
    OutputStream outstream = null;
    try {
      // response.reset();
      in = new FileInputStream(file);
      response.setContentType("application/vnd.ms-excel");
      response.addHeader("Content-Disposition", "attachment; filename=" + file.getName());
      outstream = response.getOutputStream();
      IOUtils.copyLarge(in, outstream);
    } catch (Exception e) {
      throw new OBException("Unable to download file");
    } finally {
      IOUtils.closeQuietly(outstream);
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
      /*
       * if (file != null && (!((file.getName().startsWith("PackingSalesReport_For-")) ||
       * file.getName() .startsWith("ShippingSalesReport_For-")))) file.delete();
       */
    }
  }

  private String getWebDirName() {
    final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
    File f = new File(url.getPath());
    // IbudConfig config = new IbudConfig();
    // config.getErpUrl();
    do {
      f = f.getParentFile();
      // ses
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

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // TODO Auto-generated method stub
  }

}