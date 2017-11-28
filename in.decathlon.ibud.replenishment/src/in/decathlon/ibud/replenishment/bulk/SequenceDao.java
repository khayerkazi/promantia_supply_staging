package in.decathlon.ibud.replenishment.bulk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.model.ad.utility.Sequence;

public class SequenceDao {

	/**
	 * Get the current number of a sequence and update it by adding nbOfOrder to prevent multiple call.
	 * 
	 * Note we are on a separate transaction to prevent lock if multiple things come at a time
	 * 
	 * @param sequence
	 * @param nbOfOrder
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public int getSeqNumAndAddNumberInNewTransaction(Sequence sequence, int nbOfOrder) {

		Session session = SessionFactoryController.getInstance().getSessionFactory().openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			Connection conn = session.connection();

			String queryGet = "select incrementno,currentnext from ad_sequence where ad_sequence_id=? for update";

			PreparedStatement stmt = conn.prepareStatement(queryGet);
			stmt.setString(1, sequence.getId());
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				throw new OBException("No sequence found with id from sequenceID???");
			}

			int incrementno = rs.getInt(1);
			int currentnext = rs.getInt(2);

			rs.close();
			stmt.close();

			String queryUpdt = "update ad_sequence set currentnext=? where ad_sequence_id=? ";

			PreparedStatement stmtU = conn.prepareStatement(queryUpdt);
			stmtU.setInt(1, currentnext + (incrementno * nbOfOrder));
			stmtU.setString(2, sequence.getId());

			stmtU.executeUpdate();

			tx.commit();
			return currentnext;

		} catch (Exception e) {
			if (tx != null) {
				tx.rollback();
			}
			throw new OBException(e);
		} finally {
			session.close();
		}
	}

}
