# mysql-sqlite-synchronization
Android checks the new data at the server, if there are some new data, data will be pulled to android and will be inserted into sqlite.  The origin code comes from http://programmerguru.com/android-tutorial/how-to-sync-remote-mysql-db-to-sqlite-on-android/, thank you for programmerguru :+1: .  I modified some code to be adjusted with the newer version of android (with Android Studio IDE), furthermore, I used two libraries: 

1. android-async-http-1.4.9 
2. gson-2.2.4

The newer android-async-http is different with the former.  It requires four parameters when using onSuccess and on Failure method.  The one of parameters is errorResponse, the old library used one parameter with String datatype for errorResponse, whereas the new library requires byte[] datatype for errorResponse.
