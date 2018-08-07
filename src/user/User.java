package user;

import java.io.Serializable;

public class User implements Serializable {
  private static final long serialVersionUID = 8175657762140757012L;
  public boolean authorized;
  public boolean ees;
  public String firstName;
  public String lastName;
  public String ip;

  public User() {
    this.authorized = false;
    this.ees = false;
    this.firstName = "";
    this.lastName = "";
    this.ip = "";
  }

  public User(boolean authorized, boolean ees, String firstName, String lastName, String ip) {
    this.authorized = authorized;
    this.ees = ees;
    this.firstName = firstName;
    this.lastName = lastName;
    this.ip = ip;
  }

  @Override
  public String toString() {
    return "User [authorized="
        + authorized
        + ", firstName="
        + firstName
        + ", lastName="
        + lastName
        + ", ip="
        + ip
        + "]";
  }

  public String getKey() {
    return ip;
  }

  public String getFullname() {
    return firstName + " " + lastName;
  }
}
