package adapter;

import org.apache.log4j.Logger;

import utils.Globals;

public class MailRequest {

  /* This class sends a request to the scvmsapp508 email service. It
   * provides the location of a standard config file. The email service
   * then reads that config file and sends the email. */

  private static final Logger logger = Logger.getLogger(MailRequest.class.getName());

  public static void sendRequest() {
    try {
      String command =
          String.format(
              "WMIC /user:%s /password:%s /node:scvmsapp508 process call create \"psexec -w D:\\Tasks\\MailService -i -s javaw -jar SendMail.jar \\\\t65-w7-eqcash\\incoming\\ChalkServer\\htmlBuild\\config\"",
              Globals.EMAIL_USER, Globals.EMAIL_PW);
      logger.info(command);
      Runtime.getRuntime().exec(command);
      logger.info("Email sent successfully \n");
    } catch (Exception e) {
      logger.info("Error: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    sendRequest();
  }
}
