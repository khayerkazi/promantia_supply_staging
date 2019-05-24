package in.decathlon.ibud.commons;

import org.openbravo.base.session.OBPropertiesProvider;

public class IbudConfig {

  public static String getSourceFolder() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.sourceFolder").toString();
  }

  public static String getMoveFolder() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.moveFolder").toString();
  }

  public static String getSftpHost() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.sftpHost")
        .toString();
  }

  public static String getSftpUserName() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.sftpUserName").toString();
  }

  public static String getSftpPassword() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.sftpPassword").toString();
  }

  public static String getSftpDestFolder() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.sftpDestFolder").toString();
  }

  public static String getSupplyUsername() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.username")
        .toString();
  }

  public static String getSupplyPassword() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.password")
        .toString();
  }

  public static String getSupplyHost() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.host")
        .toString();
  }

  public static int getSupplyPort() {
    return Integer.parseInt(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.port"));
  }

  public static String getSupplyRowCount() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.rowCount");
  }

  public static String getDuration() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.duration");
  }

  public static String getcsvFile() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.csvpath");
  }

  public static String getSupplyServer() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.server");
  }

  public static String getSupplyContext() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.context");
  }

  public static int getLastUpdatedDays() {
    return Integer.parseInt(OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.lastUpdatedDays"));
  }

  public static String getGrnValidateTime() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.getGrnValidateTime");
  }

  public static String getPosHost() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("pos.host");
  }

  public static String getPosUsername() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("pos.username");
  }

  public static String getPosPassword() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("pos.password");
  }

  public static int getPosPort() {
    return Integer.parseInt(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("pos.port"));
  }

  public static String getWebPosUrl() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("crm.webservice.geturl");
  }

  public static String getErpUrl() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("erpurl");
  }

  public static String getErpuser() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("username");
  }

  public static String geterppwd() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("password");
  }

  public static String getcontextUrl() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ERPInventoryContextUrl");
  }

  public static String getSupplySLUsername() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.slusername").toString();
  }

  public static String getSupplySLPassword() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.slpassword").toString();
  }

  public static String getSupplySLHost() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.slhost")
        .toString();
  }

  public static int getSupplySLPort() {
    return Integer.parseInt(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.slport"));
  }

  public static String getSupplySLRowCount() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.slrowCount");
  }

  public static String getSLDuration() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.slduration");
  }

  public static String getcsvSLFile() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.slcsvpath");
  }

  public static String getSupplySLServer() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("ibud.slserver");
  }

  public static String getSupplySLContext() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.slcontext");
  }

  public static int getSLLastUpdatedDays() {
    return Integer.parseInt(OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("ibud.sllastUpdatedDays"));
  }
}
