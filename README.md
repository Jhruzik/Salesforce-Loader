# SalesForce-Loader
SalesForce-Loader is a tool to update, insert, delete, and query data within the SalesForce Data Cloud. It can either be used as a CLI tool or within your own scripts. It is primarily designed to deal with bulk data. Hence, only the Bulk API is used. This will keep the necessary amount of API calls to a minimum.

## Prerequisites
You should have a recent version of Scala installed on your machine. The current release was built with version 2.13.1.
Also salesforce-loader depends on the following packages:
* [requests](https://github.com/lihaoyi/requests-scala)
* [scala-xml](https://github.com/scala/scala-xml)

## Usage [CLI]
If you want to use SalesForce-Loader's command line interface, you should download the most recent release. You can simply execute the jar file with some parameters: 
* -u [username: This should be the username you use to access your SalesForce instance.]
* -p [password: The password associated with your account.]
* -t [token: The security token associated with your account.]
* -o [object: The SalesForce object you want to operate on.]
* -c [command: The command you want to execute. Either one of insert, update, delete, or query.]
* -s [size: This is equal to the batch size if you choose to insert, update, or delete. It is equal to the chunk size if you choose to query.]
* -r [result file: The path where the result file will be written to.]
* -i [input file: The path of your input data (only needed for update, delete, and insert commands).]
* -q [query: The SOQL query you would like to execute (only needed for query commands).]

Let's assume that our jar file is located at /home/user/salesforce-loader-0.1.jar. We want to upload some leads in /home/user/lead_upload.csv and save the result under /home/user/result.csv. Our username is "doe@salesforce.com" and our password is "password123". Also, we have generated a security token equal to "token$!123". We decide to use a bulk size of 500. We can start the insert with the following command:
```scala /home/user/salesforce-loader-0.1.jar -u doe@salesforce.com -p password123 -t token$!123 -o Lead -c insert -s 500 -r /home/user/result.csv -i /home/user/lead_upload.csv```
Delete and update commands would look very similiar (the -c argument would be different).

We could also decide to load all accounts to /home/user/accounts.csv in batches of 5000:
```scala /home/user/salesforce-loader-0.1.jar -u doe@salesforce.com -p password123 -t token$!123 -o Account -c query -s 5000 -r /home/user/account.csv```

While running, you will get feedback on how many rows have already been processed.

## Usage [Script]
You can also use SalesForce-Loader within your scripts. There is a simple wrapper function for every workload. The wrapper functions can be accessed by running `import com.github.jhruzik.salesforceloader.Loader._`. The following commands will refer to functions within that package.

#### Login
Every action needs some login information to process data on the SalesForce Data Cloud. You can login and save your login information for later use like so:
```val login_cred = login(username = "username", password = "password", token = "token")```
Of course, username, password, and token should correspond to the values relevant for your account.

#### Insert
Assume that you want to insert lead data located at /home/user/lead.csv and that you want to save the results to /home/user/lead_result.csv.
```ìnsert(login=login_cred, sfobject = "Lead", size = 5000, input_file = "/home/user/lead.csv", ouput_file = "/home/user/lead_result.csv")```

#### Update
In this scenario we have some account data we would like to update. The input data is located at /home/user/account_update.csv and the result is to be saved under /home/user/update_result.csv.
```update(login=login_cred, sfobject = "Account", size = 5000, input_file = "/home/user/account_update.csv", output_file = "/home/user/update_result.csv")```

#### Delete
You can also delete data. For example, assume that we would like to delete Cases in /home/user/case_delete.csv and want to save the result under /home/user/delete_result.csv.
```delete(login = login_cred, sfobject = "Case", size = 5000, input_file = "/home/user/case_delete.csv, output_file = "/home/user/delete_result.csv")```

#### Query
If you would like to execute the SOQL query `SELECT FirstName,LastName FROM Lead` and save the result under /home/user/lead.csv, use the query function.
```query(login=login_cred, sfobject = "Lead", size = 5000, query = "SELECT FirstName,LastName FROM Lead", output_file = "/home/user/lead.csv")```

