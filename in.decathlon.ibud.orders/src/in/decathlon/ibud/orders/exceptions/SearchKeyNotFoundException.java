package in.decathlon.ibud.orders.exceptions;

public class SearchKeyNotFoundException extends Exception {

  private static final long serialVersionUID = 1L;
  private String message = "The searchkey is not matching";

  public SearchKeyNotFoundException() {
    super();
    // TODO Auto-generated constructor stub
  }

  public SearchKeyNotFoundException(String message) {
    super(message);
    this.message = message;
  }

  public String toString() {
    return message;
  }

}
