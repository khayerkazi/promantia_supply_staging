package in.decathlon.ibud.replenishment.bulk;

public class StatSupplyReturn {
	private int completed = 0;
	private int voided = 0;
	private int draft = 0;

	public int getVoid() {
		return voided;
	}

	public int getCompleted() {
		return completed;
	}

	public int getDraft() {
		return draft;
	}

	public void incVoid() {
		voided++;
	}

	public void incCompleted() {
		completed++;
	}

	public void incDraft() {
		draft++;
	}
}
