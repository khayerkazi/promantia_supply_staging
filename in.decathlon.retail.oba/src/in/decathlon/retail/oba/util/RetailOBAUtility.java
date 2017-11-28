package in.decathlon.retail.oba.util;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;
import in.decathlon.retail.oba.reporting.ItemWisePriceBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.pricing.pricelist.ProductPrice;

import com.oxit.oagis.extended.AmountType;
import com.oxit.oagis.extended.ApplicationAreaType;
import com.oxit.oagis.extended.DateTimeType;
import com.oxit.oagis.extended.IdentifierType;
import com.oxit.oagis.extended.ItemType;
import com.oxit.oagis.extended.ProfitActivityType;
import com.oxit.oagis.extended.SellingPriceDiscrepancyType;
import com.oxit.oagis.extended.SourceActivityType;
import com.oxit.oagis.extended.SyncProfitActivityDataAreaType;
import com.oxit.oagis.extended.SyncProfitActivityDocument;
import com.oxit.oagis.extended.SyncProfitActivityType;
import com.oxit.oagis.extended.TypedAmountType;

public class RetailOBAUtility {

	private static DecimalFormat TWO_DECIMALS_FORMAT = new DecimalFormat("#.##");
	private static String NAMESPACE = "http://www.oxylane.com/oxit/oagis";
	private static String PREFIX = "oxit";
	private static final Logger LOG = Logger.getLogger(RetailOBAUtility.class);
	private static DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	private static DateFormat FORMATTERFILE = new SimpleDateFormat("yyyyMMdd");
	
	// Fetches all the configuration details from dsidef_module_config table
	public Map<String, String> getOBAConfigurations() {
		
		final Map<String, String> configDetails = new HashMap<String, String>();
		OBContext.setAdminMode();
		OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
		configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.retail.oba"));
		if(configInfoObCriteria.count() > 0) {
			for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
				configDetails.put(config.getKey(), config.getSearchKey());
			}
		}
		OBContext.restorePreviousMode();
		return configDetails;
	}
	
	public List<String> fetchUniqueOrganizations(int NnoOfDaysBack) throws ParseException {
		
		final List<String> organizationList = new ArrayList<String>();
		
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, NnoOfDaysBack);
		final String yesterdaysDate = FORMATTER.format(cal.getTime());
		
		final Calendar cal2 = Calendar.getInstance();
		cal2.add(Calendar.DATE, NnoOfDaysBack + 1);
		final String todaysDate = FORMATTER.format(cal2.getTime());
		
		final OBQuery invoiceObCriteria = OBDal.getInstance().createQuery(Invoice.class, "as i where i.invoiceDate >= '"+ yesterdaysDate + " 00:00:00" + "' and i.invoiceDate < '"+ todaysDate + " 00:00:00" + "' and i.documentStatus='CO'");
//		invoiceObCriteria.add(Restrictions.between(Invoice.PROPERTY_INVOICEDATE, FORMATTER.parse(yesterdaysDate), new Date()));
//		invoiceObCriteria.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CO"));
		invoiceObCriteria.setSelectClause("distinct i.organization.id");
		
		LOG.info("distinct invoiceObCriteria size : "+invoiceObCriteria.count());
		if(invoiceObCriteria.count() > 0) {
			for (Object invoice : invoiceObCriteria.list()) {
				organizationList.add(invoice.toString());
			}
//			invoicesToday = invoiceObCriteria.list();
		}
		return organizationList;
	}
	public List<Invoice> fetchInvoicesOfYesterday(String orgId, int NnoOfDaysBack) throws ParseException {
		
		List<Invoice> invoicesToday = null;
		
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, NnoOfDaysBack);
		final String yesterdaysDate = FORMATTER.format(cal.getTime());
		
		final Calendar cal2 = Calendar.getInstance();
		cal2.add(Calendar.DATE, NnoOfDaysBack + 1);
		final String todaysDate = FORMATTER.format(cal2.getTime());
		
		final OBCriteria<Invoice> invoiceObCriteria = OBDal.getInstance().createCriteria(Invoice.class);
		invoiceObCriteria.add(Restrictions.ge(Invoice.PROPERTY_INVOICEDATE, FORMATTER.parse(yesterdaysDate + " 00:00:00")));
		invoiceObCriteria.add(Restrictions.lt(Invoice.PROPERTY_INVOICEDATE, FORMATTER.parse(todaysDate + " 00:00:00")));
		invoiceObCriteria.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CO"));
		invoiceObCriteria.add(Restrictions.eq(Invoice.PROPERTY_ORGANIZATION, OBDal.getInstance().get(Organization.class, orgId)));
		invoiceObCriteria.setFilterOnReadableOrganization(false);
		
		LOG.info("invoiceObCriteria size : "+invoiceObCriteria.count());
		if(invoiceObCriteria.count() > 0) {
			invoicesToday = invoiceObCriteria.list();
		}
		return invoicesToday;
	}
	
	public Map<String, ItemWisePriceBean> convertIntoItemwiseBean(List<Invoice> invoicesOfYesterday) {
		
		final Map<String, ItemWisePriceBean> itemMap = new HashMap<String, ItemWisePriceBean>();
		
		for (Invoice invoice : invoicesOfYesterday) {
			
			for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
				
				if(null != itemMap.get(invoiceLine.getProduct().getName())) {
					
					final ItemWisePriceBean itemWisePriceBean = itemMap.get(invoiceLine.getProduct().getName());
					
					final OBCriteria<ProductPrice> clPriceHistoryCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
					clPriceHistoryCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, invoiceLine.getProduct()));
					clPriceHistoryCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_ORGANIZATION, invoiceLine.getOrganization()));
					clPriceHistoryCriteria.setFilterOnReadableOrganization(false);
					clPriceHistoryCriteria.setMaxResults(1);
					Double cashierMargin = 0.0;
					Double sellingPriceDescripancy = 0.0;
					if(clPriceHistoryCriteria.count() > 0) {
						
						final ProductPrice priceHistory = clPriceHistoryCriteria.list().get(0);
						
						cashierMargin = priceHistory.getClCcunitprice().doubleValue() - invoiceLine.getUnitPrice().doubleValue();
						sellingPriceDescripancy = (priceHistory.getClCcunitprice().doubleValue() * invoiceLine.getInvoicedQuantity().longValue()) - invoiceLine.getGrossAmount().doubleValue();
					}
					
					itemWisePriceBean.setInvoicedQuantity(itemWisePriceBean.getInvoicedQuantity() + invoiceLine.getInvoicedQuantity().longValue());
					itemWisePriceBean.setTurnoverIncludingTax(itemWisePriceBean.getTurnoverIncludingTax() + invoiceLine.getGrossAmount().doubleValue());
					itemWisePriceBean.setCashierMargin(itemWisePriceBean.getCashierMargin() + cashierMargin);
					itemWisePriceBean.setTurnoverExcludingTax(itemWisePriceBean.getTurnoverExcludingTax() + invoiceLine.getLineNetAmount().doubleValue());
					itemWisePriceBean.setSellingPriceDiscrepancy(itemWisePriceBean.getSellingPriceDiscrepancy() + sellingPriceDescripancy);
					
					itemMap.put(invoiceLine.getProduct().getName(), itemWisePriceBean);
				} else {
					
					final ItemWisePriceBean itemWisePriceBean = new ItemWisePriceBean();
					
					final OBCriteria<ProductPrice> clPriceHistoryCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
					clPriceHistoryCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, invoiceLine.getProduct()));
					clPriceHistoryCriteria.add(Restrictions.eq(ProductPrice.PROPERTY_ORGANIZATION, invoiceLine.getOrganization()));
					clPriceHistoryCriteria.setFilterOnReadableOrganization(false);
					clPriceHistoryCriteria.setMaxResults(1);
					Double cashierMargin = 0.0;
					Double sellingPriceDescripancy = 0.0;
					if(clPriceHistoryCriteria.count() > 0) {
						
						final ProductPrice priceHistory = clPriceHistoryCriteria.list().get(0);
						
						cashierMargin = priceHistory.getClCcunitprice().doubleValue() - invoiceLine.getUnitPrice().doubleValue();
						sellingPriceDescripancy = (priceHistory.getClCcunitprice().doubleValue() * invoiceLine.getInvoicedQuantity().longValue()) - invoiceLine.getGrossAmount().doubleValue();
					}
					
					itemWisePriceBean.setInvoicedQuantity(invoiceLine.getInvoicedQuantity().longValue());
					itemWisePriceBean.setTurnoverIncludingTax(invoiceLine.getGrossAmount().doubleValue());
					itemWisePriceBean.setCashierMargin(cashierMargin);
					itemWisePriceBean.setTurnoverExcludingTax(invoiceLine.getLineNetAmount().doubleValue());
					itemWisePriceBean.setSellingPriceDiscrepancy(sellingPriceDescripancy);
					
					itemMap.put(invoiceLine.getProduct().getName(), itemWisePriceBean);
				}
			}
		}
		
		return itemMap;
	}
	
	public boolean generatePurchaseOrderXMLInSAPServer(
			Map<String, ItemWisePriceBean> itemWiseInvoiceMap,
			Map<String, String> configDetails, Organization currentOrg) throws IOException {
		
		boolean status = false;
		// Declare SyncProfitActivity document
		final SyncProfitActivityDocument syncProfitActivityDocument = SyncProfitActivityDocument.Factory.newInstance();
		
		// Adding SyncProfitActivity root tag
		final SyncProfitActivityType syncProfitActivity =  syncProfitActivityDocument.addNewSyncProfitActivity();
		
		// Application Area
		final ApplicationAreaType applicationArea = syncProfitActivity.addNewApplicationArea();
		// Receiver tag
		final IdentifierType receiverIdentifierType = applicationArea.addNewReceiver().addNewID();
		receiverIdentifierType.setSchemeName("EAN");
		receiverIdentifierType.setStringValue("13348364340001");
		// CreationDateTime tag
		final DateTimeType creationDateTime = DateTimeType.Factory.newInstance();
		creationDateTime.setStringValue(FORMATTER.format(new Date()));
		applicationArea.xsetCreationDateTime(creationDateTime);
		
		// Data Area
		final SyncProfitActivityDataAreaType syncProfitActivityDataArea = syncProfitActivity.addNewDataArea();
		
		syncProfitActivityDataArea.addNewSync();
		
		final ProfitActivityType profitActivity = syncProfitActivityDataArea.addNewProfitActivity();
		
		// Party tag
		final IdentifierType partyIdentifierType = profitActivity.addNewParty().addNewPartyIDs().addNewID();
		partyIdentifierType.setSchemeName("EAN13");
		partyIdentifierType.setStringValue(configDetails.get("EAN_for_" + currentOrg.getName()));
		// ActivityDateTime tag
		final DateTimeType activityDateTime = DateTimeType.Factory.newInstance();
		
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, Integer.parseInt(configDetails.get("NnoOfDaysBack")));
		
		activityDateTime.setStringValue(FORMATTER.format(cal.getTime()));
		profitActivity.xsetActivityDateTime(activityDateTime);
		
		for (Map.Entry<String, ItemWisePriceBean> entry : itemWiseInvoiceMap.entrySet()) {
			
			// SourceActivity tag
			final SourceActivityType sourceActivity = profitActivity.addNewSourceActivity();
			// Item tag
			final ItemType item = sourceActivity.addNewItem();
			final IdentifierType itemIdentifierType = item.addNewItemID().addNewID();
			itemIdentifierType.setSchemeName("ArticleCode");
			itemIdentifierType.setSchemeAgencyName("SAP");
			itemIdentifierType.setStringValue(entry.getKey());
			item.addNewQuantity().setStringValue(entry.getValue().getInvoicedQuantity().toString());
			
			// TurnoverIncludingTax
			final TypedAmountType turnoverIncludingTaxTypedAmount = sourceActivity.addNewTypedAmount();
			turnoverIncludingTaxTypedAmount.setType("TurnoverIncludingTax");
			AmountType turnoverIncludingTaxAmount = turnoverIncludingTaxTypedAmount.addNewAmount();
			turnoverIncludingTaxAmount.setCurrencyID("INR");
			turnoverIncludingTaxAmount.setStringValue(TWO_DECIMALS_FORMAT.format(entry.getValue().getTurnoverIncludingTax()));
			
			// CashierMargin
			final TypedAmountType CashierMarginTypedAmount = sourceActivity.addNewTypedAmount();
			CashierMarginTypedAmount.setType("CashierMargin");
			AmountType CashierMarginAmount = CashierMarginTypedAmount.addNewAmount();
			CashierMarginAmount.setCurrencyID("INR");
			CashierMarginAmount.setStringValue(TWO_DECIMALS_FORMAT.format(entry.getValue().getCashierMargin()));
			
			// TurnoverExcludingTax
			final TypedAmountType TurnoverExcludingTaxTypedAmount = sourceActivity.addNewTypedAmount();
			TurnoverExcludingTaxTypedAmount.setType("TurnoverExcludingTax");
			AmountType TurnoverExcludingTaxAmount = TurnoverExcludingTaxTypedAmount.addNewAmount();
			TurnoverExcludingTaxAmount.setCurrencyID("INR");
			TurnoverExcludingTaxAmount.setStringValue(TWO_DECIMALS_FORMAT.format(entry.getValue().getTurnoverExcludingTax()));
			
			// SellingPriceDiscrepancy
			final SellingPriceDiscrepancyType sellingPriceDiscrepancy = sourceActivity.addNewSellingPriceDiscrepancy();
			sellingPriceDiscrepancy.setType("P");
			AmountType SellingPriceDiscrepancyAmount = sellingPriceDiscrepancy.addNewAmount();
			SellingPriceDiscrepancyAmount.setCurrencyID("INR");
			SellingPriceDiscrepancyAmount.setStringValue(TWO_DECIMALS_FORMAT.format(entry.getValue().getSellingPriceDiscrepancy()));
		}
		String filename = generateOutput(syncProfitActivityDocument, configDetails.get("TO_XMLFileNamePrefix") + currentOrg.getName() + "_", Integer.parseInt(configDetails.get("NnoOfDaysBack")));
		try {
			if(moveToServerThroughFTPS(configDetails.get("serverHost"), configDetails.get("serverUserName"), configDetails.get("serverPassword"), configDetails.get("TO_folderInSAP"), filename)) {
				LOG.info("The "+filename+" is uploaded successfully!");
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		status = true;
		return status;
	}
	
	private String generateOutput(
			SyncProfitActivityDocument syncProfitActivityDocument,
			String filename, int NnoOfDaysBack) throws IOException  {

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, NnoOfDaysBack);
		
		filename = filename + FORMATTERFILE.format(cal.getTime()) + ".xml";
		LOG.info("In generateOutput() methd");
		final Map<String, String> prefixes = new HashMap<String, String>();
		prefixes.put(NAMESPACE, PREFIX);
		final XmlOptions xmlOptions = new XmlOptions();
		xmlOptions.setCharacterEncoding("UTF-8");
		xmlOptions.setSaveAggressiveNamespaces();
		xmlOptions.setSaveSuggestedPrefixes(prefixes);

		// Can help to understand the XML but with big xml we should remove pretty print
		xmlOptions.setSavePrettyPrint();
		syncProfitActivityDocument.save(new File(filename), xmlOptions);
		
		return filename;
	}
	
	private boolean moveToServerThroughFTPS(String host, String userName,
			String password, String toStockPath, String filename) throws NoSuchAlgorithmException {

		final FTPSClient ftpsClient = new FTPSClient();
		boolean completed = false;
		try {
			ftpsClient.connect(host);
			ftpsClient.login(userName, password);
			ftpsClient.enterLocalPassiveMode();
			ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);

			// APPROACH #2: uploads second file using an OutputStream
			final File secondLocalFile = new File(filename);
			final String secondRemoteFile = toStockPath + filename.split("/")[filename.split("/").length-1];
			final InputStream inputStream = new FileInputStream(secondLocalFile);

			LOG.info("Start uploading file");
			final OutputStream outputStream = ftpsClient
					.storeFileStream(secondRemoteFile);
			byte[] bytesIn = new byte[4096];
			int read = 0;

			while ((read = inputStream.read(bytesIn)) != -1) {
				outputStream.write(bytesIn, 0, read);
			}
			outputStream.close();

			completed = ftpsClient.completePendingCommand();

		} catch (IOException e) {
			LOG.info("Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (ftpsClient.isConnected()) {
					ftpsClient.logout();
					ftpsClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return completed;
	}
	
}
