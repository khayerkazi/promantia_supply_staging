package org.openbravo.localization.india.utils.ad_callout;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

public class SelectCurrency extends SimpleCallout{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		// TODO Auto-generated method stub
		
		String strFromAccId = info.getStringParameter("inpobinutlFrmfinacctId", null);
		String strToAccId = info.getStringParameter("inpobinutlTofinacctId", null);
		
		FIN_FinancialAccount accObj = OBDal.getInstance().get(FIN_FinancialAccount.class, strFromAccId);
		if(accObj != null){
			info.addResult("inpfrmcurrencyId",accObj.getCurrency().getId().toString());
		}
		
			}

}
