package org.openbravo.localization.india.ingst.master.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.localization.india.ingst.master.business_event.GSTProductDisplay;

public class OnChangeProductType extends SimpleCallout {
	private static final long serialVersionUID = 1L;

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		// TODO Auto-generated method stub
	    log4j.info("info" + info);
	 
	    String type = info.getStringParameter("inptype", null);
	    String value = info.getStringParameter("inpvalue", null);
	    
	    value=GSTProductDisplay.getHSNTypeValue(type, value);
	    info.addResult("inpvalue", value);
	}
}
