package in.decathlon.supply.oba.reporting;

import in.decathlon.defaults.configuration.data.DSIDEFModuleConfig;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.web.WebService;

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
import com.sysfore.catalog.CLPriceHistory;

public class SyncProfitActivityXMLGenerator implements WebService {

	private static final Logger log4j = Logger.getLogger(SyncProfitActivityXMLGenerator.class);
	private static String NAMESPACE = "http://www.oxylane.com/oxit/oagis";
	private static String PREFIX = "oxit";
//	private static String filename = "/home/anvesh/OBA/TO_STOCK/OBA_FLOW_TO_INDIA_Sample.xml";
	private static DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	private static DateFormat FORMATTERFILE = new SimpleDateFormat("yyyyMMdd");
	
	@Override
	public void doGet(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		
		try {
			
			final Map<String, String> configDetails = new HashMap<String, String>();
			// Fetching mail configurations from dsidef_module_config table
			OBContext.setAdminMode();
			OBCriteria<DSIDEFModuleConfig> configInfoObCriteria = OBDal.getInstance().createCriteria(DSIDEFModuleConfig.class);
			configInfoObCriteria.add(Restrictions.eq(DSIDEFModuleConfig.PROPERTY_MODULENAME, "in.decathlon.supply.oba"));
			if(configInfoObCriteria.count() > 0) {
				for (DSIDEFModuleConfig config : configInfoObCriteria.list()) {
					configDetails.put(config.getKey(), config.getSearchKey());
				}
			}
			OBContext.restorePreviousMode();
			
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
			
			final List<Invoice> invoicesToday = fetchCInvoiceDetails();
			
			final Map<String, ItemWisePriceBean> itemMap = new HashMap<String, ItemWisePriceBean>();
			int lines=0;
			int newLines=0;
			for (Invoice invoice : invoicesToday) {
				for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {
					lines++;
					if(null != itemMap.get(invoiceLine.getProduct().getName())) {
						final ItemWisePriceBean itemWisePriceBean = itemMap.get(invoiceLine.getProduct().getName());
						
						final OBCriteria<ProductPrice> clPriceHistoryCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
						clPriceHistoryCriteria.add(Restrictions.eq(CLPriceHistory.PROPERTY_PRODUCT, invoiceLine.getProduct()));
						clPriceHistoryCriteria.add(Restrictions.eq(CLPriceHistory.PROPERTY_ORGANIZATION, invoiceLine.getOrganization()));
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
						newLines++;
						final ItemWisePriceBean itemWisePriceBean = new ItemWisePriceBean();
						
						final OBCriteria<ProductPrice> clPriceHistoryCriteria = OBDal.getInstance().createCriteria(ProductPrice.class);
						clPriceHistoryCriteria.add(Restrictions.eq(CLPriceHistory.PROPERTY_PRODUCT, invoiceLine.getProduct()));
						clPriceHistoryCriteria.add(Restrictions.eq(CLPriceHistory.PROPERTY_ORGANIZATION, invoiceLine.getOrganization()));
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
			
			log4j.info("new Lines"+newLines);
			log4j.info("Lines"+lines);
			
			generateProfitActivityTag(syncProfitActivityDataArea, itemMap, configDetails.get("EAN_India"));
			
			final String filename = configDetails.get("TO_XMLFileNamePrefix")+FORMATTERFILE.format(new Date())+".xml";
			
			try {
				generateOutput(syncProfitActivityDocument, filename);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	private List<Invoice> fetchCInvoiceDetails() throws ParseException {

		List<Invoice> invoicesToday = null;
		// fetches the todays invoice records from c_invoice
		final String todaysDate = FORMATTER.format(new Date());
		final OBCriteria<Invoice> invoiceObCriteria = OBDal.getInstance().createCriteria(Invoice.class);
		invoiceObCriteria.add(Restrictions.between(Invoice.PROPERTY_CREATIONDATE, FORMATTER.parse(todaysDate), new Date()));
		invoiceObCriteria.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CO"));
		
		log4j.info("invoiceObCriteria size : "+invoiceObCriteria.count());
		if(invoiceObCriteria.count() > 0) {
			invoicesToday = invoiceObCriteria.list();
		}
		return invoicesToday;
	}

	private void generateProfitActivityTag(
			SyncProfitActivityDataAreaType syncProfitActivityDataArea, final Map<String, ItemWisePriceBean> itemMap, String eanIndia) {

		try {
			
			log4j.info(itemMap.size());
			// Sync tag
			syncProfitActivityDataArea.addNewSync();
			
			final ProfitActivityType profitActivity = syncProfitActivityDataArea.addNewProfitActivity();
			
			// Party tag
			final IdentifierType partyIdentifierType = profitActivity.addNewParty().addNewPartyIDs().addNewID();
			partyIdentifierType.setSchemeName("EAN13");
			partyIdentifierType.setStringValue(eanIndia);
			// ActivityDateTime tag
			final DateTimeType activityDateTime = DateTimeType.Factory.newInstance();
			activityDateTime.setStringValue(FORMATTER.format(new Date()));
			profitActivity.xsetActivityDateTime(activityDateTime);
			
			for (Map.Entry<String, ItemWisePriceBean> entry : itemMap.entrySet()) {
				System.out.println("Key : " + entry.getKey() + " Value : "
					+ entry.getValue());
				
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
				turnoverIncludingTaxAmount.setStringValue(entry.getValue().getTurnoverIncludingTax().toString());
				
				// CashierMargin
				final TypedAmountType CashierMarginTypedAmount = sourceActivity.addNewTypedAmount();
				CashierMarginTypedAmount.setType("CashierMargin");
				AmountType CashierMarginAmount = CashierMarginTypedAmount.addNewAmount();
				CashierMarginAmount.setCurrencyID("INR");
				CashierMarginAmount.setStringValue(entry.getValue().getCashierMargin().toString());
				
				// TurnoverExcludingTax
				final TypedAmountType TurnoverExcludingTaxTypedAmount = sourceActivity.addNewTypedAmount();
				TurnoverExcludingTaxTypedAmount.setType("TurnoverExcludingTax");
				AmountType TurnoverExcludingTaxAmount = TurnoverExcludingTaxTypedAmount.addNewAmount();
				TurnoverExcludingTaxAmount.setCurrencyID("INR");
				TurnoverExcludingTaxAmount.setStringValue(entry.getValue().getTurnoverExcludingTax().toString());
				
				// SellingPriceDiscrepancy
				final SellingPriceDiscrepancyType sellingPriceDiscrepancy = sourceActivity.addNewSellingPriceDiscrepancy();
				sellingPriceDiscrepancy.setType("P");
				AmountType SellingPriceDiscrepancyAmount = sellingPriceDiscrepancy.addNewAmount();
				SellingPriceDiscrepancyAmount.setCurrencyID("INR");
				SellingPriceDiscrepancyAmount.setStringValue(entry.getValue().getSellingPriceDiscrepancy().toString());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void generateOutput(
			SyncProfitActivityDocument syncProfitActivityDocument,
			String filename) throws IOException  {

		log4j.info("In generateOutput() methd");
		final Map<String, String> prefixes = new HashMap<String, String>();
		prefixes.put(NAMESPACE, PREFIX);
		final XmlOptions xmlOptions = new XmlOptions();
		xmlOptions.setCharacterEncoding("UTF-8");
		xmlOptions.setSaveAggressiveNamespaces();
		xmlOptions.setSaveSuggestedPrefixes(prefixes);

		// Can help to understand the XML but with big xml we should remove pretty print
		xmlOptions.setSavePrettyPrint();
		syncProfitActivityDocument.save(new File(filename), xmlOptions);
	}

	@Override
	public void doPost(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void doPut(String path, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		throw new UnsupportedOperationException();
	}

}
