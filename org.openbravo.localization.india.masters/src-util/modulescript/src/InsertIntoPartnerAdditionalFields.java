package modulescript.src;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class InsertIntoPartnerAdditionalFields extends ModuleScript{
	public void execute() {
		try {
		      ConnectionProvider cp = getConnectionProvider();
		      PreparedStatement ps = cp.getPreparedStatement("INSERT INTO INMD_BPARTNER_EXTNSN (INMD_BPARTNER_EXTNSN_ID, AD_CLIENT_ID , AD_ORG_ID, ISACTIVE ,CREATED ,CREATEDBY  ,UPDATED  ,UPDATEDBY , C_BPARTNER_ID  )  SELECT GET_UUID() AS INMD_BPARTNER_EXTNSN_ID, C_BPARTNER.AD_CLIENT_ID AS AD_CLIENT_ID , C_BPARTNER.AD_ORG_ID AS AD_ORG_ID,'Y' AS ISACTIVE , NOW() AS CREATED , '0' AS CREATEDBY  , NOW() AS UPDATED  , '0' AS UPDATEDBY ,C_BPARTNER.C_BPARTNER_ID AS C_BPARTNER_ID FROM C_BPARTNER LEFT JOIN INMD_BPARTNER_EXTNSN ON INMD_BPARTNER_EXTNSN.C_BPARTNER_ID=C_BPARTNER.C_BPARTNER_ID WHERE  INMD_BPARTNER_EXTNSN.C_BPARTNER_ID IS  NULL");
		      ps.executeUpdate();
		    } catch (Exception e) {
		      handleError(e);
		}
	}

}
