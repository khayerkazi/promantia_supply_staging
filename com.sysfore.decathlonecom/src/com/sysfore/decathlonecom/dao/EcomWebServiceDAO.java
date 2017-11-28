package com.sysfore.decathlonecom.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.openbravo.erpCommon.utility.SequenceIdData;

import com.sysfore.decathlonecom.model.EcomAddress;
import com.sysfore.decathlonecom.model.EcomCustomer;

public class EcomWebServiceDAO {

  Connection conn = null;
  ResourceBundle properties = null;

  public EcomWebServiceDAO() throws Exception {
  System.out.println("I am in EcomWebServiceDAO");
    properties = ResourceBundle.getBundle("com.sysfore.decathlonecom.Openbravo");
    Class.forName(properties.getString("bbdd.driver"));
    conn = DriverManager.getConnection(properties.getString("bbdd.url") + "/"
        + properties.getString("bbdd.sid"), properties.getString("bbdd.user"), properties
        .getString("bbdd.password"));

    conn.setAutoCommit(false);

  }

  public TreeMap<String, String> selectSports() {
    String sql = "Select value, name from rc_sport";
    TreeMap<String, String> rcSport = new TreeMap<String, String>();
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      rs = pst.executeQuery();

      while (rs.next()) {
        rcSport.put(rs.getString("value"), rs.getString("name"));
      }
      rs.close();
      pst.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcSport;
  }

  public String selectGreetings(String value) {
    String sql = "select c_greeting_id from c_greeting where name = (select name from rc_greeting where value=?)";
    String rcGreeting = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcGreeting = rs.getString("c_greeting_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcGreeting;
  }

  public String selectCountry(String value) {

    String sql = "select c_country_id from c_country where name = ?";
    String rcCountry = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcCountry = rs.getString("c_country_id");
      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcCountry;
  }

  public String selectState(String value) {

    String sql = "select c_region_id from c_region where value = ?";
    String rcCity = "";
    PreparedStatement pst = null;
    ResultSet rs = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        rcCity = rs.getString("c_region_id");
      }
      System.out.println("City is " + rcCity);
      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcCity;
  }

  public LinkedList<String> selectCompanyDetails(String value) {
    String sql = "select rc_company_id,rc_license_id,licenseno,companyaddress from rc_company where lower(companyname) = lower(?) LIMIT 1";

    LinkedList<String> rcCompany = new LinkedList<String>();
    PreparedStatement pst = null;
    ResultSet rs = null;
    PreparedStatement pst1 = null;
    ResultSet rs1 = null;
    try {

      pst = conn.prepareStatement(sql);
      pst.setString(1, value);
      rs = pst.executeQuery();

      if (rs.next()) {
        System.out.print("Inside the next");
        rcCompany.add(0, rs.getString("rc_company_id"));
        rcCompany.add(1, rs.getString("rc_license_id"));
        rcCompany.add(2, rs.getString("licenseno"));
        rcCompany.add(3, rs.getString("companyaddress"));
      } else {

        pst1 = conn.prepareStatement(sql);
        pst1.setString(1, "Decathlon Default");
        rs1 = pst1.executeQuery();

        if (rs1.next()) {
          System.out.print("Inside the Fallse");
          rcCompany.add(0, rs1.getString("rc_company_id"));
          rcCompany.add(1, rs1.getString("rc_license_id"));
          rcCompany.add(2, rs1.getString("licenseno"));
          rcCompany.add(3, rs1.getString("companyaddress"));
        }

      }

      pst.close();
      rs.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      pst = null;
      rs = null;
    }

    return rcCompany;
  }

  public String createEcomCustomer(EcomCustomer ecomCustomer) {
    String sql = "INSERT INTO c_bpartner (c_bpartner_id,ad_client_id,ad_org_id,value,createdby,updatedby,em_rc_source,em_rc_membertype,"
        + "c_greeting_id,name,name2,isactive,c_bp_group_id,em_rc_optin,em_rc_email,em_rc_mobile,em_rc_status,em_rc_company_id,em_rc_location,"
        + "em_rc_license_id,em_rc_licenseno,em_rc_aikido,em_rc_archery,em_rc_alpinism,em_rc_basketball,em_rc_badminton,em_rc_boxing,em_rc_cricket,"
        + "em_rc_cycling,em_rc_climbing,em_rc_diving,em_rc_dance,em_rc_fishing,em_rc_football,em_rc_fieldhockey,em_rc_fitness,em_rc_gym,em_rc_golf,"
        + "em_rc_handball,em_rc_horseriding,em_rc_hiking,em_rc_judo,em_rc_karate,em_rc_kitesurfing,em_rc_paddle,em_rc_rollerskating,em_rc_running,em_rc_rugby,"
        + "em_rc_sailing,em_rc_skiing,em_rc_snowboarding,em_rc_squash,em_rc_surfing,em_rc_swimming,em_rc_tennis,em_rc_tabletennis,em_rc_volleyball,"
        + "em_rc_windsurfing,em_rc_yoga,em_rc_comments,em_rc_conditions) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
    String sqlPhone = "select c_bpartner_id from c_bpartner where em_rc_mobile = ?";
    String rcOxylane = "failure";
    PreparedStatement pst0 = null;
    ResultSet rs0 = null;
    ResultSet rs3 = null;
    ResultSet rs4 = null;
    PreparedStatement pst = null;
    PreparedStatement pst1 = null;
    PreparedStatement pst2 = null;
    PreparedStatement pst3 = null;
    String uuidCustomer = "";
    String uuidLocation = "";

    int result = 0;
    try {
      pst0 = conn.prepareStatement(sqlPhone);
      pst0.setString(1, ecomCustomer.getMobile());
      rs0 = pst0.executeQuery();

      if (rs0.next()) {
        rs0.close();
        pst0.close();

        return "duplicate mobile";
      }

      pst = conn.prepareStatement(sql);

      uuidCustomer = SequenceIdData.getUUID();

      pst.setString(1, uuidCustomer);
      pst.setString(2, properties.getString("ws.client"));
      pst.setString(3, properties.getString("ws.organisation"));
      pst.setString(4, ecomCustomer.getMobile());
      pst.setString(5, properties.getString("ws.user"));
      pst.setString(6, properties.getString("ws.user"));
      pst.setString(7, ecomCustomer.getSource());
      pst.setString(8, ecomCustomer.getMemberType());
      pst.setString(9, ecomCustomer.getGrretingId());
      pst.setString(10, ecomCustomer.getFirstName());
      pst.setString(11, ecomCustomer.getLastName());
      pst.setString(12, "Y");
      pst.setString(13, properties.getString("ws.memebrcatagories"));
      pst.setString(14, ecomCustomer.getOptIn());
      pst.setString(15, ecomCustomer.getEmail());
      pst.setString(16, ecomCustomer.getMobile());
      pst.setString(17, ecomCustomer.getStatus());
      pst.setString(18, ecomCustomer.getCompanyId());
      pst.setString(19, ecomCustomer.getCompanyAddress());
      pst.setString(20, ecomCustomer.getLicenseId());
      pst.setString(21, ecomCustomer.getLicenseNo());
      pst.setString(22, ecomCustomer.getAikido());
      pst.setString(23, ecomCustomer.getArchery());
      pst.setString(24, ecomCustomer.getAlpinism());
      pst.setString(25, ecomCustomer.getBasket());
      pst.setString(26, ecomCustomer.getBadminton());
      pst.setString(27, ecomCustomer.getBoxing());
      pst.setString(28, ecomCustomer.getCricket());
      pst.setString(29, ecomCustomer.getCycling());
      pst.setString(30, ecomCustomer.getClimbing());
      pst.setString(31, ecomCustomer.getDiving());
      pst.setString(32, ecomCustomer.getDance());
      pst.setString(33, ecomCustomer.getFishing());
      pst.setString(34, ecomCustomer.getFootball());
      pst.setString(35, ecomCustomer.getFieldHockey());
      pst.setString(36, ecomCustomer.getFitness());
      pst.setString(37, ecomCustomer.getGym());
      pst.setString(38, ecomCustomer.getGolf());
      pst.setString(39, ecomCustomer.getHandball());
      pst.setString(40, ecomCustomer.getHorseRiding());
      pst.setString(41, ecomCustomer.getHiking());
      pst.setString(42, ecomCustomer.getJudo());
      pst.setString(43, ecomCustomer.getKarate());
      pst.setString(44, ecomCustomer.getKiteSurfing());
      pst.setString(45, ecomCustomer.getPaddle());
      pst.setString(46, ecomCustomer.getRollerskating());
      pst.setString(47, ecomCustomer.getRunning());
      pst.setString(48, ecomCustomer.getRugby());
      pst.setString(49, ecomCustomer.getSailing());
      pst.setString(50, ecomCustomer.getSkiing());
      pst.setString(51, ecomCustomer.getSnowboarding());
      pst.setString(52, ecomCustomer.getSquash());
      pst.setString(53, ecomCustomer.getSurfing());
      pst.setString(54, ecomCustomer.getSwimming());
      pst.setString(55, ecomCustomer.getTennis());
      pst.setString(56, ecomCustomer.getTableTennis());
      pst.setString(57, ecomCustomer.getVolleyBall());
      pst.setString(58, ecomCustomer.getWindsurfing());
      pst.setString(59, ecomCustomer.getYoga());
      pst.setString(60, ecomCustomer.getComments());
      //pst.setString(61, properties.getString("ws.terms"));
	 pst.setString(61, "Member has accepted Decathlon Terms and Conditions");
      result = pst.executeUpdate();
      if (result == 0) {
        pst.close();
        conn.rollback();
        return "failure";
      } else {
        uuidLocation = SequenceIdData.getUUID();
        pst1 = conn
            .prepareStatement("insert into c_location (c_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,address1,address2,em_rc_address3,em_rc_address4,city,postal,c_country_id,c_region_id)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        pst1.setString(1, uuidLocation);
        pst1.setString(2, properties.getString("ws.client"));
        pst1.setString(3, properties.getString("ws.organisation"));
        pst1.setString(4, "Y");
        pst1.setString(5, properties.getString("ws.user"));
        pst1.setString(6, properties.getString("ws.user"));
        pst1.setString(7, ((ecomCustomer.getEcomAddress()).get(0)).getAddress1());
        pst1.setString(8, ((ecomCustomer.getEcomAddress()).get(0)).getAddress2());
        pst1.setString(9, ((ecomCustomer.getEcomAddress()).get(0)).getAddress3());
        pst1.setString(10, ((ecomCustomer.getEcomAddress()).get(0)).getAddress4());
        pst1.setString(11, ((ecomCustomer.getEcomAddress()).get(0)).getCity());
        pst1.setString(12, ((ecomCustomer.getEcomAddress()).get(0)).getPostalCode());
        // System.out.println("Country " + ((ecomCustomer.getEcomAddress()).get(0)).getCountry());
        pst1.setString(13, selectCountry(((ecomCustomer.getEcomAddress()).get(0)).getCountry()));
        pst1.setString(14, selectState(((ecomCustomer.getEcomAddress()).get(0)).getState()));
        result = pst1.executeUpdate();

        if (result == 0) {
          pst1.close();
          conn.rollback();
          return "failure";
        }
      }

      pst2 = conn
          .prepareStatement("insert into c_bpartner_location (c_bpartner_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,name,c_location_id,c_bpartner_id) values (?,?,?,?,?,?,?,?,?)");
      pst2.setString(1, uuidLocation);
      pst2.setString(2, properties.getString("ws.client"));
      pst2.setString(3, properties.getString("ws.organisation"));
      pst2.setString(4, "Y");
      pst2.setString(5, properties.getString("ws.user"));
      pst2.setString(6, properties.getString("ws.user"));
      pst2.setString(7, "." + ecomCustomer.getFirstName() + "," + ecomCustomer.getLastName());
      pst2.setString(8, uuidLocation);
      pst2.setString(9, uuidCustomer);

      result = pst2.executeUpdate();

      rs0.close();
      pst0.close();
      pst.close();
      pst1.close();
      pst2.close();

      conn.commit();

      pst3 = conn.prepareStatement("select em_rc_oxylane from c_bpartner where em_rc_mobile =?");
      pst3.setString(1, ecomCustomer.getMobile());
      rs3 = pst3.executeQuery();

      if (rs3.next()) {
        rcOxylane = rs3.getString("em_rc_oxylane");
      }

    } catch (Exception e) {
      try {
        conn.rollback();

      } catch (SQLException sqe) {

      }
      e.printStackTrace();
    } finally {
      pst = null;

    }

    return rcOxylane;
  }

  public String updateEcomCustomer(EcomCustomer ecomCustomer) {
    String sql = "UPDATE c_bpartner set ad_client_id=?,ad_org_id=?,value=?,updatedby=?,em_rc_source=?,em_rc_membertype=?,"
        + "c_greeting_id=?,name=?,name2=?,isactive=?,c_bp_group_id=?,em_rc_optin=?,em_rc_email=?,em_rc_mobile=?,em_rc_status=?,em_rc_company_id=?,em_rc_location=?,"
        + "em_rc_license_id=?,em_rc_licenseno=?,em_rc_aikido=?,em_rc_archery=?,em_rc_alpinism=?,em_rc_basketball=?,em_rc_badminton=?,em_rc_boxing=?,em_rc_cricket=?,"
        + "em_rc_cycling=?,em_rc_climbing=?,em_rc_diving=?,em_rc_dance=?,em_rc_fishing=?,em_rc_football=?,em_rc_fieldhockey=?,em_rc_fitness=?,em_rc_gym=?,em_rc_golf=?,"
        + "em_rc_handball=?,em_rc_horseriding=?,em_rc_hiking=?,em_rc_judo=?,em_rc_karate=?,em_rc_kitesurfing=?,em_rc_paddle=?,em_rc_rollerskating=?,em_rc_running=?,em_rc_rugby=?,"
        + "em_rc_sailing=?,em_rc_skiing=?,em_rc_snowboarding=?,em_rc_squash=?,em_rc_surfing=?,em_rc_swimming=?,em_rc_tennis=?,em_rc_tabletennis=?,em_rc_volleyball=?,"
        + "em_rc_windsurfing=?,em_rc_yoga= ?,em_rc_comments=? where em_rc_oxylane = ? ";

    String sqlOxylane = "select c_bpartner_id from c_bpartner where em_rc_oxylane = ?";
    String sqlPhone = "select c_bpartner_id from c_bpartner where em_rc_mobile = ?";
    String sqlLocation = "select c_bpartner.c_bpartner_id, c_location_id from c_bpartner_location,c_bpartner where c_bpartner.c_bpartner_id=c_bpartner_location.c_bpartner_id and em_rc_oxylane =? ";
    String rcOxylane = "failure";
    PreparedStatement pst0 = null;
    ResultSet rs0 = null;
    ResultSet rs3 = null;
    ResultSet rs4 = null;
    PreparedStatement pst = null;
    PreparedStatement pst1 = null;
    PreparedStatement pst2 = null;
    PreparedStatement pst3 = null;
    PreparedStatement pst4 = null;
    PreparedStatement pst10 = null;
    PreparedStatement pst11 = null;
    String uuidCustomer = "";
    String uuidLocation = "";
    String bPartnerId = "";

    int result = 0;
    try {
      pst0 = conn.prepareStatement(sqlOxylane);
      pst0.setString(1, ecomCustomer.getOxylane());
      rs0 = pst0.executeQuery();

      if (rs0.next()) {
        bPartnerId = rs0.getString("c_bpartner_id");
      } else {
        rs0.close();
        pst0.close();
        return "Oxylane not existed";
      }

      pst3 = conn.prepareStatement(sqlPhone);
      pst3.setString(1, ecomCustomer.getMobile());
      rs3 = pst3.executeQuery();

      if (rs3.getFetchSize() > 1) {
        rs3.close();
        pst3.close();

        return "duplicate mobile";
      }
      System.out.println("Check Duplicatemobile");
      pst = conn.prepareStatement(sql);

      uuidCustomer = SequenceIdData.getUUID();

      pst.setString(1, properties.getString("ws.client"));
      pst.setString(2, properties.getString("ws.organisation"));
      pst.setString(3, ecomCustomer.getMobile());
      pst.setString(4, properties.getString("ws.user"));
      pst.setString(5, ecomCustomer.getSource());
      pst.setString(6, ecomCustomer.getMemberType());
      pst.setString(7, ecomCustomer.getGrretingId());
      pst.setString(8, ecomCustomer.getFirstName());
      pst.setString(9, ecomCustomer.getLastName());
      pst.setString(10, "Y");
      pst.setString(11, properties.getString("ws.memebrcatagories"));
      pst.setString(12, ecomCustomer.getOptIn());
      pst.setString(13, ecomCustomer.getEmail());
      pst.setString(14, ecomCustomer.getMobile());
      pst.setString(15, ecomCustomer.getStatus());
      pst.setString(16, ecomCustomer.getCompanyId());
      pst.setString(17, ecomCustomer.getCompanyAddress());
      pst.setString(18, ecomCustomer.getLicenseId());
      pst.setString(19, ecomCustomer.getLicenseNo());
      pst.setString(20, ecomCustomer.getAikido());
      pst.setString(21, ecomCustomer.getArchery());
      pst.setString(22, ecomCustomer.getAlpinism());
      pst.setString(23, ecomCustomer.getBasket());
      pst.setString(24, ecomCustomer.getBadminton());
      pst.setString(25, ecomCustomer.getBoxing());
      pst.setString(26, ecomCustomer.getCricket());
      pst.setString(27, ecomCustomer.getCycling());
      pst.setString(28, ecomCustomer.getClimbing());
      pst.setString(29, ecomCustomer.getDiving());
      pst.setString(30, ecomCustomer.getDance());
      pst.setString(31, ecomCustomer.getFishing());
      pst.setString(32, ecomCustomer.getFootball());
      pst.setString(33, ecomCustomer.getFieldHockey());
      pst.setString(34, ecomCustomer.getFitness());
      pst.setString(35, ecomCustomer.getGym());
      pst.setString(36, ecomCustomer.getGolf());
      pst.setString(37, ecomCustomer.getHandball());
      pst.setString(38, ecomCustomer.getHorseRiding());
      pst.setString(39, ecomCustomer.getHiking());
      pst.setString(40, ecomCustomer.getJudo());
      pst.setString(41, ecomCustomer.getKarate());
      pst.setString(42, ecomCustomer.getKiteSurfing());
      pst.setString(43, ecomCustomer.getPaddle());
      pst.setString(44, ecomCustomer.getRollerskating());
      pst.setString(45, ecomCustomer.getRunning());
      pst.setString(46, ecomCustomer.getRugby());
      pst.setString(47, ecomCustomer.getSailing());
      pst.setString(48, ecomCustomer.getSkiing());
      pst.setString(49, ecomCustomer.getSnowboarding());
      pst.setString(50, ecomCustomer.getSquash());
      pst.setString(51, ecomCustomer.getSurfing());
      pst.setString(52, ecomCustomer.getSwimming());
      pst.setString(53, ecomCustomer.getTennis());
      pst.setString(54, ecomCustomer.getTableTennis());
      pst.setString(55, ecomCustomer.getVolleyBall());
      pst.setString(56, ecomCustomer.getWindsurfing());
      pst.setString(57, ecomCustomer.getYoga());
      pst.setString(58, ecomCustomer.getComments());
      pst.setString(59, ecomCustomer.getOxylane());
      result = pst.executeUpdate();
      System.out.println("Result  " + result);
      if (result == 0) {
        pst.close();
        conn.rollback();
        return "failure";
      } else {

        pst4 = conn.prepareStatement(sqlLocation);
        pst4.setString(1, ecomCustomer.getOxylane());
        rs4 = pst4.executeQuery();

        while (rs4.next()) {
          String location_id = rs4.getString("c_location_id");
          pst10 = conn.prepareStatement("delete from c_bpartner_location where c_location_id = ?");
          pst10.setString(1, location_id);
          result = pst10.executeUpdate();

          pst11 = conn.prepareStatement("delete from c_location where c_location_id = ?");
          pst11.setString(1, location_id);
          result = pst11.executeUpdate();
        }

        rs4.close();
        pst4.close();

        List<EcomAddress> address = ecomCustomer.getEcomAddress();
        for (int i = 0; i < address.size(); i++) {
          uuidLocation = SequenceIdData.getUUID();
          pst1 = conn
              .prepareStatement("insert into c_location (c_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,address1,address2,em_rc_address3,em_rc_address4,city,postal,c_country_id,c_region_id)values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
          pst1.setString(1, uuidLocation);
          pst1.setString(2, properties.getString("ws.client"));
          pst1.setString(3, properties.getString("ws.organisation"));
          pst1.setString(4, "Y");
          pst1.setString(5, properties.getString("ws.user"));
          pst1.setString(6, properties.getString("ws.user"));
          pst1.setString(7, address.get(i).getAddress1());
          pst1.setString(8, address.get(i).getAddress2());
          pst1.setString(9, address.get(i).getAddress3());
          pst1.setString(10, address.get(i).getAddress4());
          pst1.setString(11, address.get(i).getCity());
          pst1.setString(12, address.get(i).getPostalCode());
          pst1.setString(13, selectCountry(address.get(i).getCountry()));
          pst1.setString(14, selectState(address.get(i).getState()));
          result = pst1.executeUpdate();

          if (!uuidLocation.equals("")) {
            // uuidLocation = SequenceIdData.getUUID();
            pst2 = conn
                .prepareStatement("insert into c_bpartner_location (c_bpartner_location_id,ad_client_id,ad_org_id,isactive,createdby,updatedby,name,c_location_id,c_bpartner_id) values (?,?,?,?,?,?,?,?,?)");
            pst2.setString(1, uuidLocation);
            pst2.setString(2, properties.getString("ws.client"));
            pst2.setString(3, properties.getString("ws.organisation"));
            pst2.setString(4, "Y");
            pst2.setString(5, properties.getString("ws.user"));
            pst2.setString(6, properties.getString("ws.user"));
            pst2.setString(7, "." + ecomCustomer.getFirstName() + "," + ecomCustomer.getLastName());
            pst2.setString(8, uuidLocation);
            pst2.setString(9, bPartnerId);

            result = pst2.executeUpdate();
            pst2.close();
          }

        }

      }
      rs0.close();
      pst0.close();
      pst.close();
      pst1.close();

      conn.commit();

    } catch (Exception e) {
      e.printStackTrace();
      try {
        conn.rollback();
        // System.out.println("Inside the roll back");
        return "failure";
      } catch (SQLException sqe) {

      }

      return "failure";
    } finally {
      pst = null;

    }

    return "success";

  }

  public void closeConnection() throws SQLException {
	       if (conn == null) {
       return;
    }
    conn.close();
    conn = null;
  }
}
