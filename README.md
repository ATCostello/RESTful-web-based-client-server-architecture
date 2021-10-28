# RESTful web-based client server architecture
 A web-based client-server architecture utilising chord nodes, remote method invocation and distributed hash tables. 
 A system was made which utilises a REST service, DHT and RMI in order to provide a way for users to connect to the REST service and upload a file which will have tasks completed on it. The users will then be able to retrieve the results at any time from the system.
My solution uses a Chord network DHT with each chord node being an individual “worker node” that executes and stores the outputs of the operations for later retrieval. These nodes all communicate with a “Server” element, which acts as a middle ground and load balancer for the chord network and the REST service. The REST Service allows users to perform tasks on two types of files – txt files and jpg files. The users are also allowed to select which tasks they would like to perform on these files and can retrieve the results at any time.

## How to utilise my system
Open three separate command lines, and navigate two to the “Server” folder, and one to the “Client” folder.
Within the “Server” command lines, run the following command in the first command line:
>rmiregistry

Now within the second “Server” command line, run the following:
>java Server

And finally, once the Server responds with “Server masterserver ready” - within the “Client” command line, run the following:
>java RestKit

Now the client and server applications are running, go to your web browser and navigate to the following URLs:
>localhost:8080
This is the main text “File Upload” page for the system. You can select a file to upload, give it a name, and select which tasks you would like to perform. To perform tasks on a jpg file, use the “Swap to JPG” button on the top of the page.

>localhost:8080/files
This page displays a list of the files currently in the system and allows users to retrieve the files in a different format. The downloaded files are stored in the “./Client/downloads” folder. “raw” downloads the files in their original, processed formats, “xml” allows the download of the results of the text tasks as an xml file format. 

 >Written in Java with in-line HTML and CSS.
