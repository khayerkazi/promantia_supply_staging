package in.decathlon.ibud.shipment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;

import static java.lang.System.*;
/**
 * Servlet implementation class DownloadInventoryReport
 */
public class DownloadInventoryReport extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final int BUFFER_SIZE = 4096;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DownloadInventoryReport() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
    	
    	String webDirName = getWebDirName();
    	
    	String filename = request.getParameter("param");

        final File file = new File( webDirName + "/" + filename);

        response.setContentType("text/csv");

        response.setHeader("Content-Disposition",
                "attachment; filename=" + file.getName());

        final BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                response.getWriter().write(line + "\n");
            }
        } finally {
            br.close();
        }
    }
    
    private String getWebDirName() {
        final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
        File f = new File(url.getPath());
     //   IbudConfig config = new IbudConfig();
      // config.getErpUrl();
        do {
          f = f.getParentFile();
          //ses
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
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
