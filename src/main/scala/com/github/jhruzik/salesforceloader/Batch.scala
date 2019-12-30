package com.github.jhruzik.salesforceloader

object Batch {

  // Import Modules
  import scala.io.Source
  import scala.xml.XML

  // Define Function to Split Input into Multiple Batches
  def batchify(size: Int, input: String): List[List[String]] = {

    // Validate User Input
    if (size > 10000) throw new IllegalArgumentException("Size must be 10000 or smaller")

    // Read in Input
    val file = Source.fromFile(input)
    val file_list = file.getLines().toList

    // Close File
    file.close()

    // Cut CSV between Header and Content
    val header = file_list(0)
    val content = file_list.tail

    // Check if Content is present
    if (content.isEmpty) throw new IllegalArgumentException("No Rows in CSV")

    // Pack Content into different Batches
    val content_split = content.sliding(size, size).toList

    // Add Header to every Content Batch and Return Result
    content_split.map((x: List[String]) => header :: x)

  }


  // Define Function to add Batch to Job
  def add_batch(batch: String, job: String, instance: String, session: String): Map[String, Serializable] = {

    // Define URL for POST Request
    val post_url = s"https://$instance.salesforce.com/services/async/47.0/job/$job/batch"

    // Define Header
    val header = Map("X-SFDC-Session" -> session,
                     "Content-Type" -> "text/csv; charset=UTF-8")

    // Make POST Call
    val response = requests.post(post_url, headers = header, data = batch)

    // Parse Response and Return Map
    val response_xml = XML.loadString(response.text)
    val response_map = if (response.text.contains("exceptionMessage")) {
      Map("batchid" -> None, "error" -> (response_xml \\ "exceptionMessage").text)
    } else {
      Map("batchid" -> (response_xml \\ "id").text, "error" -> None)
    }

    // Return Response Map
    response_map
  }

  // Define Function to Check Status of Batch
  def check_batch(batch: String, job: String, instance: String, session: String): Map[String, Serializable]  = {

    // Create URL for POST Request
    val post_url = s"https://$instance.salesforce.com/services/async/47.0/job/$job/batch/$batch"

    // Create Header
    val header = Map("X-SFDC-Session" -> session)

    // Make POST Request
    val response = requests.get(post_url, headers = header)

    // Parse Response XML and create Map
    val response_xml = XML.loadString(response.text)
    val response_map = if (response.text.contains("exceptionMessage")) {
      Map("status" -> None, "error" -> (response_xml \\ "exceptionMessage").text)
    } else {
      Map("status" -> (response_xml \\ "state").text, "error" -> None)
    }

    // Return Result Map
    response_map
  }

  // Define Function to get Status of all Batches
  def check_batchesAll(job: String, session: String, instance: String): Map[String, List[String]] = {

    // Define Header
    val header = Map("X-SFDC-Session" -> session)

    // Create URL for GET Request
    val get_url = s"https://$instance.salesforce.com/services/async/47.0/job/$job/batch"

    // Make GET Request
    val response = requests.get(get_url, headers = header)

    // Parse Response XML
    val response_xml = XML.loadString(response.text)

    // Extract List of BatchIds and States
    val batch_ids = (response_xml \\ "id").toList.map(x => x.text)
    val states = (response_xml \\ "state").toList.map(x => x.text)
    val progress = (response_xml \\ "numberRecordsProcessed").toList.map(x => x.text)

    // Return Map
    Map("batches" -> batch_ids, "states" -> states, "progress" -> progress)
  }

  // Define Function to Fetch Result of Batch
  def fetch_result(batch: String, job: String,
                   session: String, instance: String): (String, List[String]) = {

    // Define URL for GET request
    val get_url = s"https://$instance.salesforce.com/services/async/47.0/job/$job/batch/$batch/result"

    // Define Header
    val header = Map("X-SFDC-Session" -> session)

    // Make GET Request
    val response = requests.get(get_url, headers = header)

    // Parse Response CSV
    val response_string = response.text
    val response_csv = response_string.split("\n").toList

    // Split between Header and Content
    val columns = response_csv.head
    val rows = response_csv.tail

    // Return Data as Tuple
    (columns, rows)
  }

  // Define Function to Fetch Result of Query
  def fetch_query(batch: String, job: String,
                  session: String, instance: String): (String, List[String]) = {

    // Define URL for GET Request
    val get_url = s"https://$instance.salesforce.com/services/async/47.0/job/$job/batch/$batch/result"

    // Define Header
    val header = Map("X-SFDC-Session" -> session)

    // Make GET Request
    val response = requests.get(get_url, headers = header)

    // Parse Response XML
    val response_xml = XML.loadString(response.text)
    val result_list = (response_xml \\ "result").toList.map(x => x.text)

    // Define Function to Fetch Chunks
    def fetch_chunks(result: String): String = {

      // Contruct URL for GET Request
      val get_url_data = get_url + s"/$result"

      // Make GET Request
      val result_data = requests.get(get_url_data, headers = header)

      // Return Data
      result_data.text
    }

    // Extract Data for every Result
    val data_list = result_list.map(x => fetch_chunks(x)).map(x => x.split("\n"))

    // Split Columns and Rows
    val cols = data_list(0)(0)
    val rows = data_list.flatMap(x => x.toList.tail)

    // Return Final CSV
    (cols, rows)
  }

}
