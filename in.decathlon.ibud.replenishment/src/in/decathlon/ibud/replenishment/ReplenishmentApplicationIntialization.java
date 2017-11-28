package in.decathlon.ibud.replenishment;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.openbravo.client.kernel.ApplicationInitializer;
import org.openbravo.dal.service.OBDal;

public class ReplenishmentApplicationIntialization implements ApplicationInitializer {

  @Override
  public void initialize() {
    OBDal.getInstance().registerSQLFunction("ibdrep_get_qty_on_order",
        new StandardSQLFunction("ibdrep_get_qty_on_order", StandardBasicTypes.BIG_DECIMAL));
    OBDal.getInstance().registerSQLFunction("ibdrep_get_stock",
            new StandardSQLFunction("ibdrep_get_stock", StandardBasicTypes.BIG_DECIMAL));
        
    
  }

}

