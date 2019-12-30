package com.github.jhruzik.salesforceloader

object Loader {

  // Import Modules
  import com.github.jhruzik.salesforceloader.Account
  import com.github.jhruzik.salesforceloader.Batch
  import com.github.jhruzik.salesforceloader.Job
  import java.io.File

  // Define Class that will hold login information
  case class login_info(session: String, instance: String)

  // Define Function to Login
  def login(username: String, password: String, token: String): login_info = {
    val login_map = Account.login(username = username, password = password, token = token)
    if (login_map("error") != None) {
      throw new Exception(login_map("error").toString())
    }
    login_info(session = login_map("session").toString(),
               instance = login_map("instance").toString())
  }

  // Define Function that will do Insert/Update/Delete
  def put_data(login: login_info, operation: String, sfobject: String, size: Int,
               input_file: String, output_file: String): Unit = {

    // Create Job
    val job = Job.create_job(operation = operation, sfobject = sfobject,
                             session = login.session, instance = login.instance)
    if (job("error") != None) {
      throw new Exception(job("error").toString())
    }

    // Put Data into Batches
    val batches = Batch.batchify(size = size, input = input_file)

    // Add Batches to Job
    for (batch <- batches) {
      val batch_str = batch.mkString("\n")
      val added = Batch.add_batch(batch = batch_str,
                                  job = job("jobid").toString(),
                                  session = login.session,
                                  instance = login.instance)
      if (added("error") != None) throw new Exception(added("error").toString())
    }

    // Close Job
    Job.close_job(job = job("jobid").toString(),
                  session = login.session,
                  instance = login.instance)

    // Wait until all Batches are Done
    var done = false
    while (!done) {
      val batch_map = Batch.check_batchesAll(job = job("jobid").toString(),
                                             session = login.session,
                                             instance = login.instance)
      val processed = batch_map("progress").map(x => x.toInt).sum.toString
      print(s"Processed $processed Entries\r")
      done = batch_map("states").forall(x => x == "Completed" | x == "Failed")
    }
    println("")

    // Get Snapshot of all Batches
    val batch_status = Batch.check_batchesAll(job = job("jobid").toString(),
                                              session = login.session,
                                              instance = login.instance)

    // Retrieve all Results
    val results = batch_status("batches").map(x => Batch.fetch_result(
      batch = x,
      job = job("jobid").toString(),
      session = login.session,
      instance = login.instance)
    )

    // Put Results into CSV
    val result_columns = results(0)._1
    val result_rows = results.flatMap(x => x._2).mkString("\n")
    val result_total = List(result_columns, result_rows).mkString("\n")

    // Save Result to designated Path
    val output = new java.io.File(output_file)
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(output))
    bw.write(result_total)
    bw.close()
  }

  // Define Wrapper Functions for put_data
  def insert(login: login_info, sfobject: String, size: Int,
             input_file: String, output_file: String): Unit = {

    put_data(login = login, operation = "insert", sfobject = sfobject,
             size = size, input_file = input_file, output_file = output_file)

  }

  def update(login: login_info, sfobject: String, size: Int,
             input_file: String, output_file: String): Unit = {

    put_data(login = login, operation = "update", sfobject = sfobject,
             size = size, input_file = input_file, output_file = output_file)

  }

  def delete(login: login_info, sfobject: String, size: Int,
             input_file: String, output_file: String): Unit = {

    put_data(login = login, operation = "delete", sfobject = sfobject,
             size = size, input_file = input_file, output_file = output_file)

  }

  // Define Function to Query Data
  def query(login: login_info, sfobject: String, size: Int,
            query: String, output_file: String): Unit = {

    // Create Job
    val job = Job.create_job(operation = "query", sfobject = sfobject,
                             session = login.session, instance = login.instance,
                             chunksize = size)

    // Insert Query
    val batch = Batch.add_batch(batch = query, job = job("jobid").toString(),
                                session = login.session, instance = login.instance)

    // Parse All Batches but first and stop when all are Completed
    var done = false
    while (!done) {
      val batch_map = Batch.check_batchesAll(job = job("jobid").toString(),
                                             session = login.session,
                                             instance = login.instance)
      val processed = batch_map("progress").map(x => x.toInt).sum.toString
      print(s"Processed $processed Entries\r")
      done = batch_map("states").forall(x => x == "Completed" | x == "Failed" | x == "NotProcessed")
      if (batch_map("states").forall(x => x == "Failed" | x == "NotProcessed")) {
        throw new Exception("Extraction failed, please make sure your SOQL query is correct.")
      }
    }
    println("")


    // Get Snapshot of all Batches
    val batch_status = Batch.check_batchesAll(job = job("jobid").toString(),
                                              session = login.session,
                                              instance = login.instance)

    // Find Batches to Track
    val queries_to_fetch = batch_status("batches").tail

    // Collect Chunks to Retrieve
    val chunk_list = queries_to_fetch.map(x => Batch.fetch_query(batch = x,
                                                                 job = job("jobid").toString(),
                                                                 session = login.session,
                                                                 instance = login.instance
                                                                 )
                                         )

    // Construct final Result
    val cols = chunk_list(0)._1
    val rows = chunk_list.flatMap(x => x._2)
    val result_csv = cols + "\n" + rows.mkString("\n")

    // Close Job
    Job.close_job(job = job("jobid").toString(),
                  session = login.session,
                  instance = login.instance)

    // Save Result to designated Path
    val output = new java.io.File(output_file)
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(output))
    bw.write(result_csv)
    bw.close()
  }
}
