package in.decathlon.ibud.orders.client;

public class SOConstants {

  public static final String StoreReqWindowId = "016AB328ABF845D49D22979B6A68D081";
  public static final String RegionalWarehouse = "CAR";
  public static final String ContinentalWarehouse = "CAC";
  public static final String StockReservationAutomatic = "CRP";
  public static final String Standard = "9.7";
  public static final String NonStandard = "54";
  public static final String SpecialCategory = "5";
  public static final String Equipment = "145";
  public static final String DraftDocumentStatus = "DR";
  public static final String SWMovement = "IRC";
  public static final String CompleteDocumentStatus = "CO";
  public static final String User = "Openbravo";
  public static final String PaymentMethod = "Cash";
  public static final String PaymentTerm = "Immediate";
  public static final String disputeWarehouse = "Dispute Warehouse Store";
  public static final String disputeBin = "Dispute Bin";
  public static final String Client = "DSI";
  public static final String POPriceList = "DMI CATALOGUE";
  public static final String jsonId = "id";
  public static final String jsonProduct = "product";
  public static final String jsonOrderedQty = "orderedQuantity";
  public static final String jsonTranxDoc = "transactionDocument";
  public static final String jsonDocNo = "documentNo";
  public static final String jsonCreatedBy = "createdBy";
  public static final String jsonCreationDate = "creationDate";
  public static final String jsonUpdatedBy = "updatedBy";
  public static final String jsonUpdated = "updated";
  public static final String jsonSalesTransaction = "salesTransaction";
  public static final String jsonDocumentAction = "documentAction";
  public static final String jsonProcessed = "processed";
  public static final String jsonOrganization = "organization";
  public static final String jsonBusinessPartner = "businessPartner";
  public static final String jsonPartnerAddress = "partnerAddress";
  public static final String jsonInvoiceAddress = "invoiceAddress";
  public static final String jsonWarehouse = "warehouse";
  public static final String jsonOrderDate = "orderDate";
  public static final String jsonScheduledDeliveryDate = "scheduledDeliveryDate";
  public static final String jsonDocumentType = "documentType";
  public static final String jsonTransactionDocument = "transactionDocument";
  public static final String jsonSalesOrder = "salesOrder";
  public static final String delimiterForGRN = "&&&&\"}";
  public static final String jsonSOPriceList = "priceList";
  public static final String SOPriceList = "DMI CATALOGUE";
  public static final String ServiceKeyProduct = "Product";
  public static final String ServiceKeyPriceList = "PriceList";
  public static final String ServiceKeyOrganization = "Organization";
  public static final String ServiceKeyShipmentInOut = "ShipmentInOut";
  public static final String jsonMovementQuantity = "movementQuantity";
  public static final String ServiceKeyUser = "User";
  public static final String performanceTest = "[pt]";
  public static final String picked = "IBDO_PK";
  public static final String shipped = "IBDO_SH";
  public static final String pickListShippedStatus = "IBUDPK_SH";
  public static final String partialPicked = "OBWPL_PPK";
  public static final String partialShipped = "IBDO_PSH";
  public static final String partialRecieved = "IBDO_PR";
  public static final String TreeType = "OO";
  public static final String UserId = "100";
  public static final String ReturnToVendorWindowId = "C50A8AEE6F044825B5EF54FAAE76826F";
  public static final String Manual = "Y";
  public static final String manualReturnDocType = "SRN";
  public static final String implantationMovementType = "IMPL";
  public static final String closed = "CL";
  public static final String voided = "VO";
  public static final String perfOrmanceEnhanced = "[pte]";
  public static final String RECORD_ID = "recordId";
  public static final String recordIdentifier = "recordIdentifier";
  public static final String TABLE_NAME = "tableName";
  public static final String ERROR = "Error";
  public static final String ECOMORGID = "B2D0E3B212614BA6989ADCA3074FC423";
  public static final String B2BOrgId = "076DD16AEA914588A919422D1C5FF037";
  public static final String POS = "POS";
  public static final String ECOM = "ECOM";
  public static final String ECOMReturns = "ECOMReturns";
  public static final String POSReturns = "POSReturns";
  public static final String salesDocType = "ARI";
  public static final String returnsDocType = "ARI_RM";
  public static final String STORE = "Store";
  public static final String STORECASH = "STORECASH";
  public static final String MODULE_NAME = "in.decathlon.supply.dc";
  public static final String MODULE_NAME_FTS = "in.decathlon.factorytostore";
  public static final String DSI_WEARHOUSE = "603C6A266B4C40BCAD87C5C43DDF53EE";
  public static final String NCN_CUSTOMER_EXCHANGE = "NCN_Customer Exchange";
  public static final String SBIN = "E-commerce Return Bin";
  public static final String SODOCTYPEID = "9DAB1E7146FE4F488AA873EA0CB05824";
  public static final String EcomOrder = "Ecommerce";
  public static final String completeGRNProcessId = "A0AA4CB8C64341288203FB1B783F0BA4";

  public static final String ServiceKeystatusUpdate = "StatusUpdate";
  public static final String ServiceKeycreateTruck = "obwship";

  public static final String ServiceKeyFTSstatusUpdate = "FTSStatusUpdate";
  public static final String ServiceKeyFTScreateTruck = "FTSobwship";
  public static final String ServiceKeyFTSShipmentInOut = "FTSShipmentInOut";

  public static final String directDelivery = "FACST_DD";
  public static final String FOBPriceList = "FOB%";

  public static enum Status {
    CO, OBWPL_PPK, IBDO_PK, IBDO_PSH, IBDO_SH, IBDO_PR, CL
  }

}
