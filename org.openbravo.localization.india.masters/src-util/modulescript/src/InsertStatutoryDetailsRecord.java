package modulescript.src;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class InsertStatutoryDetailsRecord extends ModuleScript {

	@Override
	public void execute() {
		try {
		      ConnectionProvider cp = getConnectionProvider();
		      PreparedStatement ps = cp.getPreparedStatement("INSERT INTO INMD_TAXDETAILS (INMD_TAXDETAILS_ID,AD_CLIENT_ID ,AD_ORG_ID ,ISACTIVE ,CREATED ,CREATEDBY  ,UPDATED  ,UPDATEDBY ,TYPE_OF_COMPANY) SELECT  GET_UUID() AS INMD_TAXDETAILS_ID, AD_ORG.AD_CLIENT_ID , AD_ORG.AD_ORG_ID , 'Y' AS ISACTIVE , NOW() AS CREATED , '0' AS CREATEDBY  , NOW() AS UPDATED  , '0' AS UPDATEDBY , ' ' AS TYPE_OF_COMPANY FROM AD_ORG LEFT JOIN INMD_TAXDETAILS ON INMD_TAXDETAILS.AD_ORG_ID = AD_ORG.AD_ORG_ID WHERE  AD_ORG.AD_CLIENT_ID <> '0' AND INMD_TAXDETAILS.AD_ORG_ID IS NULL");
		      ps.executeUpdate();
		    } catch (Exception e) {
		      handleError(e);
		}
	}
}
