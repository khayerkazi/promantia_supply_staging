package in.decathlon.journalentry;

import in.decathlon.ibud.commons.BusinessEntityMapper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.sysfore.sankalpcrm.RCCompany;

public class LedgerAccountingEntries extends DalBaseProcess {

  ProcessLogger logger;
  Logger log4j = Logger.getLogger(getClass());

  private enum LedgerMappingType {
    ORGELTX, POELTX, ORGEL, POEL
  };

  class AmountStruct {
    String description;
    String ledgerAlias;
    BigDecimal txnamtdr;
    BigDecimal txnamtcr;
    BigDecimal amtdr;
    BigDecimal amtcr;
    TaxRate taxRate;
  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    String processid = bundle.getProcessId();
    try{
    logger.log("Started Processing ");
    processFactAcctForB2B();
    processPOSTxns();
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    logger.log("Finished Txn Processing ");
    roundoff();
    logger.log("Finished Roundoff Processing ");
    }catch(Exception e){
    	 BusinessEntityMapper.rollBackNlogError(e, processid, null);
    }

  }

  private LedgerMapping getLedgerMapping(Organization org, ElementValue element, TaxRate tax,
      LedgerMappingType type) {

    OBCriteria<LedgerMapping> lm = OBDal.getInstance().createCriteria(LedgerMapping.class);
    Organization parent = new OrganizationStructureProvider().getParentOrg(org);
    switch (type) {
    case ORGELTX:
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));

      break;
    case POELTX:
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parent));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));

      break;
    case ORGEL:

      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
      lm.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      lm.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));

      break;

    case POEL:
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

  private LedgerMapping computeLedgerMapping(AccountingFact acctInfo, InvoiceLine lineInfo) {
    log4j.debug("Org :" + acctInfo.getOrganization().getId() + "AccntElement :"
        + acctInfo.getAccount().getId() + " Tax : "
        + (lineInfo == null ? "" : lineInfo.getTax().getId()));

    for (LedgerMappingType type : LedgerMappingType.values()) {
      TaxRate rate = null;
      if (lineInfo != null)
        rate = lineInfo.getTax();
      LedgerMapping map = null;
      map = getLedgerMapping(acctInfo.getOrganization(), acctInfo.getAccount(), rate, type);
      if (map != null) {

        return map;
      }
    }
    log4j.debug("return ledger mapping null");
    return null;
  }

  private void processPOSTxns() throws ParseException {

    String queryString = "";
    String ledgerName = "";
    String ledgerAlias = "";
    String ledgerDescription = "";
    Boolean isSales = false;
    Boolean isPos = false;
    Boolean isReturn = false;
    log4j.debug("Processing Pos Txns");
    logger.log("Processing Pos Txns");

    AmountStruct currentAmtStruct = null;
    boolean generateHeader;

    queryString = " f where djeProcessed <> 'Y'  and table.id = '318' order by accountingDate,organization.id,groupID ";

    OBQuery<AccountingFact> factLineQuery = OBDal.getInstance().createQuery(AccountingFact.class,
        queryString);
    String recordId = null;
    Currency currency = null;
    generateHeader = true;
    LedgerTransaction lt = null;
    Costcenter cs = null;
    List<AccountingFact> factLineList = factLineQuery.list();
    HashMap<String, AmountStruct> summaryTotalsMap = new HashMap<String, AmountStruct>();

    String orgPrev = null;
    Date datePrev = null;

    for (AccountingFact line : factLineList) {

      currency = line.getCurrency();
      recordId = line.getRecordID();
      Invoice inv = OBDal.getInstance().get(Invoice.class, recordId);
      isSales = inv.isSalesTransaction();
      isReturn = inv.getDocumentType().isReturn();
      isPos = inv.getOrganization().isSWIsstore();
      if (isSales) {
        if (!isPos)
          continue;
        if (isReturn)
          continue;
      } else
        continue;

      if (orgPrev == null || datePrev == null || !orgPrev.equals(line.getOrganization().getId())
          || !(datePrev.compareTo(line.getAccountingDate()) == 0)) {
        generateHeader = true;
        if ((orgPrev != null) && (datePrev != null)) {
          generateLedgerTransactionLines(currency, lt, summaryTotalsMap);
          OBDal.getInstance().save(lt);
          summaryTotalsMap = new HashMap<String, AmountStruct>();
        }
        orgPrev = line.getOrganization().getId();
        datePrev = line.getAccountingDate();

      }

      if (generateHeader) {
        lt = generateHeaderPOS(line, inv);
        lt.setProcessed(false);
        generateHeader = false;
      }

      InvoiceLine invoiceLine = null;
      if (line.getLineID() != null)
        invoiceLine = OBDal.getInstance().get(InvoiceLine.class, line.getLineID());

      LedgerMapping map = computeLedgerMapping(line, invoiceLine);
      log4j.debug(" map object for computeLedgerMapping " + map);
      if (map != null) {
        ledgerName = map.getLedgerName();
        ledgerDescription = map.getDescription();
        ledgerAlias = map.getLedgerAlias();
      }

      if (map == null) {

        LedgerMapping map1 = computeDefaultLedgerMapping(line.getOrganization());
        log4j.debug(" map object for computeDefaultLedgerMapping " + map1);

        ledgerName = map1.getLedgerName();
        ledgerDescription = map1.getDescription();
        ledgerAlias = map1.getLedgerAlias();

      }

      String key;
      cs = getCostCenter(line, lt);

      key = ledgerName + '|' + "";
      key = key + " | ";
      if (cs != null) {
        key += cs.getId();
      } else {
        key += " ";
      }

      key = key + " | ";
      TaxRate currentRate = null;
      if (line.getTax() != null) {

        key += line.getTax().getRate().stripTrailingZeros();
        currentRate = line.getTax();
      } else if (invoiceLine != null) {
        key += invoiceLine.getTax().getRate().stripTrailingZeros();
        currentRate = invoiceLine.getTax();

      }

      key = key + " | ";
      if (line.getTax() != null)
        key += "T";
      else if (line.getAccount().getAccountType().equals("R"))
        key += "S";
      else
        key += "O";

      if ((currentAmtStruct = summaryTotalsMap.get(key)) == null) {
        log4j.debug("The inserted key is:" + key);
        currentAmtStruct = new AmountStruct();
        currentAmtStruct.amtcr = line.getCredit();
        currentAmtStruct.amtdr = line.getDebit();
        currentAmtStruct.txnamtcr = line.getForeignCurrencyCredit();
        currentAmtStruct.txnamtdr = line.getForeignCurrencyDebit();
        currentAmtStruct.taxRate = currentRate;
        summaryTotalsMap.put(key, currentAmtStruct);
      } else {

        currentAmtStruct.amtcr = currentAmtStruct.amtcr.add(line.getCredit());
        currentAmtStruct.amtdr = currentAmtStruct.amtdr.add(line.getDebit());
        currentAmtStruct.txnamtcr = currentAmtStruct.txnamtcr.add(line.getForeignCurrencyCredit());
        currentAmtStruct.txnamtdr = currentAmtStruct.txnamtdr.add(line.getForeignCurrencyDebit());

      }
      currentAmtStruct.description = ledgerDescription;
      currentAmtStruct.ledgerAlias = ledgerAlias;
      line.setDjeProcessed(true);
      OBDal.getInstance().save(line);
    }
    if ((orgPrev != null) && (datePrev != null)) {
      generateLedgerTransactionLines(currency, lt, summaryTotalsMap);
      OBDal.getInstance().save(lt);

    }

  }

  private void generateLedgerTransactionLines(Currency currency, LedgerTransaction lt,
      HashMap<String, AmountStruct> summaryTotalsMap) {
    AmountStruct currentAmtStruct;
    for (String key : summaryTotalsMap.keySet()) {
      currentAmtStruct = summaryTotalsMap.get(key);
      LedgerTransactionLeg ltl = OBProvider.getInstance().get(LedgerTransactionLeg.class);
      ltl.setClient(lt.getClient());
      ltl.setOrganization(lt.getOrganization());
      ltl.setActive(true);
      ltl.setCreatedBy(lt.getCreatedBy());
      ltl.setUpdatedBy(lt.getUpdatedBy());
      ltl.setCreationDate(new Date());
      ltl.setUpdated(new Date());
      ltl.setDJELedgerTxn(lt);
      ltl.setCurrency(currency);
      ltl.setLedgerName(key.split("\\|")[0]);
      ltl.setLedgerAlias(currentAmtStruct.ledgerAlias);
      ltl.setCredit(currentAmtStruct.amtcr);
      ltl.setDebit(currentAmtStruct.amtdr);
      ltl.setTxnamountcr(currentAmtStruct.txnamtcr);
      ltl.setTxnamountdr(currentAmtStruct.txnamtdr);
      if (key.split("\\|")[2].trim().equals(""))
        ltl.setCostcenter(null);
      else {
        String costCentreId = key.split("\\|")[2].trim();
        ltl.setCostcenter(OBDal.getInstance().get(Costcenter.class, costCentreId));
      }
      ltl.setTax(currentAmtStruct.taxRate);
      ltl.setLTLType(key.split("\\|")[4].trim());
      ltl.setDescription(currentAmtStruct.description);
      lt.getDJELedgerTxnLegList().add(ltl);

    }
  }

  private LedgerMapping computeDefaultLedgerMapping(Organization org) {

    OBCriteria<LedgerMapping> mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
    mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_ACCOUNTELEMENT));
    mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));
    if (mapping.list().size() == 0) {
      Organization parentorg = new OrganizationStructureProvider().getParentOrg(org);
      mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parentorg));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_ACCOUNTELEMENT));
      mapping.add(Restrictions.isNull(LedgerMapping.PROPERTY_TAX));
    }
    if (mapping.list().size() > 0) {
      return mapping.list().get(0);
    }

    return null;
  }

  public Costcenter getCostCenter(AccountingFact line, LedgerTransaction lt) {

    Costcenter costcenter = null;
    costcenter = line.getCostcenter();
    if ((costcenter == null)) {
      if (line.getAccount().getAccountType() != null) {

        if (lt.getOrganization().getOrganizationInformationList() != null
            && lt.getOrganization().getOrganizationInformationList().size() > 0)
          costcenter = lt.getOrganization().getOrganizationInformationList().get(0)
              .getDjeCostcenter();

      }
    }

    return costcenter;

  }

  private LedgerTransaction generateHeaderPOS(AccountingFact line, Invoice inv) {
    LedgerTransaction lt;
    lt = OBProvider.getInstance().get(LedgerTransaction.class);
    lt.setClient(line.getClient());
    lt.setOrganization(line.getOrganization());
    lt.setUpdatedBy(line.getUpdatedBy());
    lt.setCreatedBy(line.getCreatedBy());
    lt.setCreationDate(new Date());
    lt.setUpdated(new Date());

    lt.setDocumentType(inv.getDocumentType());
    lt.setExport(new Date());
    lt.setActive(true);
    lt.setExported(false);
    lt.setTransactionDate(line.getTransactionDate());
    lt.setFinancialsystemtype("EONE");
    lt.setGroupid(line.getGroupID());
    lt.setDocumentNo(inv.getDocumentNo());
    lt.setTXNSource("Store");
    SimpleDateFormat format = new SimpleDateFormat("MMM/dd");
    lt.setPartnerreference(inv.getOrganization().getSearchKey() + "-"
        + format.format(inv.getInvoiceDate()));
    return lt;
  }

  private void roundoff() {
    logger.log("Started Processing for rounding");

    String queryString = "";
    queryString = " f where f.isprocessed <> 'Y'";

    OBQuery<LedgerTransaction> ledgerQuery = OBDal.getInstance().createQuery(
        LedgerTransaction.class, queryString);

    List<LedgerTransaction> ledgerTxns = ledgerQuery.list();
    for (LedgerTransaction txns : ledgerTxns) {

      String queryStringltl = " f where dJELedgerTxn='" + txns.getId()
          + "' order by tax , lTLType desc";
      OBQuery<LedgerTransactionLeg> ltlQuery = OBDal.getInstance().createQuery(
          LedgerTransactionLeg.class, queryStringltl);
      List<LedgerTransactionLeg> ltlList = ltlQuery.list();

      Map<BigDecimal, LedgerTransactionLeg> taxMap = new HashMap<BigDecimal, LedgerTransactionLeg>();
      Map<BigDecimal, LedgerTransactionLeg> salesMap = new HashMap<BigDecimal, LedgerTransactionLeg>();
      Map<String, LedgerTransactionLeg> cutMap = new HashMap<String, LedgerTransactionLeg>();
      Map<BigDecimal, LedgerTransactionLeg> delvChrgMap = new HashMap<BigDecimal, LedgerTransactionLeg>();

      for (LedgerTransactionLeg ltl : ltlList) {

        if (ltl.getLTLType().equals("T")) {

          taxMap.put(ltl.getTax().getRate().stripTrailingZeros(), ltl);

        } else if (ltl.getLTLType().equals("S")) {

          salesMap.put(ltl.getTax().getRate().stripTrailingZeros(), ltl);

        } else if (ltl.getLTLType().equals("D")) {
          delvChrgMap.put(ltl.getTax().getRate().stripTrailingZeros(), ltl);
        }
      }

      if (txns.getTXNSource().equals("Store")) {

        for (BigDecimal key : taxMap.keySet()) {
          log4j.debug(key);
          LedgerTransactionLeg taxLeg = taxMap.get(key);
          LedgerTransactionLeg salesLeg = salesMap.get(key);
          System.out.println(".....salesLeg...." + salesLeg.getCredit());

          BigDecimal taxAmt = new BigDecimal(0);
          BigDecimal taxAdjAmt = new BigDecimal(0);
          if (taxLeg.getCredit().compareTo(taxLeg.getDebit()) == 1) {
            System.out.println("....Tax if start...");

            taxLeg.setCredit(taxLeg.getCredit().subtract(taxLeg.getDebit()));
            taxLeg.setDebit(new BigDecimal(0));

            taxAmt = new BigDecimal(Math.ceil(taxLeg.getCredit().doubleValue()));
            taxAdjAmt = taxAmt.subtract(taxLeg.getCredit());
            taxLeg.setCredit(taxAmt);
            taxLeg.setTxnamountcr(taxAmt);
            taxLeg.setTxnamountdr(new BigDecimal(0));
            System.out.println(".....Tax if end...");

          } else {
            System.out.println("....Tax else start...");

            taxLeg.setDebit(taxLeg.getDebit().subtract(
                taxLeg.getCredit()));
            taxLeg.setCredit(new BigDecimal(0));
            taxAmt = new BigDecimal(Math
                .floor(taxLeg.getDebit().doubleValue()));
            taxAdjAmt = taxLeg.getDebit().subtract(taxAmt).multiply(
                new BigDecimal(-1));
            taxLeg.setDebit(taxAmt);
            taxLeg.setTxnamountdr(taxAmt);
            taxLeg.setTxnamountcr(new BigDecimal(0));
            System.out.println(".....Tax else end...");

          }
          if (salesLeg.getCredit().compareTo(salesLeg.getDebit()) == 1) {
            System.out.println(".....sales if start...");

            salesLeg.setCredit(salesLeg.getCredit().subtract(
                salesLeg.getDebit()).subtract(taxAdjAmt));
            // Rounding the sales
            salesLeg.setCredit(roundBigDecimal(salesLeg.getCredit()));
            salesLeg.setDebit(new BigDecimal(0));

            salesLeg.setTxnamountcr(salesLeg.getCredit());

            salesLeg.setTxnamountdr(new BigDecimal(0));
            System.out.println(".....sales if end...");

          } else {
            System.out.println(".....sales else start...");

            salesLeg.setDebit(salesLeg.getDebit()
                .subtract(salesLeg.getCredit()).subtract(taxAdjAmt));
            // rounding the sales
            salesLeg.setDebit(roundBigDecimal(salesLeg
                .getDebit()));

            salesLeg.setCredit(new BigDecimal(0));

            salesLeg.setTxnamountdr(salesLeg.getDebit());

            salesLeg.setTxnamountcr(new BigDecimal(0));
            System.out.println(".....sales else end...");

          }

          OBDal.getInstance().save(taxLeg);
          OBDal.getInstance().save(salesLeg);

        }
      } else if (txns.getTXNSource().equals("ECOM")) {

        BigDecimal totalAmt = new BigDecimal(0);
        BigDecimal diffAmtDelvChrg = new BigDecimal(0);

        for (LedgerTransactionLeg ltl : ltlList) {

          if (ltl.getLTLType().equals("O")) {

            cutMap.put("Others", ltl);

          }

        }

        if (!delvChrgMap.isEmpty()) {

          for (BigDecimal key : delvChrgMap.keySet()) {

            delvChrgMap.get(key).setCredit(
                new BigDecimal(Math.floor(delvChrgMap.get(key).getCredit().doubleValue())));
            delvChrgMap.get(key).setDebit((new BigDecimal(0)));
            delvChrgMap.get(key).setTxnamountcr(
                new BigDecimal(Math.floor(delvChrgMap.get(key).getCredit().doubleValue())));
            delvChrgMap.get(key).setTxnamountdr((new BigDecimal(0)));
          }

        }

        for (String key : cutMap.keySet()) {
          LedgerTransactionLeg cutLeg = cutMap.get(key);
          BigDecimal customer = new BigDecimal(0);
          BigDecimal actcustvalue = new BigDecimal(0);
          BigDecimal diff = new BigDecimal(0);

          actcustvalue = cutLeg.getDebit();
          customer = roundBigDecimal(actcustvalue);
          cutLeg.setDebit(customer);
          cutLeg.setTxnamountdr(customer);
          cutLeg.setTxnamountcr(new BigDecimal(0));
          diff = customer.subtract(actcustvalue);
          System.out.println("........diff......" + diff);
          // This section is for distributing the fraction came from rounding in between the tax and
          // sales legs

          BigDecimal individualdiff = diff.divide(new BigDecimal(taxMap.size() + salesMap.size()));
          System.out.println("........individualdiff......" + individualdiff);

          for (BigDecimal keyTaxSales : taxMap.keySet()) {

            taxMap.get(keyTaxSales).setCredit(
                taxMap.get(keyTaxSales).getCredit().add(individualdiff));

            salesMap.get(keyTaxSales).setCredit(
                salesMap.get(keyTaxSales).getCredit().add(individualdiff));

            System.out.println(taxMap.get(keyTaxSales));
            System.out.println(salesMap.get(keyTaxSales));

            salesMap.get(keyTaxSales).setCredit(
                new BigDecimal(Math.floor(salesMap.get(keyTaxSales).getCredit().doubleValue())));

            salesMap.get(keyTaxSales).setTxnamountcr(salesMap.get(keyTaxSales).getCredit());

            salesMap.get(keyTaxSales).setTxnamountdr(new BigDecimal(0));
            salesMap.get(keyTaxSales).setDebit(new BigDecimal(0));

            System.out.println("after subtracting setting sales");

            taxMap.get(keyTaxSales).setCredit(
                new BigDecimal(Math.ceil(taxMap.get(keyTaxSales).getCredit().doubleValue())));

            taxMap.get(keyTaxSales).setTxnamountcr(taxMap.get(keyTaxSales).getCredit());
            taxMap.get(keyTaxSales).setTxnamountdr(new BigDecimal(0));
            taxMap.get(keyTaxSales).setDebit(new BigDecimal(0));

            System.out.println("Done all calculations");

          }

        }

        for (LedgerTransactionLeg ltl : ltlList) {

          if (!ltl.getLTLType().equals("O")) {
            totalAmt = totalAmt.add(ltl.getCredit());

          }

        }
        if (cutMap.size() != 0) {
          diffAmtDelvChrg = cutMap.get("Others").getDebit().subtract(totalAmt);
        }
        if (diffAmtDelvChrg.compareTo(new BigDecimal(0)) != 0) {

          if (!delvChrgMap.isEmpty()) {
            for (BigDecimal bkey : delvChrgMap.keySet()) {
              // delvChrgMap.get(bkey).getCredit().add(diffAmtDelvChrg);
              delvChrgMap.get(bkey).setCredit(
                  delvChrgMap.get(bkey).getCredit().add(diffAmtDelvChrg));
              delvChrgMap.get(bkey).setTxnamountcr(delvChrgMap.get(bkey).getCredit());
              delvChrgMap.get(bkey).setDebit(new BigDecimal(0));
              delvChrgMap.get(bkey).setTxnamountdr(new BigDecimal(0));

              break;

            }

          } else {

            for (BigDecimal bkey : salesMap.keySet()) {
              salesMap.get(bkey).getCredit().add(diffAmtDelvChrg);
              salesMap.get(bkey).setCredit(salesMap.get(bkey).getCredit().add(diffAmtDelvChrg));
              salesMap.get(bkey).setTxnamountcr(salesMap.get(bkey).getCredit());
              salesMap.get(bkey).setDebit(new BigDecimal(0));
              salesMap.get(bkey).setTxnamountdr(new BigDecimal(0));

              break;
            }

          }

        }

      }

      txns.setProcessed(true);
      OBDal.getInstance().save(txns);

    }
  }

  private OBError processFactAcctForB2B() {
    OBError error = new OBError();

    Boolean isSales = false;
    Boolean isPos = false;
    Boolean isReturn = false;
    String queryString = "";
    String prevGroup = "";
    queryString = " f where djeProcessed <> 'Y'  and table.id = '318' order by groupID";
    OBQuery<AccountingFact> factLineQuery = OBDal.getInstance().createQuery(AccountingFact.class,
        queryString);
    String recordId;
    List<AccountingFact> factLineList = factLineQuery.list();
    for (AccountingFact line : factLineList) {

      if (!prevGroup.equals(line.getGroupID())) {
        prevGroup = line.getGroupID();
        recordId = line.getRecordID();
        Invoice inv = OBDal.getInstance().get(Invoice.class, recordId);
        isSales = inv.isSalesTransaction();
        isReturn = inv.getDocumentType().isReturn();
        isPos = (inv.getOrganization().isSWIsstore() == null ? false : inv.getOrganization()
            .isSWIsstore());
        if (isSales) {
          if (!isPos) {

            processB2B(prevGroup);
          } else {
            continue;
          }

        }
      }
    }
    return error;
  }

  private void processB2B(String currentGroup) {
    logger.log("Started Processing for B2B/ECOM ");
    String ledgerName;
    String ledgerAlias;
    String ledgerDescription;
    LedgerTransaction lt = null;
    Invoice inv = null;
    AccountingFact lines = null;
    List<AccountingFact> factLineList = getGroupTransactions(currentGroup);
    if (factLineList.size() > 0) {

      lines = factLineList.get(0);
      inv = OBDal.getInstance().get(Invoice.class, lines.getRecordID());

      lt = createLedgerTransaction(currentGroup, lines, inv);
    }
    for (AccountingFact line : factLineList) {
      ledgerName = "";
      ledgerAlias = "";
      ledgerDescription = "";
      LedgerTransactionLeg ltl = null;
      InvoiceLine invoiceLine = null;
      if (line.getLineID() != null)
        invoiceLine = OBDal.getInstance().get(InvoiceLine.class, line.getLineID());
      LedgerMapping map = computeLedgerMapping(line, invoiceLine);
      if (map != null) {
        ledgerDescription = map.getDescription();
        ledgerAlias = map.getLedgerAlias();
        ledgerName = map.getLedgerName();
      }
      for (LedgerTransactionLeg cLeg : lt.getDJELedgerTxnLegList()) {

        if (cLeg.getLedgerName() != null & cLeg.getLedgerName().equals(ledgerName)) {
          ltl = cLeg;
          break;
        }

      }
      if (ltl == null) {
        ltl = createLedgerTransactionLine(ledgerName, ledgerAlias, ledgerDescription, lt, line);
        setLtlTax(line, ltl, invoiceLine);
      }

      else {
        ltl.setCredit(ltl.getCredit().add(line.getCredit()));
        ltl.setDebit(ltl.getDebit().add(line.getDebit()));
        ltl.setTxnamountcr(ltl.getTxnamountcr().add(line.getForeignCurrencyCredit()));
        ltl.setTxnamountdr(ltl.getTxnamountdr().add(line.getForeignCurrencyDebit()));
      }

      OBDal.getInstance().save(lt);
      line.setDjeProcessed(true);
      OBDal.getInstance().save(line);
    }
    deliveryChargesB2B(lt, inv, lines);
  }

  private void setLtlTax(AccountingFact line, LedgerTransactionLeg ltl, InvoiceLine invoiceLine) {
    if (line.getTax() != null)
      ltl.setTax(line.getTax());
    else if (invoiceLine != null)
      ltl.setTax(invoiceLine.getTax());

    if (line.getTax() != null)
      ltl.setLTLType("T");
    else if (line.getAccount().getAccountType().equals("R"))
      ltl.setLTLType("S");
    else
      ltl.setLTLType("O");
  }

  private LedgerTransactionLeg createLedgerTransactionLine(String ledgerName, String ledgerAlias,
      String ledgerDescription, LedgerTransaction lt, AccountingFact line) {
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
    ltl.setAccountingFact(line);
    ltl.setLedgerName(ledgerName);
    ltl.setLedgerAlias(ledgerAlias);
    ltl.setDescription(ledgerDescription);
    ltl.setCredit(line.getCredit());
    ltl.setDebit(line.getDebit());
    ltl.setTxnamountcr(line.getForeignCurrencyCredit());
    ltl.setTxnamountdr(line.getForeignCurrencyDebit());
    ltl.setCurrency(line.getCurrency());
    ltl.setCostcenter(getCostCenter(line, lt));
    lt.getDJELedgerTxnLegList().add(ltl);
    return ltl;
  }

  private List<AccountingFact> getGroupTransactions(String currentGroup) {
    String queryString = " f where f.groupID='" + currentGroup + "'";
    OBQuery<AccountingFact> factLineQuery = OBDal.getInstance().createQuery(AccountingFact.class,
        queryString);
    List<AccountingFact> factLineList = factLineQuery.list();

    return factLineList;
  }

  private LedgerTransaction createLedgerTransaction(String prevGroup, AccountingFact line,
      Invoice inv) {
    LedgerTransaction lt;
    lt = OBProvider.getInstance().get(LedgerTransaction.class);
    lt.setClient(line.getClient());
    lt.setOrganization(line.getOrganization());
    lt.setUpdatedBy(line.getUpdatedBy());
    lt.setCreatedBy(line.getCreatedBy());
    lt.setCreationDate(new Date());
    lt.setUpdated(new Date());
    lt.setDocumentType(inv.getDocumentType());
    lt.setExport(new Date());
    lt.setActive(true);
    lt.setExported(false);
    lt.setTransactionDate(line.getTransactionDate());
    lt.setFinancialsystemtype("EONE");
    lt.setGroupid(prevGroup);
    if (inv.getBusinessPartner().getRCCompany().getDfexThirdNum() == null) {

      // Create Object for Document Sequence
      // Get NextAssigned Number
      // Populate it in Third NUm filed
      // Add 1 to NextAssigned Number and save it in Sequence again
      Sequence seq = null;
      OBCriteria<Sequence> seqCriteri = OBDal.getInstance().createCriteria(Sequence.class);
      seqCriteri.add(Restrictions.eq(Sequence.PROPERTY_NAME, "ThirdNumber"));
      seq = seqCriteri.list().get(0);
      Long newSeqNo = (seq.getNextAssignedNumber());

      // Set the newSeqno
      lt.setDfexThirdNum(newSeqNo);
      RCCompany company = inv.getBusinessPartner().getRCCompany();
      company.setDfexThirdNum(newSeqNo);

      // Update Sequence with the nextAssignedNumber
      seq.setNextAssignedNumber(newSeqNo + 1);
      OBDal.getInstance().save(seq);
      OBDal.getInstance().save(company);

    } else
      lt.setDfexThirdNum(inv.getBusinessPartner().getRCCompany().getDfexThirdNum());
    lt.setDocumentNo(inv.getSalesOrder().getDocumentNo());
    String s = inv.getSalesOrder().getDocumentNo();
    int pos = s.indexOf("*", 1);
    String soDocNum = s.substring(pos + 1, s.length());
    lt.setPartnerreference(soDocNum.split("\\*")[0]);
    // lt.setPartnerreference(s.substring(pos + 1, s.length()));
    lt.setProcessed(false);
    lt.setTXNSource("ECOM");
    OBDal.getInstance().save(lt);
    return lt;
  }

  private void deliveryChargesB2B(LedgerTransaction lt, Invoice inv, AccountingFact line) {
    logger.log("Started Processing deliveryCharges");
    String ledgerName = "";
    String ledgerAlias = "";
    String ledgerDescription = "";
    BigDecimal chgAmt = inv.getSalesOrder().getDSChargeAmt();
    LedgerTransactionLeg ltl;
    if (!chgAmt.equals(new BigDecimal(0.00))) {

      OBCriteria<ProductCategory> pca = OBDal.getInstance().createCriteria(ProductCategory.class);
      pca.add(Restrictions.eq(ProductCategory.PROPERTY_NAME, "Charges"));
      ElementValue categoryActs = pca.list().get(0).getProductCategoryAccountsList().get(0)
          .getProductRevenue().getAccount();

      ltl = customerDeliveryLedgerTransactionLine(ledgerName, ledgerAlias, lt, chgAmt);
      List<InvoiceTax> invTaxList = inv.getInvoiceTaxList();

      for (InvoiceTax tax : invTaxList) {
        String name = "";
        String alias = "";
        String description = "";
        System.out.println(".......grandtotalamount..............." + inv.getGrandTotalAmount());
        if (!inv.getGrandTotalAmount().toString().equals("0.00")) {
          BigDecimal chargeAmountPerTax = (((tax.getTaxableAmount()).add(tax.getTaxAmount()))
              .multiply(chgAmt).divide((inv.getGrandTotalAmount()), 2, RoundingMode.HALF_UP));
          BigDecimal saleAmtdch = ((chargeAmountPerTax).multiply(new BigDecimal(100))).divide(
              (new BigDecimal(100).add(tax.getTax().getRate())), 2, RoundingMode.HALF_UP);
          BigDecimal taxAmtdch = (chargeAmountPerTax).subtract(saleAmtdch);

          LedgerMapping map1 = deliveryMapping(lt.getOrganization(), categoryActs, tax.getTax());
          if (map1 != null) {
            ledgerName = map1.getLedgerName();
            ledgerAlias = map1.getLedgerAlias();
            ledgerDescription = map1.getDescription();
          }
          ltl = salesDeliveryLedgerTransactionLine(ledgerName, ledgerAlias, lt, saleAmtdch,
              ledgerDescription, line, tax.getTax());

          LedgerMapping map = deliveryMappingTax(lt.getOrganization(), tax.getTax()
              .getFinancialMgmtAccountingFactList().get(0).getAccount());
          if (map != null) {
            name = map.getLedgerName();
            alias = map.getLedgerAlias();
            description = map.getDescription();
          }
          ltl = taxDeliveryLedgerTransactionLine(name, alias, lt, taxAmtdch);
        } else {
          logger
              .log("This record GrandTotalAmount Zero Invoice DocumentNo :" + inv.getDocumentNo());
          log4j.debug("This record GrandTotalAmount Zero Invoice DocumentNo :"
              + inv.getDocumentNo());
          continue;
        }
      }

    }

  }

  private LedgerMapping deliveryMappingTax(Organization org, ElementValue element) {

    OBCriteria<LedgerMapping> mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
    if (mapping.list().size() == 0) {
      Organization parentorg = new OrganizationStructureProvider().getParentOrg(org);
      mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parentorg));
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
    }
    if (mapping.list().size() > 0) {
      return mapping.list().get(0);
    }

    return null;
  }

  private LedgerMapping deliveryMapping(Organization org, ElementValue element, TaxRate tax) {

    OBCriteria<LedgerMapping> mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, org));
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
    mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));
    if (mapping.list().size() == 0) {
      Organization parentorg = new OrganizationStructureProvider().getParentOrg(org);
      mapping = OBDal.getInstance().createCriteria(LedgerMapping.class);
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ORGANIZATION, parentorg));
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_ACCOUNTELEMENT, element));
      mapping.add(Restrictions.eq(LedgerMapping.PROPERTY_TAX, tax));
    }
    if (mapping.list().size() > 0) {
      return mapping.list().get(0);
    }

    return null;
  }

  private LedgerTransactionLeg salesDeliveryLedgerTransactionLine(String ledgerName,
      String ledgerAlias, LedgerTransaction lt, BigDecimal transactionAmt,
      String ledgerDescription, AccountingFact line, TaxRate tax) {
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
    ltl.setLTLType("D");
    ltl.setDescription(ledgerDescription);
    lt.getDJELedgerTxnLegList().add(ltl);
    return ltl;
  }

  private LedgerTransactionLeg customerDeliveryLedgerTransactionLine(String ledgerName,
      String ledgerAlias, LedgerTransaction lt, BigDecimal transactionAmt) {
    List<LedgerTransactionLeg> legs = lt.getDJELedgerTxnLegList();

    for (LedgerTransactionLeg ltl : legs) {
      if ((((ltl.getLedgerName() == null) && ledgerName == null) || ltl.getLedgerName().equals(
          ledgerName))
          && (((ltl.getLedgerAlias() == null) && ledgerAlias == null) || ltl.getLedgerAlias()
              .equals(ledgerAlias))) {

        ltl.setDebit(ltl.getDebit().add(transactionAmt));
        ltl.setTxnamountdr(ltl.getTxnamountdr().add(transactionAmt));

        break;
      }

    }
    return null;
  }

  private LedgerTransactionLeg taxDeliveryLedgerTransactionLine(String ledgerName,
      String ledgerAlias, LedgerTransaction lt, BigDecimal transactionAmt) {
    List<LedgerTransactionLeg> legs = lt.getDJELedgerTxnLegList();

    for (LedgerTransactionLeg ltl : legs) {
      if ((((ltl.getLedgerName() == null) && ledgerName == null) || ltl.getLedgerName().equals(
          ledgerName))
          && (((ltl.getLedgerAlias() == null) && ledgerAlias == null) || ltl.getLedgerAlias()
              .equals(ledgerAlias))) {

        ltl.setCredit(ltl.getCredit().add(transactionAmt));
        ltl.setDebit(ltl.getDebit().add(
            new BigDecimal(0.00)));
        ltl.setTxnamountcr(ltl.getTxnamountcr().add(transactionAmt));
        ltl.setTxnamountdr(ltl.getTxnamountdr().add(new BigDecimal(0.00)));
        break;
      }
    }
    return null;

  }

  public static BigDecimal roundBigDecimal(final BigDecimal input) {
    return input.round(new MathContext(input.toBigInteger().toString().length(),
        RoundingMode.HALF_UP));
  }

}
