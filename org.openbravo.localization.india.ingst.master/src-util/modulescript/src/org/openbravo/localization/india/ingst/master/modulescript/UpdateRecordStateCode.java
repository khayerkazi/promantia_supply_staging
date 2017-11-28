package org.openbravo.localization.india.ingst.master.modulescript;

import java.sql.PreparedStatement;

import org.apache.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class UpdateRecordStateCode extends ModuleScript {
  private static final Logger log = Logger.getLogger(UpdateRecordStateCode.class);

  @Override
  public void execute() {

    try {

      log.info("Running Module Script For Updateing Indian State Code of Region");
      ConnectionProvider cp = getConnectionProvider();
      PreparedStatement ps = cp
          .getPreparedStatement("update c_region SET description =? , em_ingst_statecode=? where c_country_id =(select c_country_id from c_country where   name ='India')  and name=? ");

      ps.setString(1, "TYPE: STATE  CAPITAL:Srinagar / Jammu");
      ps.setString(2, "01");
      ps.setString(3, "Jammu & Kashmir");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Shimla");
      ps.setString(2, "02");
      ps.setString(3, "Himachal Pradesh");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Chandigarh");
      ps.setString(2, "03");
      ps.setString(3, "Punjab");
      ps.addBatch();

      ps.setString(1, "TYPE: It belongs to union territory  CAPITAL:Chandigarh");
      ps.setString(2, "04");
      ps.setString(3, "Chandigarh");
      ps.addBatch();
      
      ps.setString(1, "Formerly it is called as Uttaranchal");
      ps.setString(2, "05");
      ps.setString(3, "Uttarakhand");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Chandigarh");
      ps.setString(2, "06");
      ps.setString(3, "Haryana");
      ps.addBatch();

      ps.setString(1, "TYPE: It belongs to union territory  CAPITAL:Delhi");
      ps.setString(2, "07");
      ps.setString(3, "Delhi");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Jaipur");
      ps.setString(2, "08");
      ps.setString(3, "Rajasthan");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Lucknow");
      ps.setString(2, "09");
      ps.setString(3, "Uttar Pradesh");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Patna");
      ps.setString(2, "10");
      ps.setString(3, "Bihar");
      ps.addBatch();
      
       ps.setString(1, "TYPE: STATE  CAPITAL:Gangtok");
      ps.setString(2, "11");
      ps.setString(3, "Sikkim");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Itanagar");
      ps.setString(2, "12");
      ps.setString(3, "Arunachal Pradesh");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Kohima");
      ps.setString(2, "13");
      ps.setString(3, "Nagaland");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Imphal");
      ps.setString(2, "14");
      ps.setString(3, "Manipur");
      ps.addBatch();
   
      ps.setString(1, "TYPE: STATE  CAPITAL:Aizawl");
      ps.setString(2, "15");
      ps.setString(3, "Mizoram");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Agartala");
      ps.setString(2, "16");
      ps.setString(3, "Tripura");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Shillong");
      ps.setString(2, "17");
      ps.setString(3, "Meghalaya");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Dispur");
      ps.setString(2, "18");
      ps.setString(3, "Assam");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Kolkata");
      ps.setString(2, "19");
      ps.setString(3, "West Bengal");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Ranchi");
      ps.setString(2, "20");
      ps.setString(3, "Jharkhand");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Bhubaneswar");
      ps.setString(2, "21");
      ps.setString(3, "Orissa");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Raipur");
      ps.setString(2, "22");
      ps.setString(3, "Chattisgarh");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Bhopal");
      ps.setString(2, "23");
      ps.setString(3, "Madhya Pradesh");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Gandhinagar");
      ps.setString(2, "24");
      ps.setString(3, "Gujarat");
      ps.addBatch();

      ps.setString(1, "TYPE: It belongs to union territory  CAPITAL:Daman");
      ps.setString(2, "25");
      ps.setString(3, "Daman & Diu");
      ps.addBatch();      	
      	
      ps.setString(1, "TYPE: It belongs to union territory  CAPITAL:Silvassa");
      ps.setString(2, "26");
      ps.setString(3, "Dadra and Nagar Haveli");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Mumbai");
      ps.setString(2, "27");
      ps.setString(3, "Maharashtra");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Hyderabad");
      ps.setString(2, "28");
      ps.setString(3, "Andhra Pradesh");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Bangalore");
      ps.setString(2, "29");
      ps.setString(3, "Karnataka");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Panaji");
      ps.setString(2, "30");
      ps.setString(3, "Goa");
      ps.addBatch();
	      	
      ps.setString(1, "TYPE: UT  CAPITAL:Kavaratti");
      ps.setString(2, "31");
      ps.setString(3, "Lakshadweep");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Thiruvananthapuram");
      ps.setString(2, "32");
      ps.setString(3, "Kerala");
      ps.addBatch();

      ps.setString(1, "TYPE: STATE  CAPITAL:Chennai");
      ps.setString(2, "33");
      ps.setString(3, "Tamil Nadu");
      ps.addBatch();

      ps.setString(1, "TYPE: It belongs to union territory  CAPITAL:Puducherry");
      ps.setString(2, "34");
      ps.setString(3, "Puducherry");
      ps.addBatch();

      ps.setString(1, "TYPE: It belongs to union territory  CAPITAL:Port Blair");
      ps.setString(2, "35");
      ps.setString(3, "Andaman & Nicobar");
      ps.addBatch();
      
      ps.setString(1, "TYPE: STATE and CAPITAL: Hyderabad ");
      ps.setString(2, "36");
      ps.setString(3, "Telangana");
      ps.addBatch();

      ps.executeBatch();
      ps.executeUpdate();
 
      log.info("Successfully Update Record");

    } catch (Exception e) {
      handleError(e);
    }

  }

}
