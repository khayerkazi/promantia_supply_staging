org={};
org.openbravo = {};
org.openbravo.localization = {};
org.openbravo.localization.india = {};
org.openbravo.localization.india.ingst={};
org.openbravo.localization.india.ingst.sales={};
org.openbravo.localization.india.ingst.sales.Change_ProdCode={};
 
org.openbravo.localization.india.ingst.sales.Change_ProdCode = function(item, view, form, grid) {
    
  var callback = function (response, data, request) {
		  if (form.getField('iNGSTSTGstProductCode') != null) {
			console.log('GstprodCode====='
					+ form.getField('iNGSTSTGstProductCode').getValue());
			console.log('data=====' + data.gstProdCode);
			form.getField('iNGSTSTGstProductCode').setValue(data.gstProdCode);
		}
	  }; 
	  
	  OB.RemoteCallManager.call('org.openbravo.localization.india.ingst.sales.OnchangeProductHandler', 
			  {prodId:item.getValue()}, {}, callback);
 
 
}

