package com.github.jhruzik.salesforceloader

object Account {

  // Import Modules
  import scala.xml.XML

  // Define Function to Login (Retrieve Session ID and Server ID)
  def login(username: String, password: String, token: String): Map[String, Serializable] = {

    // Paste Password and Token
    val password_token = password + token

    // Login Job
    val login_text = List(
      "<?xml version=\"1.0\" encoding=\"utf-8\" ?>",
      "<env:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"",
      "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
      "xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">",
      "<env:Body>",
      "<n1:login xmlns:n1=\"urn:partner.soap.sforce.com\">",
      s"<n1:username>$username</n1:username>",
      s"<n1:password>$password_token</n1:password>",
      "</n1:login>",
      "</env:Body>",
      "</env:Envelope>"
    )

    // Define Header
    val head = Map("Content-Type" -> "text/xml;charset=UTF-8",
                   "SOAPAction" -> "login")

    // Make POST Request
    val response = requests.post("https://login.salesforce.com/services/Soap/u/47.0",
                                 headers = head,
                                 data = login_text.mkString("\n"))

    // Parse Response and put Into Map
    val response_xml = XML.loadString(response.text)
    val response_map = if (response.text.contains("faultstring")) {
      Map("session" -> None, "instance" -> None, "error" -> (response_xml \\ "faultstring").text)
    } else {
      val instance_pat = "https://(.+)\\.salesforce".r
      val serverUrl = (response_xml \\ "serverUrl").text
      val instance = instance_pat.findAllIn(serverUrl).group(1)
      Map("session" -> (response_xml \\ "sessionId").text,
          "instance" -> instance,
          "error" -> None)
    }

    // Return Response Map
    response_map
  }

}
