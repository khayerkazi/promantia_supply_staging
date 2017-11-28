package in.decathlon.journalentry;

import in.decathlon.ibud.commons.BusinessEntityMapper;
import in.decathlon.ibud.orders.client.SOConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Duration;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.geography.Location;
import org.openbravo.model.common.geography.Region;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.CategoryAccounts;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.sankalpcrm.RCCompany;

public class LedgerTransactionGenerator extends DalBaseProcess {
  private static final String LTL_TYPE_DLVY_CHRG = "D";
  private static final String ACCT_TYPE_REVENUE = "R";
  private static final String LTL_TYPE_OTHERS = "O";
  private static final String LTL_TYPE_SALES = "S";
  private static final String LTL_TYPE_TAX = "T";
  private static final String ACCOUNT_TYPE_ASSET = "A";
  private ElementValue chrgAcct;
  private static String processId = "";

  enum ADJ_TYPE {
    TAX, SALES, PAYMENT
  };

  class MapValues {
    ElementValue element_val;
    TaxRate tax_rate;
    BigDecimal creditAmt;
  };

  ProcessLogger logMonitor;
  Logger log = Logger.getLogger(getClass());

  private enum LedgerMappingType {
    ORG_ELEMENT_TAX, STATE_ELEMENT_TAX, ORG_ELEMENT_NOTAX, STATE_ELEMENT_NOTAX, PARENT_ELEMENT_TAX, PARENT_ELEMENT_NOTAX
  };

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // use it for all org in same process
    logMonitor = bundle.getLogger();
    processId = bundle.getProcessId();
    String logMessage;
    List<Object[]> groups;
    Duration duration;

    try {

      chrgAcct = getChargeAccount();

      Date beginTime = new Date();
      Date startTime;

      generateEcomTransactions("B2B", SOConstants.B2BOrgId, processId);

      generateEcomTransactions("ECOM", SOConstants.ECOMORGID, processId);

      // POS Process

      List<Organization> orgList = BusinessEntityMapper.getStoreOrganizations();
      List<Timestamp> txnDates = new ArrayList<Timestamp>();
      for (Organization org : orgList) {
        startTime = new Date();
        logMessage = "\n Started POS ledger accounting Entries for " + org.getName();
        log.info(logMessage);
        logMonitor.log(logMessage);
        log.debug("------------LTG-- POS query to retrieve all records begin ---" + new Date());
        txnDates = getPosLedgerSets(org.getId());
        log.debug("------------LTG-- POS query to retrieve all records end ---" + new Date());
        for (Timestamp txnDate : txnDates) {

          String dateString = new SimpleDateFormat("yyyy-MM-dd").format(txnDate);
          log.debug(" Date " + dateString);
          createLedgerForPOS(queryForFactAcctListByDate(dateString, org.getId()), SOConstants.POS,
              processId);
        }
        duration = new Duration(new Date().getTime() - startTime.getTime());
        logMessage = " Finished POS ledger accounting Entries " + org.getName() + " in "
            + duration.toString();
        log.info(logMessage);
        logMonitor.log(logMessage);

      }

      duration = new Duration(new Date().getTime() - beginTime.getTime());
      logMessage = " Finished run  in " + duration.toString();
      log.info(logMessage);
      logMonitor.log(logMessage);
    } catch (Exception e) {
      log.error("Processing Ledger Txns", e);
      logMonitor.logln("\n Error : " + e);
      BusinessEntityMapper.rollBackNlogError(e, processId, null);
    }
  }

  private void generateEcomTransactions(String channelType, String channelOrgId, String processId)
      throws Exception {

    String logMessage;
    List<Object[]> groups;
    Duration duration;
    Date startTime;
    Timestamp currentTxnDate;
    String currentGroupId;
    try {
      startTime = new Date();
      logMessage = "\n Started " + channelType + " ledger accounting Entries";
      log.info(logMessage);
      logMonitor.log(logMessage);
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      cal.add(Calendar.YEAR, -5);
      currentTxnDate = new Timestamp(cal.getTime().getTime());
      currentGroupId = "";
      do {
        log.debug("------------LTG-- ecom query to retrieve all records begin ---" + new Date());
        groups = getEcomLedgerSets(channelOrgId, currentTxnDate, currentGroupId);
        log.debug("------------LTG-- ecom query to retrieve all records end ---" + new Date());
        log.debug("No of groups is " + groups.size());
        for (Object[] arr : groups) {
          log.debug("Date " + arr[0] + " Group Id" + arr[1]);
          log.debug("------------LTG-- ecom query to retrieve records for perticular grp begin ---"
              + new Date());
          String qryString = queryForFactAcctListByGroup((String) arr[1]);
          OBQuery<AccountingFact> accntRecords = OBDal.getInstance().createQuery(
              AccountingFact.class, qryString);
          List<AccountingFact> accntList = accntRecords.list();
          log.debug("------------LTG-- ecom query to retrieve records for perticular grp end ---"
              + new Date());
          createLedgerTxn(accntList, SOConstants.ECOM, processId);
        }
        if (groups.size() > 0) {
          currentTxnDate = (Timestamp) groups.get(groups.size() - 1)[0];
          currentGroupId = (String) groups.get(groups.size() - 1)[1];

        }
      } while (groups.size() > 0);
      duration = new Duration(new Date().getTime() - startTime.getTime());
      logMessage = " Finished " + channelType + " ledger accounting Entries in "
          + duration.toString();
      log.info(logMessage);
      logMonitor.log(logMessage);
      // return groups;
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      logMonitor.log("\nError:" + e);
      BusinessEntityMapper.rollBackNlogError(e, processId, null);
    }
  }

  private List<Object[]> getEcomLedgerSets(String orgId, Timestamp transactonDate, String groupId) {
    // String dateString = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(transactonDate);

    String qryString = "select distinct transactionDate, groupID from "
        + "FinancialMgmtAccountingFact af where djeProcessed = false " + "and organization.id = '"
        + orgId + "' and table.id = '318' and (transactionDate > '" + transactonDate
        + "' or (transactionDate = '" + transactonDate + "' and groupID > '" + groupId + "'))"
        + " and documentCategory='ARI' order by transactionDate , groupID ";
    final Query qry = OBDal.getInstance().getSession().createQuery(qryString);
    log.debug(qry);
    qry.setMaxResults(20);
    return qry.list();

  }

  private List<Timestamp> getPosLedgerSets(String orgId) {
    String qryString = "select distinct transactionDate  from "
        + "FinancialMgmtAccountingFact af where djeProcessed = false " + "and organization.id = '"
        + orgId + "' and table.id = '318' and documentCategory='ARI' order by transactionDate ";
    final Query qry = OBDal.getInstance().getSession().createQuery(qryString);
    return qry.list();

  }

  private String queryForFactAcctListByGroup(String groupID) {
    String qryString = "fa where  groupID = '" + groupID + "'";
    return qryString;
  }

  private String queryForFactAcctListByDate(String dateString, String orgId) {
    String qryString = "fa where  transactionDate = '" + dateString + "' and organization.id = '"
        + orgId + "' and table.id = '318' and documentCategory='ARI'";
    return qryString;
  }

  private void createLedgerForPOS(String qryString, String pos, String processId) throws Exception {
    log.debug("------------LTG-- pos query to retrieve records for perticular date begin ---"
        + new Date());
    OBQuery<AccountingFact> accntRecords = OBDal.getInstance().createQuery(AccountingFact.class,
        qryString);

    List<AccountingFact> accntList = accntRecords.list();
    log.debug("------------LTG-- pos query to retrieve records for perticular date end ---"
        + new Date());
    createLedgerTxn(accntList, pos, processId);
  }

  private void createLedgerTxn(List<AccountingFact> accntList, String typeOfOrg, String processId) {
    // logMonitor.log("Started creation of ledger for sales invoice " +
    // accntList.get(0).getRecordID());
    AccountingFact aFactLine = accntList.get(0);
    String ledgerDescription = "";
    String ledgerAlias = "";
    String ledgerName = "";
    LedgerTransaction txn = null;
    LedgerMapping map = null;
    JSONObject errorLogObj = new JSONObject();
    // test it
    try {

      if (accntList.size() > 0) {
        // errorLogObj.put(SOConstants.RECORD_ID, accntList.get(0).getRecordID());
        txn = createLedgerTrxnHeader(aFactLine, typeOfOrg);
      }

      for (AccountingFact factAccntLine : accntList) {
        ledgerDescription = null;
        ledgerAlias = null;
        ledgerName = null;
        aFactLine = factAccntLine;
        errorLogObj.put("Sales invoice ID ", factAccntLine.getRecordID());
        LedgerTransactionLeg ltl = null;
        InvoiceLine invLine = null;
        String invlineId = factAccntLine.getLineID();
        if (invlineId != null) {
          invLine = OBDal.getInstance().get(InvoiceLine.class, invlineId);
        }
        map = computeLedgerMapping(aFactLine, invLine);

        if (map == null && typeOfOrg.equals(SOConstants.POS)
            && (aFactLine.getAccount().getAccountType().equals(ACCOUNT_TYPE_ASSET))) {
          map = computeDefaultLedgerMapping(aFactLine.getOrganization());
        }
        // there is no map defined for pos and B2B,ECOM with credit records
        // TODO : Why POS and ECOM ? Better than credit would be type of account as Income
        if (map == null
            && (typeOfOrg.equals(SOConstants.POS) || (typeOfOrg.equals(SOConstants.ECOM) && aFactLine
                .getCredit().compareTo(BigDecimal.ZERO) > 0))) {
          throw new OBException("No mapping defined for the org " + aFactLine.getOrganization()
              + " with line " + aFactLine);
        }
        if (map != null) {
          ledgerDescription = map.getDescription();
          ledgerAlias = map.getLedgerAlias();
          ledgerName = map.getLedgerName();
        }
        for (LedgerTransactionLeg cLeg : txn.getDJELedgerTxnLegList()) {

          if (cLeg.getLedgerName() != null && cLeg.getLedgerName().equals(ledgerName)) {
            if (typeOfOrg.equals(SOConstants.POS)) {
              if (cLeg.getLedgerAlias() != null && cLeg.getLedgerAlias().equals(ledgerAlias)) {
                ltl = cLeg;
                break;
              }
            } else {
              ltl = cLeg;
              break;
            }
          }

        }
        log.debug("Mapping defined for fact acct " + aFactLine + "is :" + map);

        if (ltl == null) {
          ltl = createLedgerTransactionLine(ledgerName, ledgerAlias, ledgerDescription, txn,
              aFactLine);

          TaxRate tax = aFactLine.getTax();
          ElementValue elementVal = aFactLine.getAccount();
          if (tax != null)
            ltl.setTax(tax);
          else if (invLine != null)
            ltl.setTax(invLine.getTax());
          else {
            // Should i skip whole ledger if tax is not assigned?
            // logMonitor.log("No tax assigned for ledger leg with fact account   " +
            // factaccntLine);
          }

          ltl.setLTLType(getLTLTypeFromFactTxn(tax, elementVal));

        }

        else {
          ltl.setCredit(ltl.getCredit().add(aFactLine.getCredit()));
          ltl.setDebit(ltl.getDebit().add(aFactLine.getDebit()));
          ltl.setTxnamountcr(ltl.getTxnamountcr().add(aFactLine.getForeignCurrencyCredit()));
          ltl.setTxnamountdr(ltl.getTxnamountdr().add(aFactLine.getForeignCurrencyDebit()));
        }
        aFactLine.setDjeProcessed(true);
        OBDal.getInstance().save(aFactLine);
      }
      log.debug("Creation of ledger with its legs completed for the group ID"
          + accntList.get(0).getGroupID());
      log.debug("Applying Delivery charges for the same at " + new Date());
      processDeliveryCharges(txn, aFactLine);
      log.debug("delivery charges applied for the group ID " + accntList.get(0).getGroupID()
          + " at " + new Date());
      netOffAndRoundTxn(txn);
      log.debug("Finished the ledger creation for group id " + accntList.get(0).getGroupID());
      OBDal.getInstance().save(txn);
      OBDal.getInstance().flush();
      SessionHandler.getInstance().commitAndStart();
      OBDal.getInstance().getSession().clear();
      log.debug("Saved the ledger creation for group id " + accntList.get(0).getGroupID());

    } catch (Exception e) {
      SessionHandler.getInstance().rollback();
      log.error("Processing  txns", e);
      logMonitor.log("Error in creation of trxn :" + e);
      BusinessEntityMapper.rollBackNlogError(e, processId, errorLogObj);
    }

  }

  private void netOffAndRoundTxn(LedgerTransaction txn) {

    LedgerTransactionLeg lastSalesLeg = null;
    BigDecimal adj = BigDecimal.ZERO;
    for (LedgerTransactionLeg currentLeg : txn.getDJELedgerTxnLegList()) {
      log.debug("Before rounding " + currentLeg.getLTLType() + " " + currentLeg.getLedgerName()
          + " *Dr*" + currentLeg.getDebit() + "* Cr*" + currentLeg.getCredit());
      if (currentLeg.getLTLType().equals(LTL_TYPE_TAX))
        adj = adj.add(netOffAndRoundLeg(currentLeg, ADJ_TYPE.TAX));
      else if (currentLeg.getLTLType().equals(LTL_TYPE_SALES)) {
        lastSalesLeg = currentLeg;
        adj = adj.add(netOffAndRoundLeg(currentLeg, ADJ_TYPE.SALES));
      } else if (currentLeg.getLTLType().equals(LTL_TYPE_DLVY_CHRG))
        adj = adj.add(netOffAndRoundLeg(currentLeg, ADJ_TYPE.SALES));
      else
        adj = adj.add(netOffAndRoundLeg(currentLeg, ADJ_TYPE.PAYMENT));
      log.debug("After rounding " + currentLeg.getLTLType() + " " + currentLeg.getLedgerName()
          + " *Dr*" + currentLeg.getDebit() + "* Cr*" + currentLeg.getCredit() + " adj is now "
          + adj);

    }
    assert (lastSalesLeg != null);
    if (lastSalesLeg.getDebit().equals(BigDecimal.ZERO)) {
      lastSalesLeg.setCredit(lastSalesLeg.getCredit().subtract(adj));
      lastSalesLeg.setTxnamountcr(lastSalesLeg.getCredit());
    } else {
      lastSalesLeg.setDebit(lastSalesLeg.getDebit().add(adj));
      lastSalesLeg.setTxnamountdr(lastSalesLeg.getDebit());
    }
    log.debug("Last Leg Adjustment " + lastSalesLeg.getLTLType() + " "
        + lastSalesLeg.getLedgerName() + " *Dr*" + lastSalesLeg.getDebit() + "* Cr*"
        + lastSalesLeg.getCredit());

  }

  private BigDecimal netOffAndRoundLeg(LedgerTransactionLeg currentLeg,
      LedgerTransactionGenerator.ADJ_TYPE adjType) {
    // The adjustment shows the net change of the debit - credit amount / so if debit is zero then
    // credit would be added
    // to the adjusted amount to find the difference
    BigDecimal adj = BigDecimal.ZERO;
    BigDecimal newAmt = BigDecimal.ZERO;

    BigDecimal original = currentLeg.getDebit().subtract(currentLeg.getCredit());
    // Net the txn if both sides are there ( dr and cr ).
    if (original.compareTo(BigDecimal.ZERO) >= 0) {
      currentLeg.setDebit(currentLeg.getDebit().subtract(currentLeg.getCredit()));
      currentLeg.setTxnamountdr(currentLeg.getTxnamountdr().subtract(currentLeg.getTxnamountcr()));
      currentLeg.setCredit(BigDecimal.ZERO);
      currentLeg.setTxnamountcr(BigDecimal.ZERO);
    } else {
      currentLeg.setCredit(currentLeg.getCredit().subtract(currentLeg.getDebit()));
      currentLeg.setTxnamountcr(currentLeg.getTxnamountcr().subtract(currentLeg.getTxnamountdr()));
      currentLeg.setDebit(BigDecimal.ZERO);
      currentLeg.setTxnamountdr(BigDecimal.ZERO);
    }
    switch (adjType) {
    case TAX: {
      if (currentLeg.getDebit().compareTo(BigDecimal.ZERO) > 0) {
        newAmt = new BigDecimal(Math.floor(currentLeg.getDebit().doubleValue()));
        adj = original.subtract(newAmt).setScale(2, RoundingMode.HALF_UP);
        currentLeg.setDebit(newAmt);
        currentLeg.setTxnamountdr(newAmt);
      } else {
        newAmt = new BigDecimal(Math.ceil(currentLeg.getCredit().doubleValue()));
        adj = newAmt.add(original).setScale(2, RoundingMode.HALF_UP);
        currentLeg.setCredit(newAmt);
        currentLeg.setTxnamountcr(newAmt);
      }

      break;
    }
    case SALES: {
      if (currentLeg.getDebit().compareTo(BigDecimal.ZERO) > 0) {
        newAmt = new BigDecimal(Math.ceil(currentLeg.getDebit().doubleValue()));
        adj = original.subtract(newAmt).setScale(2, RoundingMode.HALF_UP);
        currentLeg.setDebit(newAmt);
        currentLeg.setTxnamountdr(newAmt);
      } else {
        newAmt = new BigDecimal(Math.floor(currentLeg.getCredit().doubleValue()));
        adj = newAmt.add(original).setScale(2, RoundingMode.HALF_UP);
        currentLeg.setCredit(newAmt);
        currentLeg.setTxnamountcr(newAmt);
      }
      break;
    }
    case PAYMENT: {
      if (currentLeg.getDebit().compareTo(BigDecimal.ZERO) > 0) {
        newAmt = new BigDecimal(Math.round(currentLeg.getDebit().doubleValue()));
        adj = original.subtract(newAmt).setScale(2, RoundingMode.HALF_UP);

        currentLeg.setDebit(newAmt);
        currentLeg.setTxnamountdr(newAmt);
      } else {
        newAmt = new BigDecimal(Math.round(currentLeg.getCredit().doubleValue()));
        adj = newAmt.add(original).setScale(2, RoundingMode.HALF_UP);
        currentLeg.setCredit(newAmt);
        currentLeg.setTxnamountcr(newAmt);
      }
      break;
    }
    }

    return adj;

  }

  private LedgerMapping computeDefaultLedgerMapping(Organization org) {
    LedgerMapping map = null;
    OBCriteria<LedgerMapping> mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
    mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_ACCOUNTELEMENT));
    mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));
    if (mapping.list().size() == 0) {
      mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_REGION, getRegionRestriction(org)));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_ACCOUNTELEMENT));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));
    }
    if (mapping.list().size() == 0) {
      mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
      Organization parent = new OrganizationStructureProvider().getParentOrg(org);
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parent));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_REGION));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_ACCOUNTELEMENT));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));
    }
    if (mapping.list().size() > 0) {
      map = mapping.list().get(0);
    }

    return map;
  }

  private String getLTLTypeFromFactTxn(TaxRate tax, ElementValue elementVal) {
    if (tax != null) {
      return LTL_TYPE_TAX;
    } else if (elementVal != null && elementVal.getAccountType().equals(ACCT_TYPE_REVENUE)) {
      return LTL_TYPE_SALES;
    } else {
      return LTL_TYPE_OTHERS;
    }
  }

  private void processDeliveryCharges(LedgerTransaction lt, AccountingFact line) throws Exception {

    // logMonitor.log("Started Processing deliveryCharges");
    String ledgerName = "";
    String ledgerAlias = "";
    String ledgerDescription = "";
    String recordId = line.getRecordID();
    Invoice inv = OBDal.getInstance().get(Invoice.class, recordId);
    try {
      if (inv == null) {
        return;
      }
      BigDecimal chgAmt = BigDecimal.ZERO;
      if (inv.getSalesOrder() == null)
        // return as there are no delivery charges
        return;
      chgAmt = inv.getSalesOrder().getDSChargeAmt();
      if (chgAmt == null || chgAmt == BigDecimal.ZERO)
        return;
      addDeliveryChargesToDebtor(lt, chgAmt);

      Map<BigDecimal, MapValues> taxMap = new HashMap<BigDecimal, MapValues>();
      Map<BigDecimal, MapValues> salesMap = new HashMap<BigDecimal, MapValues>();

      for (LedgerTransactionLeg leg : lt.getDJELedgerTxnLegList()) {
        MapValues mapvalue = new MapValues();
        mapvalue.element_val = leg.getAccountingFact().getAccount();
        mapvalue.tax_rate = leg.getTax();
        mapvalue.creditAmt = leg.getCredit().subtract(leg.getDebit());
        if (leg.getLTLType().equals(LTL_TYPE_TAX)) {
          taxMap.put(leg.getTax().getRate().setScale(2, RoundingMode.HALF_UP), mapvalue);
        }
        if (leg.getLTLType().equals(LTL_TYPE_SALES)) {
          salesMap.put(leg.getTax().getRate().setScale(2, RoundingMode.HALF_UP), mapvalue);
        }
      }

      for (BigDecimal rate : taxMap.keySet()) {
        if (taxMap.get(rate) == null || salesMap.get(rate) == null) {
          throw new OBException("No mapping defined for the rate " + rate + "taxmap: "
              + taxMap.get(rate) + " and salesmap: " + salesMap.get(rate));
        }
        String name = "";
        String alias = "";
        if (!inv.getGrandTotalAmount().toString().equals("0.00")) {
          BigDecimal chargeAmountPerTax = (salesMap.get(rate).creditAmt
              .add(taxMap.get(rate).creditAmt)).multiply(chgAmt).divide(
              (inv.getGrandTotalAmount()), 2, RoundingMode.HALF_UP);
          BigDecimal saleAmtPerTax = ((chargeAmountPerTax).multiply(new BigDecimal(100))).divide(
              (new BigDecimal(100).add(rate)), 2, RoundingMode.HALF_UP);
          BigDecimal taxAmt = (chargeAmountPerTax).subtract(saleAmtPerTax);

          LedgerMapping map = computeLedgerMapping(lt.getOrganization(), chrgAcct,
              taxMap.get(rate).tax_rate);
          if (map != null) {
            ledgerName = map.getLedgerName();
            ledgerAlias = map.getLedgerAlias();
            ledgerDescription = map.getDescription();
          }
          createDeliveryChargesLeg(ledgerName, ledgerAlias, lt, saleAmtPerTax, ledgerDescription,
              line, taxMap.get(rate).tax_rate);
          map = computeTaxLegMapping(lt.getOrganization(), taxMap.get(rate).element_val, null);
          if (map != null) {
            name = map.getLedgerName();
            alias = map.getLedgerAlias();
          }
          addTaxFromDeliveryCharges(name, alias, lt, taxAmt);
        } else {
          log.info("GrandTotalAmount Zero for the Invoice DocumentNo :" + inv.getDocumentNo());
          continue;
        }

      }

    } catch (Exception e) {
      throw new OBException("\n error while creating delivery charges for the Invoice " + inv
          + " Error: " + e);
    }

  }

  private ElementValue getChargeAccount() {
    OBCriteria<ProductCategory> pca = OBDal.getInstance().createCriteria(ProductCategory.class);
    pca.add(Restrictions.eq(ProductCategory.PROPERTY_NAME, "Charges"));
    if (pca.list().size() == 0) {
      throw new OBException("Product Category Charges not defined");
    }
    List<CategoryAccounts> chargeCategoryAccountList = pca.list().get(0)
        .getProductCategoryAccountsList();
    if (chargeCategoryAccountList.size() == 0) {
      throw new OBException("No Category account list for the Prod category " + pca.list().get(0));
    }
    ElementValue categoryAct = chargeCategoryAccountList.get(0).getProductRevenue().getAccount();
    return categoryAct;
  }

  private LedgerMapping computeTaxLegMapping(Organization organization, ElementValue account,
      TaxRate rate) {
    LedgerMapping map = null;
    map = getLedgerMapping(organization, account, null, LedgerMappingType.ORG_ELEMENT_NOTAX);

    if (map == null) {
      map = getLedgerMapping(organization, account, null, LedgerMappingType.STATE_ELEMENT_NOTAX);
    }
    return map;
  }

  private void addTaxFromDeliveryCharges(String ledgerName, String ledgerAlias,
      LedgerTransaction lt, BigDecimal transactionAmt) {
    List<LedgerTransactionLeg> legs = lt.getDJELedgerTxnLegList();

    for (LedgerTransactionLeg ltl : legs) {

      if (isEqualOrNull(ltl.getLedgerName(), ledgerName)
          && isEqualOrNull(ltl.getLedgerAlias(), ledgerAlias)) {
        ltl.setCredit(ltl.getCredit().add(transactionAmt));
        ltl.setDebit(ltl.getDebit().add(new BigDecimal(0.00)));
        ltl.setTxnamountcr(ltl.getTxnamountcr().add(transactionAmt));
        ltl.setTxnamountdr(ltl.getTxnamountdr().add(new BigDecimal(0.00)));
        break;
      }
    }

  }

  private boolean isEqualOrNull(String nullableString1, String nullableString2) {
    if ((nullableString1 == null) && (nullableString2 == null))
      return true;
    if ((nullableString1 == null) && (nullableString2 != null))
      return false;
    if ((nullableString1 != null) && (nullableString2 == null))
      return false;
    return nullableString1.equals(nullableString2);
  }

  private void createDeliveryChargesLeg(String ledgerName, String ledgerAlias,
      LedgerTransaction lt, BigDecimal transactionAmt, String ledgerDescription,
      AccountingFact line, TaxRate tax) {
    LedgerTransactionLeg ltl;
    ltl = OBProvider.getInstance().get(LedgerTransactionLeg.class);
    ltl.setClient(lt.getClient());
    ltl.setOrganization(lt.getOrganization());
    ltl.setActive(true);
    ltl.setCreatedBy(lt.getCreatedBy());
    ltl.setUpdatedBy(lt.getUpdatedBy());
    ltl.setCreationDate(new Date());
    ltl.setUpdated(new Date());
    ltl.setDJELedgerTxn(lt);
    ltl.setLedgerName(ledgerName);
    ltl.setLedgerAlias(ledgerAlias);
    ltl.setCredit(transactionAmt);
    ltl.setAccountingFact(line);
    ltl.setCurrency(line.getCurrency());
    ltl.setCostcenter(getCostCenter(line, lt));
    ltl.setDebit(new BigDecimal(0.00));
    // added tax rate to the delivery charges line
    ltl.setTax(tax);
    ltl.setTxnamountcr(transactionAmt);
    ltl.setTxnamountdr(new BigDecimal(0.00));
    ltl.setLTLType(LTL_TYPE_DLVY_CHRG);
    ltl.setDescription(ledgerDescription);
    lt.getDJELedgerTxnLegList().add(ltl);
    return;
  }

  private void addDeliveryChargesToDebtor(LedgerTransaction lt, BigDecimal transactionAmt) {
    List<LedgerTransactionLeg> legs = lt.getDJELedgerTxnLegList();

    for (LedgerTransactionLeg ltl : legs) {
      // the debtor leg has null value for Name and Alias
      if ((((ltl.getLedgerName() == null) && (ltl.getLedgerAlias() == null)))) {
        ltl.setDebit(ltl.getDebit().add(transactionAmt));
        ltl.setTxnamountdr(ltl.getTxnamountdr().add(transactionAmt));

        break;
      }

    }
    return;
  }

  private LedgerTransactionLeg createLedgerTransactionLine(String ledgerName, String ledgerAlias,
      String ledgerDescription, LedgerTransaction legHeader, AccountingFact line) throws Exception {
    try {
      LedgerTransactionLeg ltl = OBProvider.getInstance().get(LedgerTransactionLeg.class);
      ltl.setOrganization(legHeader.getOrganization());
      ltl.setClient(legHeader.getClient());
      ltl.setActive(true);
      ltl.setCreatedBy(legHeader.getCreatedBy());
      ltl.setUpdatedBy(legHeader.getUpdatedBy());
      ltl.setCreationDate(new Date());
      ltl.setUpdated(new Date());
      ltl.setLedgerName(ledgerName);
      ltl.setLedgerAlias(ledgerAlias);
      ltl.setDescription(ledgerDescription);
      ltl.setAccountingFact(line);
      ltl.setDJELedgerTxn(legHeader);
      ltl.setCredit(line.getCredit());
      ltl.setDebit(line.getDebit());
      ltl.setTxnamountcr(line.getForeignCurrencyCredit());
      ltl.setTxnamountdr(line.getForeignCurrencyDebit());
      ltl.setCurrency(line.getCurrency());
      ltl.setCostcenter(getCostCenter(line, legHeader));
      legHeader.getDJELedgerTxnLegList().add(ltl);
      return ltl;
    } catch (Exception e) {
      throw e;
    }
  }

  private Costcenter getCostCenter(AccountingFact line, LedgerTransaction legHeader) {

    Costcenter costcenter = null;
    costcenter = line.getCostcenter();
    if ((costcenter == null)) {
      Organization org = legHeader.getOrganization();
      List<OrganizationInformation> orgInfoList = org.getOrganizationInformationList();
      if (orgInfoList != null && orgInfoList.size() > 0) {
        costcenter = orgInfoList.get(0).getDjeCostcenter();
      }

    }

    return costcenter;

  }

  private LedgerMapping computeLedgerMapping(AccountingFact accountingTrxn, InvoiceLine invoiceLine) {
    // log.debug("Org :" + accountingTrxn.getOrganization().getId() + "AccntElement :"
    // + accountingTrxn.getAccount().getId() + " Tax : "
    // + (invoiceLine == null ? "" : invoiceLine.getTax().getId()));
    TaxRate rate = null;
    BigDecimal rateValue = BigDecimal.ZERO;
    // accountingTrxn.getTax().getRate().stripTrailingZeros();
    try {

      if (accountingTrxn.getTax() != null) {
        rate = accountingTrxn.getTax();
      } else if (invoiceLine != null) {
        rate = invoiceLine.getTax();
      }
      log.debug("Account is " + accountingTrxn.getAccount().getName() + " Tax rate is "
          + (rate != null ? rate.getName() : "Null") + " and txn id is " + accountingTrxn.getId());
      for (LedgerMappingType type : LedgerMappingType.values()) {
        LedgerMapping map = getLedgerMapping(accountingTrxn.getOrganization(),
            accountingTrxn.getAccount(), rate, type);
        if (map != null) {
          log.debug("Ledger Name is " + map.getLedgerName() + " Alias is " + map.getLedgerAlias());
          return map;
        }
      }
      return null;
    } catch (Exception e) {
      throw new OBException("Error while retrieving mapping " + e);
    }

  }

  private LedgerMapping computeLedgerMapping(Organization org, ElementValue element, TaxRate tax) {
    LedgerMapping map = null;
    map = getLedgerMapping(org, element, tax, LedgerMappingType.ORG_ELEMENT_TAX);
    if (map == null)
      map = getLedgerMapping(org, element, tax, LedgerMappingType.STATE_ELEMENT_TAX);

    return map;
  }

  private LedgerMapping getLedgerMapping(Organization org, ElementValue element, TaxRate tax,
      LedgerMappingType type) {

    OBCriteria<LedgerMapping> lm = OBDal.getInstance().createCriteria(LedgerMapping.class);
    Organization parent = new OrganizationStructureProvider().getParentOrg(org);

    switch (type) {
    case ORG_ELEMENT_TAX:
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));

      break;
    case STATE_ELEMENT_TAX:
      // cal method while assigning
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_REGION, getRegionRestriction(org)));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));

      break;
    case ORG_ELEMENT_NOTAX:

      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));

      break;

    case STATE_ELEMENT_NOTAX:
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_REGION, getRegionRestriction(org)));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));

      break;

    case PARENT_ELEMENT_TAX:
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parent));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));

      break;

    case PARENT_ELEMENT_NOTAX:
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parent));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));

      break;

    default:

      return null;
    }

    if (lm.list().size() == 0) {
      return null;
    } else {
      return lm.list().get(0);

    }

  }

  private Region getRegionRestriction(Organization org) {
    List<OrganizationInformation> orgInfoList = org.getOrganizationInformationList();
    if (orgInfoList != null && orgInfoList.size() > 0) {
      OrganizationInformation orginfo = orgInfoList.get(0);
      if (orginfo != null) {
        Location locationaddr = orginfo.getLocationAddress();
        if (locationaddr != null) {
          Region region = locationaddr.getRegion();
          if (region != null) {
            return region;
          }
        }
      }
    }
    log.info("No region details for the org");
    return null;
  }

  private LedgerTransaction createLedgerTrxnHeader(AccountingFact line, String posOrEcom)
      throws Exception {
    try {
      Invoice inv = null;
      LedgerTransaction legTrxn = OBProvider.getInstance().get(LedgerTransaction.class);
      legTrxn.setClient(line.getClient());
      legTrxn.setOrganization(line.getOrganization());
      legTrxn.setUpdatedBy(line.getUpdatedBy());
      legTrxn.setCreatedBy(line.getCreatedBy());
      legTrxn.setCreationDate(new Date());
      legTrxn.setUpdated(new Date());
      legTrxn.setActive(true);
      legTrxn.setExported(false);
      legTrxn.setDocumentType(line.getDocumentType());
      legTrxn.setProcessed(false);
      legTrxn.setTransactionDate(line.getTransactionDate());
      legTrxn.setGroupid(line.getGroupID());
      // legTrxn.setExport(new Date());

      // legTrxn.setFinancialsystemtype("EONE");

      // ** Setting Doc No, Parent Reference, Third number
      String invId = line.getRecordID();
      if (invId == null) {
        throw new OBException("No invoice id asssigned for the fact account" + line);
      }
      inv = OBDal.getInstance().get(Invoice.class, invId);
      if (inv == null) {
        throw new OBException("No invoice with id " + invId);
      }
      if (posOrEcom.equals(SOConstants.POS)) {
        legTrxn.setDocumentNo(inv.getDocumentNo());
        legTrxn.setTXNSource(SOConstants.STORE);
        SimpleDateFormat format = new SimpleDateFormat("MMM/dd");
        legTrxn.setPartnerreference(inv.getOrganization().getSearchKey() + "-"
            + format.format(inv.getInvoiceDate()));

      }

      else if (posOrEcom.equals(SOConstants.ECOM)) {
        legTrxn.setTXNSource(SOConstants.ECOM);
        Order salesOrder = inv.getSalesOrder();
        if (salesOrder == null) {
          throw new OBException("No sales order for the invoice " + inv);
        }
        setDocumentNo(legTrxn, salesOrder);
        BusinessPartner bPartner = inv.getBusinessPartner();
        if (bPartner == null) {
          log.error("No Bpartner for the invoice " + inv + " so Third no cannot be assigned");
          throw new OBException("No Bpartner for the invoice " + inv
              + " so Third no cannot be assigned");
        }
        setThirdNumber(inv, legTrxn, bPartner);
      }

      OBDal.getInstance().save(legTrxn);
      return legTrxn;
    } catch (Exception e) {
      throw e;
    }
  }

  private void setDocumentNo(LedgerTransaction legTrxn, Order salesOrder) {
    String docNo = salesOrder.getDocumentNo();
    // Assign the document no cropping Prefix and postfix
    if (docNo != null) {
      legTrxn.setDocumentNo(docNo);
      int pos = docNo.indexOf("*", 1);
      String soDocNum = docNo.substring(pos + 1, docNo.length());
      legTrxn.setPartnerreference(soDocNum.split("\\*")[0]);
    }
  }

  private void setThirdNumber(Invoice inv, LedgerTransaction legTrxn, BusinessPartner bPartner) {
    RCCompany rcCompany = bPartner.getRCCompany();
    if (rcCompany != null) {
      if (rcCompany.getDfexThirdNum() == null) {

        // Create Object for Document Sequence
        // Get NextAssigned Number
        // Populate it in Third NUm filed
        // Add 1 to NextAssigned Number and save it in Sequence again
        OBCriteria<Sequence> seqCriteri = OBDal.getInstance().createCriteria(Sequence.class);
        seqCriteri.add(Restrictions.eq(Sequence.PROPERTY_NAME, "ThirdNumber"));
        List<Sequence> seqList = seqCriteri.list();
        if (seqList != null && seqList.size() > 0) {
          Sequence seq = seqList.get(0);
          Long newSeqNo = (seq.getNextAssignedNumber());

          // Set the newSeqno
          legTrxn.setDfexThirdNum(newSeqNo);
          rcCompany.setDfexThirdNum(newSeqNo);

          // Update Sequence with the nextAssignedNumber
          seq.setNextAssignedNumber(newSeqNo + 1);
          OBDal.getInstance().save(seq);
          OBDal.getInstance().save(rcCompany);
        } else {
          // logMonitor.logln("No document sequence with name third Number");
          log.error("No document sequence with name third Number");
          throw new OBException("No document sequence with name third Number");
        }

      } else {
        legTrxn.setDfexThirdNum(rcCompany.getDfexThirdNum());
      }
    } else {
      // logMonitor.logln("No rcCompany for bpartner " + bPartner + " for the invoice " + inv
      // + " so Third no cannot be assigned");
      log.error("No rcCompany for bpartner " + bPartner + " for the invoice " + inv
          + " so Third no cannot be assigned");
      throw new OBException("No rcCompany for bpartner " + bPartner + " for the invoice " + inv
          + " so Third no cannot be assigned");
    }
  }
}
