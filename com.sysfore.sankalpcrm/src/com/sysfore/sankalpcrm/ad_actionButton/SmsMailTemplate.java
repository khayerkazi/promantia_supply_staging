package com.sysfore.sankalpcrm.ad_actionButton;

public class SmsMailTemplate {
  public static String sendMailCreateTemplate(String name, String oxyleid, String email,
      String password) {

    StringBuffer message = new StringBuffer();
    message        
        .append(
            "<pre>Dear ")
        .append(name)       
        .append(
            ",\n \nThis is a confirmation mail that your company registration has been approved and your account successfully created with Decathlon Sports India.")
        .append("\n\nDecathlon registration number: ")
        .append(oxyleid)
        .append(
            "\n\nYou can collect your membership card when you are visiting our store at the welcome desk. Simply print this email as a proof of registration.Please do carry photo ID along")
        .append(
            "\n\nFor further details please contact: contact.india@decathlon.com \n080-71008600 \n \nSee you at the DECATHLON STORE soon!!!");
             
    
    return message.toString();

  }

  public static String sendMailUpdateTemplate(String name, String oxyleid, String email,
      String password,String companyName,String licenseNo,String rFirstName,
      String rLastName,String rDesignation,String rMobile,String rEmail,String billingAddress,String listOfSports,String news) {

    StringBuffer message = new StringBuffer();
    message
        .append(
            "<pre>Dear ")
        .append(name)
        .append(
            " ,\n \n This is a confirmation mail that your registration has been updated.\n Company registration:")
        .append(
            "\n &nbsp;&nbsp;-Decathlon registration number: ")
        .append(oxyleid)
        .append("\n \n Company details:")
        .append("\n &nbsp;&nbsp;-Company name: ")
        .append(companyName)
        .append("\n &nbsp;&nbsp;-Business License Number: ")
        .append(licenseNo)
        .append("\n\n Company representative:\n &nbsp;&nbsp;&nbsp;-First name: ")
        .append(rFirstName)
        .append("\n &nbsp;&nbsp;-Last name: ")
        .append(rLastName)
        .append("\n &nbsp;&nbsp;-Designation: ")
        .append(rDesignation)
        .append("\n &nbsp;&nbsp;-Email: ")
        .append(rEmail)
        .append("\n &nbsp;&nbsp;-Phone/Mobile: ")
        .append(rMobile)
        .append("\n \n Company billing address: ")
        .append(billingAddress)
        .append("\n list of sports : ")
        .append(listOfSports)
        .append(
            "\n \n Would you like to receive information about Decathlon and new products available in India :")
        .append(news)
        .append(
            "\n \n See you soon on \n\n<a href=http://www.decathlon.in>www.decathlon.in</a>\n India Sports cash and carry")
        .append(
            "\n \n Order hotline: +91 80 25741509</pre>");
    return message.toString();

  }

  public static String sendMailDisableTemplate(String name) {

    StringBuffer message = new StringBuffer();
    message        
        .append(
            "<pre>Dear ")
        .append(name)
        .append(
            ",\n \n")
        .append(
            "Unfortunately we were not in a position to confirm your company registration with the information you provided.\n ")
        .append(
            "\n Explanation provided:we did not receive the required documentation from you, hence your registration has been disabled in our systems. Please note that you can still complete")
        .append(
            "your registration by providing the documents later.\n \n Please contact us to study how we can solve this issue and allow you to benefit from our range of product.\n See you soon on")
        .append(
            "\n <a href=http://www.decathlon.in>www.decathlon.in</a>\n India Sports cash and carry \n \n Order hotline: +91 80 25741509")
        .append("</pre>");
    return message.toString();
  }

  public static String sendSMSTemplate(String mobileNo, String oxylane, String password) {
    StringBuffer sms = new StringBuffer();
    sms
        .append(
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:tem=\"http://tempuri.org/\">")
        .append(
            "<soap:Header/><soap:Body><tem:SendMessage><tem:accountId>decathlon</tem:accountId><tem:password>ajw81d3</tem:password>")
        .append("<tem:smsMessage><tem:MessageId>Johnson</tem:MessageId><tem:Mobile>")
        .append(mobileNo)
        .append(
            "</tem:Mobile><tem:Message>Welcome to Decathlon experience!\nYou have been successfully registered \nYour ID is ")
        .append(oxylane)        
        .append(" \n\"Play more, pay less\".\nSportingly \n Dhiren Shetty")
        .append(
            "</tem:Message><tem:Priority>1</tem:Priority><tem:SenderId>DCATHLON</tem:SenderId></tem:smsMessage></tem:SendMessage></soap:Body></soap:Envelope>");
    return sms.toString();
  }
}
