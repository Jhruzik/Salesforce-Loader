package com.github.jhruzik.salesforceloader

object Job {

  // Import Modules
  import scala.xml.XML

  // Define Function to create a Job
  def create_job(operation: String, sfobject: String, session: String,
                 instance: String, chunksize:Int = 1000): Map[String, Serializable] = {

    // Validate Arguments
    val operations_allowed = List("insert", "update", "delete", "query")
    if (!operations_allowed.contains(operation)) throw new
        IllegalArgumentException(s"Allowed operations are: ${operations_allowed.mkString(", ")}")

    // Create Configuration XML
    val config_job_list = List(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
      "<jobInfo xmlns=\"http://www.force.com/2009/06/asyncapi/dataload\">",
      s"<operation>$operation</operation>",
      s"<object>$sfobject</object>",
      if (operation == "query") "<concurrencyMode>Parallel</concurrencyMode>"
      else "",
      s"<contentType>CSV</contentType>",
      "</jobInfo>"
    )

    val config_job_string = config_job_list.filter(x => x != "").mkString("\n")

    // Create Header for Post Request
    val header = scala.collection.mutable.Map("X-SFDC-Session" -> session,
                                              "Content-Type" -> "application/xml; charset=UTF-8")
    if (operation == "query") header += ("Sforce-Enable-PKChunking" -> s"chunkSize=$chunksize")

    // Define POST Request URL
    val post_url = s"https://$instance.salesforce.com/services/async/47.0/job"

    // Make POST Request
    val response = requests.post(post_url, headers = header, data = config_job_string)

    // Parse Response and put Into Map
    val response_xml = XML.loadString(response.text)
    val response_map = if (response.text.contains("exceptionMessage")) {
      Map("jobid" -> None, "error" -> (response_xml \\ "exceptionMessage").text)
    } else {
      Map("jobid" -> (response_xml \\ "id").text, "error" -> None)
    }

    // Return Response Map
    response_map
  }

  // Define Function to Close a job
  def close_job(job: String, instance: String, session: String): Unit = {

    // Define POST Request URL
    val post_url = s"https://$instance.salesforce.com/services/async/47.0/job/$job"

    // Define Header
    val header = Map("X-SFDC-Session" -> session,
                     "Content-Type" -> "application/xml; charset=UTF-8")

    // Create Configuration XML
    val close_config = List(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
      "<jobInfo xmlns=\"http://www.force.com/2009/06/asyncapi/dataload\">",
      "<state>Closed</state>",
      "</jobInfo>"
    )
    val close_config_string = close_config.mkString("\n")

    // Make POST Request
    val response = requests.post(post_url, headers = header, data = close_config_string)
  }

}
