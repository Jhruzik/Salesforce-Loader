package com.github.jhruzik.salesforceloader

object Main extends App {

  // Parse Command Line Arguments

  // Define Function to Check and Extract Argument
  def check_var(arg_letter: String, error_msg: String): String = {
    if (!args.contains(s"-$arg_letter")) {
      throw new IllegalArgumentException(error_msg)
    } else {
      val index_var = args.indexOf(s"-$arg_letter") + 1
      args(index_var)
    }
  }

  // Define Operations for Update etc. Route
  val operations_change = List("insert", "update", "delete")

  // User Name
  val username = check_var(arg_letter = "u", error_msg = "Please specify a username")
  // Password
  val password = check_var(arg_letter = "p", error_msg = "Please specify a password")
  // Security Token
  val token = check_var(arg_letter = "t", error_msg = "Please specify a security token")
  // SalesForce Object
  val sfobject = check_var(arg_letter = "o", error_msg = "Please specify a SalesForce Object")
  // Command
  val operation = check_var(arg_letter = "c", error_msg = "Please give an operation").toLowerCase()
  // Batch/Chunk Size
  val size = check_var(arg_letter = "s", error_msg = "Please give a Batch/Chunk Size").toInt
  // Output File
  val file_result = check_var(arg_letter = "r", error_msg = "Please give a path for the result file")
  // Input File
  val file_input = if (operations_change.contains(operation)) {
    check_var(arg_letter = "i", error_msg = "Please give a path to an input file")
  } else ""
  // Query
  val query = if (operation == "query") {
    check_var(arg_letter = "q", error_msg = "Please give a SOQL query")
  } else ""

  // Log User into SalesForce
  val login = Loader.login(username = username, password = password, token = token)

  // Run Command
  operation match {
    case "insert" => Loader.insert(login = login, sfobject = sfobject, size = size,
                                   input_file = file_input, output_file = file_result)
    case "update" => Loader.update(login = login, sfobject = sfobject, size = size,
                                   input_file = file_input, output_file = file_result)
    case "delete" => Loader.delete(login = login, sfobject = sfobject, size = size,
                                   input_file = file_input, output_file = file_result)
    case "query" => Loader.query(login = login, sfobject = sfobject, size = size,
                                 query = query, output_file = file_result)
  }


}
