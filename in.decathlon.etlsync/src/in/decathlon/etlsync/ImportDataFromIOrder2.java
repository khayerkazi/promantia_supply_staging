package in.decathlon.etlsync;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.validation.ValidationException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.dataimport.Order;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.json.JsonToDataConverter;
import org.openbravo.service.web.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ImportDataFromIOrder2 implements WebService {
	
	private static final Logger log = Logger.getLogger(ImportDataFromIOrder2.class);

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		int i=0;//initialize header/c_order variable		
		
		// Fetching the records from i_order table where the batch no is NULL with unique receipt numbers
		final OBQuery<Order> obQuery = OBDal.getInstance().createQuery(Order.class, "as o where o.imBatchno is null");
		obQuery.setSelectClause("DISTINCT o.iMEMImReceiptno");
		obQuery.setMaxResult(50);
		
		List batchNumbers = new ArrayList();
		// for-each to call the second webservice uniquely by fetching the specific records
		for (Object recNo : obQuery.list().toArray()) {
			log.info((String)recNo);
			final OBQuery<Order> obQuery1 = OBDal.getInstance().createQuery(Order.class, "as o where o.iMEMImReceiptno='"+(String)recNo+"' and o.imBatchno is null");
			
			// Generate random string for batch no
			String batch_no = UUID.randomUUID().toString().replace("-", "").toUpperCase();
			batchNumbers.add(batch_no);
			
			for(Order iorder : obQuery1.list()) {
				
				iorder.setImBatchno(batch_no);
				iorder.setProcessNow(true);
				OBDal.getInstance().save(iorder);
				OBDal.getInstance().flush();
			}
			OBDal.getInstance().commitAndClose();
			
		}
		// for-each for rotating based on batch number
		for(Object batchNo : batchNumbers) {
			
			String currentBatchno = (String)batchNo;
			
			final OBQuery<Order> obQuery2 = OBDal.getInstance().createQuery(Order.class, "as o where o.imBatchno='"+currentBatchno+"'");
			if(obQuery2.list().size() > 0) {

				Order orderHeader = obQuery2.list().get(i);
	
				// Fetching the Organization specific information
				final OBQuery<Organization> obOrg = OBDal.getInstance().createQuery(Organization.class, "as org where org.id='"+orderHeader.getOrganization().getId()+"'");
				final Organization organization = obOrg.list().get(0);
				
				// Default values declaration
				String doctype = organization.getDsidefPosdoctype();
				String warehouse = organization.getDsidefPoswarehouse();
				String pricelist = organization.getDsidefPospricelist();
				String bpartner_loc_id = organization.getDsidefPosinvaddr();
				
				final String dateOrdered = orderHeader.getOrderDate().toString();
				String desc = orderHeader.getDescription();
				if(orderHeader.getDescription() !=null) 
					desc = orderHeader.getDescription().replace("\n", " ").trim();
				
				String bpID = "";
				OBContext.setAdminMode();
				final OBCriteria<DSIDEFModuleConfig> moduleConfigObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
				moduleConfigObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.etlsync"));
				moduleConfigObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_KEY, "defaultcustomer"));
				if(moduleConfigObCriteria.count() > 0) {
					bpID = moduleConfigObCriteria.list().get(0).getSearchKey();
				}
				OBContext.restorePreviousMode();

				String oxylaneId = "";
		        if(orderHeader.getSyncOxylane() == null)
		        	oxylaneId = "";
		        else 
		        	oxylaneId = orderHeader.getSyncOxylane();
				try {
					JSONObject job = new JSONObject("{\"_entityName\":\"Order\",\"client\":\""+orderHeader.getClient().getId()+"\",\"client._identifier\":\"\",\"organization\":\""+orderHeader.getOrganization().getId()+"\",\"organization._identifier\":\"\",\"createdBy\":\""+orderHeader.getUserContact().getId()+"\",\"updatedBy\":\""+orderHeader.getUserContact().getId()+"\",\"salesTransaction\":\"true\",\"documentNo\":\""+orderHeader.getDocumentNo()+"\",\"documentStatus\":\"DR\",\"documentAction\":\"CO\",\"documentType\":\""+doctype+"\",\"documentType._identifier\":\"\",\"transactionDocument\":\""+doctype+"\",\"transactionDocument._identifier\":\"\",\"description\":\""+desc+"\",\"orderDate\":\""+orderHeader.getOrderDate().toString()+"\",\"scheduledDeliveryDate\":\""+orderHeader.getOrderDate().toString()+"\",\"accountingDate\":\""+orderHeader.getOrderDate().toString()+"\",\"businessPartner\":\""+bpID+"\",\"businessPartner._identifier\":\"\",\"invoiceAddress\":\""+bpartner_loc_id+"\",\"invoiceAddress._identifier\":\"\",\"partnerAddress\":\""+bpartner_loc_id+"\",\"partnerAddress._identifier\":\"\",\"printDiscount\":\"false\",\"currency\":\"304\",\"currency._identifier\":\"\",\"formOfPayment\":\"B\",\"paymentTerms\":\""+organization.getDsidefPospayterms()+"\",\"summedLineAmount\":0,\"paymentTerms._identifier\":\"\",\"invoiceTerms\":\"D\",\"deliveryTerms\":\"A\",\"freightCostRule\":\"I\",\"deliveryMethod\":\"P\",\"priority\":\"5\",\"warehouse\":\""+warehouse+"\",\"warehouse._identifier\":\"\",\"priceList\":\""+pricelist+"\",\"priceList._identifier\":\"\",\"userContact\":\""+orderHeader.getUserContact().getId()+"\",\"paymentMethod\":\""+organization.getDsidefPospaymethod()+"\",\"paymentMethod._identifier\":\"\",\"dSReceiptno\":\""+orderHeader.getIMEMImReceiptno()+"\",\"dSTotalpriceadj\":"+orderHeader.getIMEMImTotalpriceadj()+",\"dSPosno\":\""+orderHeader.getIMEMImPosno()+"\",\"dSEMDsRatesatisfaction\":\""+orderHeader.getIMEMImCustomersatisfaction()+"\",\"dSChargeAmt\":"+orderHeader.getIMEMImCustomersatisfaction()+",\"generateTemplate\":\"\",\"copyFromPO\":\"\",\"pickFromShipment\":\"\",\"receiveMaterials\":\"\",\"createInvoice\":\"\",\"addOrphanLine\":\"\",\"calculatePromotions\":\"\",\"quotation\":\"\",\"reservationStatus\":\"NR\",\"swSendorder\":\"\",\"swModorder\":\"\",\"swDelorder\":\"\",\"obwplGeneratepicking\":\"\",\"sWEMSwPostatus\":\"\",\"syncLandline\":\""+orderHeader.getSyncLandline()+"\",\"syncEmail\":\""+orderHeader.getSyncEmail()+"\",\"rCOxylaneno\":\""+oxylaneId+"\",\"rCMobileNo\":\""+orderHeader.getPhone()+"\"}");
					log.info(job.toString());
					final JsonToDataConverter toBaseOBObject = OBProvider.getInstance().get(JsonToDataConverter.class);
					// Converting JSON Object to Openbravo Business object 
					
					BaseOBObject bob = null;
					try {
						bob = toBaseOBObject.toBaseOBObject(job);
					} catch (ValidationException ve) {
						ve.printStackTrace();
					}
					
					// Database Connections and Database insertions 
					try {
						OBDal.getInstance().save(bob);
						OBDal.getInstance().flush();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
					SessionHandler.getInstance().commitAndStart();
				
					// Inserted Sales Order Header id(c_order_id)
					final String olineID = (String) bob.getId();
					log.info(olineID);
					int lineno = 0;
					for (Order order : obQuery2.list()) {
						
						log.info("inside for loop");
						lineno++;
						BigDecimal qtyordered = order.getOrderedQuantity().add(new BigDecimal(order.getIMEMImPcbqty()).add(new BigDecimal(order.getIMEMImUeqty())));
						String taxId;
						if (order.getTax() != null) {
							taxId = order.getTax().getId();
						} else {
							taxId = null;
						}
						JSONObject jobi = new JSONObject("{\"_entityName\":\"OrderLine\",\"client\":\""+order.getClient().getId()+"\",\"client._identifier\":\"\",\"organization\":\""+order.getOrganization().getId()+"\",\"organization._identifier\":\"\",\"createdBy\":\""+order.getUserContact().getId()+"\",\"updatedBy\":\""+order.getUserContact().getId()+"\",\"active\":\"true\",\"salesOrder\":\""+olineID+"\",\"salesOrder._identifier\":\"\",\"lineNo\":"+lineno+",\"businessPartner\":\""+bpID+"\",\"partnerAddress\":\""+bpartner_loc_id+"\",\"orderDate\":\""+order.getOrderDate().toString()+"\",\"product\":\""+order.getProduct()+"\",\"product._identifier\":\"\",\"warehouse\":\""+warehouse+"\",\"warehouse._identifier\":\"\",\"uOM\":\"100\",\"uOM._identifier\":\"\",\"orderedQuantity\":"+qtyordered+",\"reservedQuantity\":0,\"reservationStatus\":\"NR\",\"manageReservation\":\"\",\"managePrereservation\":\"\",\"currency\":\"304\",\"currency._identifier\":\"\",\"unitPrice\":"+order.getUnitPrice()+",\"attributeSetValue\":null,\"tax\":\""+taxId+"\",\"tax._identifier\":\"\",\"dSLotqty\":"+order.getIMEMImUeqty()+",\"dSLotprice\":"+order.getIMEMImUeprice()+",\"dSBoxqty\":"+order.getIMEMImPcbqty()+",\"dSBoxprice\":"+order.getIMEMImPcbprice()+",\"dSEMDSUnitQty\":"+order.getOrderedQuantity()+"}");
						
						BaseOBObject bobi = null;
						try {
							bobi = toBaseOBObject.toBaseOBObject(jobi);
						} catch (ValidationException ve) {
							ve.printStackTrace();
						}
						// Database Connections and Database insertions 
						try {
							OBDal.getInstance().save(bobi);
							OBDal.getInstance().flush();
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}
					OBDal.getInstance().commitAndClose();
					
					final List parameters = new ArrayList();
				    parameters.add(olineID);
				    parameters.add(orderHeader.getIMEMImReceiptno());
				    parameters.add(dateOrdered);
				    final String procedureName = "ds_order_post3";
				    String resp = CallStoredProcedure.getInstance().call(procedureName, parameters, null).toString();
					
				    log.info(resp);
				    if(resp.equals("1")) {
				    	for (Order order : obQuery2.list()) {
				    		OBDal.getInstance().remove(order);
				    	}
				    	OBDal.getInstance().commitAndClose();
				    }
				} catch (Exception e) {
					for (Order order : obQuery2.list()) {
						order.setImportErrorMessage("Error in Record");
						OBDal.getInstance().save(order);
						OBDal.getInstance().flush();
					}
					OBDal.getInstance().commitAndClose();
				    e.printStackTrace();
				}
			}
		}
		log.info("The End");
		//Create Document
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		// Create root element
		Element root = doc.createElement("Openbravo");
		root.setAttribute("response", "Success");
		doc.appendChild(root);
		String xml=null;
		try {
			TransformerFactory transfac = TransformerFactory.newInstance();
		    Transformer trans = transfac.newTransformer();
		    trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		    trans.setOutputProperty(OutputKeys.VERSION, "1.0");
		    trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		    trans.setOutputProperty(OutputKeys.INDENT, "yes");
		    // create string from xml tree
		    StringWriter sw = new StringWriter();
		    StreamResult result = new StreamResult(sw);
		    DOMSource source = new DOMSource(doc);
		    trans.transform(source, result);
		    xml = sw.toString();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		// writing to the response i.e. 'created' 
		response.setContentType("text/xml");
		response.setCharacterEncoding("utf-8");
	    response.setHeader("Content-Encoding", "UTF-8");
	    final Writer w = response.getWriter();
	    w.write(xml);
	    w.close();
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	  /**
		 * Creates a HTTP connection.
		 * 
		 * @param wsPart
		 * @param method
		 *          POST, PUT, GET or DELETE
		 * @return the created connection
		 * @throws Exception
		 */
		protected HttpURLConnection createConnection(Order iOrder, String key) throws Exception {
			
			String customerDBURL = "";
			String customerDBUName = "";
			String customerDBPwd = "";
			OBContext.setAdminMode();
			final Map<String, String> custmerDBConfig = new HashMap<String, String>();
			OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
			configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.customerdb"));
			if(configInfoObCriteria.count() > 0) {
				for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
					custmerDBConfig.put(config.getKey(), config.getSearchKey());
				}
				customerDBURL = custmerDBConfig.get("customerdbWSURL");
				customerDBUName = custmerDBConfig.get("custSearchId");
				customerDBPwd = custmerDBConfig.get("custSearchPwd");
			}
			OBContext.restorePreviousMode();
			
			URL url = null; 
			if(key.equals("old")) {
				if(iOrder.getPhone() != null) {
					url = new URL(customerDBURL+"/dsiCustomerdbGet?mobileno="+iOrder.getPhone()+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
					log.info(customerDBURL+"/dsiCustomerdbGet?mobileno="+iOrder.getPhone()+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
				} else if(iOrder.getSyncEmail() != null) {
					url = new URL(customerDBURL+"/dsiCustomerdbGet?email="+iOrder.getSyncEmail()+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
					log.info(customerDBURL+"/dsiCustomerdbGet?email="+iOrder.getSyncEmail()+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
				} else if(iOrder.getSyncLandline() != null) {
					url = new URL(customerDBURL+"/dsiCustomerdbGet?landline="+iOrder.getSyncLandline()+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
					log.info(customerDBURL+"/dsiCustomerdbGet?landline="+iOrder.getSyncLandline()+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
				}
			} else {
				url = new URL(customerDBURL+"/dsiCustomerdbPost?mobileno="+getNotNull(iOrder.getPhone())+"&landline="+getNotNull(iOrder.getSyncLandline())+"&email="+getNotNull(iOrder.getSyncEmail())+"&name=Anonymous&name2=Customer&em_sms_alert=N&em_email_alert=N&store="+getNotNull(iOrder.getIMEMImStorename())+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
				log.info(customerDBURL+"/dsiCustomerdbPost?mobileno="+getNotNull(iOrder.getPhone())+"&landline="+getNotNull(iOrder.getSyncLandline())+"&email="+getNotNull(iOrder.getSyncEmail())+"&name=Anonymous&name2=Customer&em_sms_alert=N&em_email_alert=N&store="+getNotNull(iOrder.getIMEMImStorename())+"&username="+customerDBUName+"&pwd="+customerDBPwd+"");
			}
			
			final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
			if(key.equals("old")) 
				hc.setRequestMethod("GET");
			if(key.equals("new")) 
				hc.setRequestMethod("POST");
		  	hc.setAllowUserInteraction(false);
		  	hc.setDefaultUseCaches(false);
		  	hc.setDoInput(true);
		  	hc.setInstanceFollowRedirects(true);
		  	hc.setUseCaches(false);
			return hc;
		}
		
		private String getNotNull(String string) {
			if (null == string || string.equals("null")) {
				return "";
			} else
				return string;
		}
}
