/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  dan "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package in.nous.dmi.orderclose.ad_process;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalBaseProcess;

/*
 * This class is used to close the Purchase Order
 */

public class PurchaseOrderClosureProcess extends DalBaseProcess {
  private ProcessLogger logger;
  static Logger log4j = Logger.getLogger(PurchaseOrderClosureProcess.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    VariablesSecureApp vars = bundle.getContext().toVars();
    final String language = bundle.getContext().getLanguage();
    try {
      System.out.println("Inside doExecute() of PurchaseOrderClosureProcess: " + new Date());
      List<Object> param = new ArrayList<Object>();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date currentDate = new Date();

      Calendar c = Calendar.getInstance();
      c.setTime(currentDate);
      c.add(Calendar.DATE, -1);

      param.add(c.getTime());
      param.add(currentDate);
      String x = (String) CallStoredProcedure.getInstance().call("ndoc_close_po", param, null,
          true, true);

      System.out.println("Result is: " + x);
    } catch (Exception e) {
      logger
          .logln(Utility.parseTranslation(bundle.getConnection(), vars, language, e.getMessage()));
    }
  }

}
