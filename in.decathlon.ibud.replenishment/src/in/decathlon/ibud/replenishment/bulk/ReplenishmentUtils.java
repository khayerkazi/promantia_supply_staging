package in.decathlon.ibud.replenishment.bulk;

import in.decathlon.ibud.commons.JSONHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

public class ReplenishmentUtils {
	public static final int NB_PRODUCT_PER_ORDER = 1000;

	/**
	 * Utils class no instance.
	 */
	private ReplenishmentUtils() {
	}

	/**
	 * Compare only the hour/minute portion of date.
	 * 
	 * @param dateToBeCompared
	 * @param startDate
	 *            taken from db for that row
	 * @param endDate
	 *            from db for that row
	 * @return
	 */
	public static boolean timeCompare(Date dateToBeCompared, Date startDate, Date endDate) {
		if (startDate == null) {
			throw new OBException("Start date cannot be null");
		}
		if (endDate == null) {
			throw new OBException("End date cannot be null");
		}

		// Compare only the time portion(HH:MM) of start and end date with the date being compared
		Calendar dateComparedHHMM = Calendar.getInstance();
		dateComparedHHMM.setTime(dateToBeCompared);

		Calendar startDateHHMM = mergeCalendar(dateToBeCompared, startDate);
		Calendar endDateHHMM = mergeCalendar(dateToBeCompared, endDate);

		// standard choice : start and end on the same day
		if (startDateHHMM.before(endDateHHMM)) {
			return (dateComparedHHMM.after(startDateHHMM) && dateComparedHHMM.before(endDateHHMM));
		}

		// we also can use the from 23h to 6h format, so...
		// add one day to start and use it as upper boundary
		startDateHHMM.add(Calendar.DAY_OF_MONTH, 1);

		return (dateComparedHHMM.after(endDateHHMM) && dateComparedHHMM.before(startDateHHMM));
	}

	/**
	 * Return a calendar with day part from a calendar and hour part from another
	 * 
	 * @param day
	 *            calendar
	 * @param hour
	 *            calendar
	 */
	private static final Calendar mergeCalendar(Date day, Date hour) {
		Calendar to = Calendar.getInstance();
		to.setTime(day);

		Calendar from = Calendar.getInstance();
		from.setTime(hour);

		to.set(Calendar.HOUR_OF_DAY, from.get(Calendar.HOUR_OF_DAY));
		to.set(Calendar.MINUTE, from.get(Calendar.MINUTE));
		to.set(Calendar.SECOND, 0);
		to.set(Calendar.MILLISECOND, 0);

		return to;
	}

	/**
	 * Create json from order
	 * 
	 */
	public static JSONObject generateJsonPO(List<Order> orderList) throws JSONException {

		JSONObject order = new JSONObject();
		JSONArray ordersArray = new JSONArray();

		if (orderList != null && orderList.size() > 0) {
			ordersArray = createJsonObjOfPO(orderList);
		}
		order.put("data", ordersArray);
		return order;
	}

	/**
	 * Convert order header to json
	 * 
	 * @throws JSONException
	 */
	private static JSONArray createJsonObjOfPO(List<Order> ords) throws JSONException {
		JSONArray jsonOrds = new JSONArray();
		short i = 0;
		for (Order or : ords) {
			JSONObject orderObj = new JSONObject();
			JSONObject orderHeader = new JSONObject();
			JSONArray orderLines = new JSONArray();
			orderHeader = JSONHelper.convetBobToJson(or);
			orderLines = createOrderLineJson(or);
			orderObj.put("Header", orderHeader);
			orderObj.put("Lines", orderLines);

			jsonOrds.put(i, orderObj);
			i++;
		}

		return jsonOrds;
	}

	/**
	 * convert orderline to json
	 * 
	 * @param or
	 * @return
	 * @throws JSONException
	 */
	private static JSONArray createOrderLineJson(Order or) throws JSONException {

		JSONArray ordLines = new JSONArray();

		List<OrderLine> orderLineList = or.getOrderLineList();
		for (OrderLine ol : orderLineList) {
			ordLines.put(JSONHelper.convetBobToJson(ol));

		}
		return ordLines;
	}
}
