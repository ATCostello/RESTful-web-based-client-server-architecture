import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.io.IOException;
import java.nio.file.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.LinkedList;

import javax.imageio.*;

/**
 * Handles the HTTP requests and splits the headers into chunks
 */
class HTTPRequest {
	RequestType type;
	String resource;
	HTTPHeader headers[];

	String getHeaderValue(String key) {
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].key.equals(key))
				return headers[i].value;
		}

		return null;
	}
}

/**
 * Provides the interface and functions used to create a web client
 */
public class Web {

	static int RESPONSE_OK = 200;
	static int RESPONSE_NOT_FOUND = 404;
	static int RESPONSE_SERVER_ERROR = 501;
	// Create hashmap of the operations
	HashMap<String, Integer> operations = new HashMap<String, Integer>();
	HashMap<String, Integer> operationsJPG = new HashMap<String, Integer>();

	FormMultipart formParser = new FormMultipart();
	IFileServer server;
	String givenName = "";

	/**
	 * Constructor for Web class which connects to the master server and fills the operations hash maps with values
	 */
	public Web() {
		try {
			String name = "masterserver";
			Registry registry = LocateRegistry.getRegistry("localhost");
			server = (IFileServer) registry.lookup(name);
			// Default to off
			operations.put("wordcount", 0);
			operations.put("mostfreqword", 0);
			operations.put("avgwordlength", 0);
			operationsJPG.put("blackandwhite", 0);
			operationsJPG.put("flip", 0);
			operationsJPG.put("colourboost", 0);
			operationsJPG.put("stretch", 0);
		} catch (Exception e) {
			System.err.println("Exception:");
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param output
	 * @param responseCode
	 * @param contentType
	 * @param content
	 */
	private void sendResponse(OutputStream output, int responseCode, String contentType, byte content[]) {
		try {
			output.write(new String("HTTP/1.1 " + responseCode + "\r\n").getBytes());
			output.write("Server: Kitten Server\r\n".getBytes());
			if (content != null)
				output.write(new String("Content-length: " + content.length + "\r\n").getBytes());
			if (contentType != null)
				output.write(new String("Content-type: " + contentType + "\r\n").getBytes());
			output.write(new String("Connection: close\r\n").getBytes());
			output.write(new String("\r\n").getBytes());

			if (content != null)
				output.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * File upload page for txt files html and response.
	 * @param output The output stream handed to the sendResponse function
	 */
	void page_index(OutputStream output) {
		String response = "";
		response = response + "<html>";
		response = response + "<body>";
		response = response + "<h1> File Upload (TXT)</h1>";
		response = response + "<form action=\"/download\" method=\"GET\" enctype=\"multipart/form-data\">";
		response = response + "<input type=\"submit\" value=\"Swap to JPG\" name=goToJPG/><br>";
		response = response + "</form>";
		response = response + "<form action=\"/upload_do\" method=\"POST\" enctype=\"multipart/form-data\">";
		response = response + "<input type=\"text\" name=\"name\" placeholder=\"File name\"  required/>";
		response = response + "<input type=\"file\" name=\"content\" accept=\".txt\" required/>";
		response = response + "<input type=\"submit\" name=\"submit\"/><br><br>";
		response = response + "<Text> Select operations to perform on your TXT file </Text><br>";
		response = response
				+ "<input type=\"checkbox\" id=\"wordcount\" name=\"wordcount\" value=\"wordcount\" unchecked>";
		response = response + "<label for=\"x\">Word Count</label><br>";
		response = response
				+ "<input type=\"checkbox\" id=\"mostfreqword\" name=\"mostfreqword\" value=\"mostfreqword\" unchecked>";
		response = response + "<label for=\"x\">Most Frequent Word</label><br>";
		response = response
				+ "<input type=\"checkbox\" id=\"avgwordlength\" name=\"avgwordlength\" value=\"avgwordlength\" unchecked>";
		response = response + "<label for=\"x\">Average Word Length</label><br>";
		response = response + "</form>";
		response = response + "</body>";
		response = response + "</html>";

		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}

	/**
	 * File upload page for jpg files html and response.
	 * @param output The output stream handed to the sendResponse function
	 */
	void page_index_jpg(OutputStream output) {
		String response = "";
		response = response + "<html>";
		response = response + "<body>";
		response = response + "<h1> File Upload (JPG)</h1>";
		response = response + "<form action=\"/download\" method=\"GET\" enctype=\"multipart/form-data\">";
		response = response + "<input type=\"submit\" value=\"Swap to TXT\" name=goToTxt/><br>";
		response = response + "</form>";
		response = response + "<form action=\"/upload_do\" method=\"POST\" enctype=\"multipart/form-data\">";
		response = response + "<input type=\"text\" name=\"name\" placeholder=\"File name\" required/>";
		response = response + "<input type=\"file\" name=\"content\" accept=\".jpg\" required/>";
		response = response + "<input type=\"submit\" name=\"submit\"/><br><br>";
		response = response + "<Text> Select operations to perform on your JPG file </Text><br>";
		response = response
				+ "<input type=\"checkbox\" id=\"blackandwhite\" name=\"blackandwhite\" value=\"blackandwhite\" unchecked>";
		response = response + "<label for=\"x\">Black and White</label><br>";
		response = response + "<input type=\"checkbox\" id=\"flip\" name=\"flip\" value=\"flip\" unchecked>";
		response = response + "<label for=\"x\">Flip</label><br>";
		response = response
				+ "<input type=\"checkbox\" id=\"colourboost\" name=\"colourboost\" value=\"colourboost\" unchecked>";
		response = response + "<label for=\"x\">Colour Boost</label><br>";
		response = response + "<input type=\"checkbox\" id=\"stretch\" name=\"stretch\" value=\"stretch\" unchecked>";
		response = response + "<label for=\"x\">Stretch</label><br>";
		response = response + "</form>";
		response = response + "</body>";
		response = response + "</html>";

		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}

	/**
	 * Maps GET requests onto functions which returns HTML pages
	 * @param request The http request data
	 * @param output The output stream given to the sendResponse function
	 */
	void get(HTTPRequest request, OutputStream output) {
		// System.out.println(request.resource);
		// Download Request Received
		if (request.resource.contains("2F=Swap+to+TXT"))
			page_index(output);
		if (request.resource.contains("2F=Swap+to+JPG"))
			page_index_jpg(output);
		if (request.resource.contains("2F=raw"))
			download(request, output);
		if (request.resource.contains("2F=xml"))
			viewXML(request, output);
		if (request.resource.equals("/"))
			page_index(output);
		else if (request.resource.equals("/files"))
			listFiles(output);
		else
			sendResponse(output, RESPONSE_NOT_FOUND, null, null);

		// System.out.println("\nPRINTING HEADER CONTENTS");
		// for (int i = 0; i < request.headers.length; i++)
		// System.out.println(request.headers[i].value);
		// System.out.println("\n");
	}

	// this function maps POST requests onto functions / code which return HTML
	// pages
	/**
	 * Maps POST requests onto functions which return HTML data
	 * Sends data to the server for operation
	 * @param request The http request data
	 * @param payload 
	 * @param output The output stream given to the sendResponse function
	 */
	void post(HTTPRequest request, byte payload[], OutputStream output) {
		// Upload Request Received
		if (request.resource.equals("/upload_do")) {
			System.out.println("upload do");
			// FormMultipart
			if (request.getHeaderValue("content-type") != null
					&& request.getHeaderValue("content-type").startsWith("multipart/form-data")) {
				FormData data = formParser.getFormData(request.getHeaderValue("content-type"), payload);
				String filename = "";
				// Convert contents from byte array to string
				String s = new String(data.fields[1].content, StandardCharsets.UTF_8);

				for (int i = 0; i < data.fields.length; i++) {
					System.out.println(data.fields[i].name.toString());

					if (data.fields[i].name.equals("content")) {
						// System.out.println(" -- filename: " + ((FileFormField)
						// data.fields[i]).filename);
						filename = ((FileFormField) data.fields[i]).filename;
						// System.out.println(" -- contents: " + ((FileFormField)
						// data.fields[i]).content.toString());
					} else if (data.fields[i].name.equals("wordcount")) {
						operations.put("wordcount", 1);
					} else if (data.fields[i].name.equals("mostfreqword")) {
						operations.put("mostfreqword", 1);
					} else if (data.fields[i].name.equals("avgwordlength")) {
						operations.put("avgwordlength", 1);
					} else if (data.fields[i].name.equals("blackandwhite")) {
						operationsJPG.put("blackandwhite", 1);
					} else if (data.fields[i].name.equals("flip")) {
						operationsJPG.put("flip", 1);
					} else if (data.fields[i].name.equals("colourboost")) {
						operationsJPG.put("colourboost", 1);
					} else if (data.fields[i].name.equals("stretch")) {
						operationsJPG.put("stretch", 1);
					} else if (data.fields[i].name.equals("name")) {
						givenName = new String(data.fields[i].content, StandardCharsets.UTF_8);
						// System.out.println("given name is " + givenName);
					}
				}

				if (filename.contains(".txt")) {
					// System.out.println("op is " + operations);
					try {
						server.uploadFile(givenName + ".txt", s.getBytes());
					} catch (Exception e) {
						e.printStackTrace();
					}
					try {
						server.operate(givenName + ".txt", s.getBytes(), operations);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (filename.contains(".jpg")) {
					try {
						server.uploadFile(givenName + ".jpg", data.fields[1].content);
						// WRITE BUFFER CONTENTS TO JPG FILE (WORKS)
						// ByteArrayInputStream bais = new ByteArrayInputStream(data.fields[1].content);
						// BufferedImage image = ImageIO.read(bais);
						// File outputfile = new File("./downloads/saved.jpg");
						// ImageIO.write(image, "jpg", outputfile);
						try {
							server.operateJPG(givenName + ".jpg", data.fields[1].content, operationsJPG);
						} catch (Exception e) {
							e.printStackTrace();
						}
						try {
							ByteArrayInputStream bais = new ByteArrayInputStream(server.get(givenName + ".jpg"));
							BufferedImage image = ImageIO.read(bais);
							File outputfile = new File("./downloads/" + givenName + ".jpg");
							ImageIO.write(image, "jpg", outputfile);
							server.uploadFile(givenName + ".jpg", server.get(givenName + ".jpg"));
						} catch (Exception e) {
							e.printStackTrace();
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				operations.put("wordcount", 0);
				operations.put("mostfreqword", 0);
				operations.put("avgwordlength", 0);
				operationsJPG.put("blackandwhite", 0);
				operationsJPG.put("flip", 0);
				operationsJPG.put("colourboost", 0);
				operationsJPG.put("stretch", 0);

				// Send response with redirection to home screen after 2 seconds
				String response = "";

				if (filename.contains(".txt")) {
					response = response + "<meta http-equiv=\"refresh\" content=\"5; URL=http://localhost:8080/download?goToTxt%2F=Swap+to+TXT\" />";
					response = response + "<html>File sent, thanks! <br> Returning to upload screen.</html>";
				} else if (filename.contains(".jpg")) {
					response = response + "<meta http-equiv=\"refresh\" content=\"5; URL=http://localhost:8080/download?goToJPG%2F=Swap+to+JPG\" />";
					response = response + "<html>File sent, thanks! <br> Returning to upload screen.</html>";
				}
				sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
			} else {
				sendResponse(output, RESPONSE_SERVER_ERROR, null, null);
			}
		}
	}

	/**
	 * Sends a download reqest to the server to download a specified file
	 * @param request The http request data
	 * @param output The output stream given to the sendResponse function
	 */
	public void download(HTTPRequest request, OutputStream output) {
		System.out.println("Download received");

		// Split the request to grab the filename which should be downloaded
		String[] splitResource1 = request.resource.split("\\?", 0);
		String[] splitResource2 = splitResource1[1].split("\\%", 0);
		String filename = splitResource2[0];

		if (filename.contains(".txt")) {
			// write to file
			try {
				String contents = new String(server.downloadFile(filename), StandardCharsets.UTF_8);

				// create file
				try {
					File newFile = new File("./downloads/" + filename);
					if (newFile.createNewFile()) {
						// System.out.println("File created: " + newFile.getName());
					} else {
						// System.out.println("File already exists.");
					}
				} catch (IOException e) {
					// System.out.println("An error occurred.");
					e.printStackTrace();
				}

				// write to file
				try {
					FileWriter myWriter = new FileWriter("./downloads/" + filename);
					myWriter.write(contents);
					myWriter.close();
					// System.out.println("Successfully wrote to the file.");

				} catch (IOException e) {
					// System.out.println("An error occurred.");
					e.printStackTrace();
				}
			} catch (IOException e) {
				// System.out.println("An error occurred.");
				e.printStackTrace();
			}
		} else {
			try {
				ByteArrayInputStream bais = new ByteArrayInputStream(server.downloadFile(filename));
				BufferedImage image = ImageIO.read(bais);
				File outputfile = new File("./downloads/" + filename);
				ImageIO.write(image, "jpg", outputfile);
			} catch (IOException e) {
				// System.out.println("An error occurred.");
				e.printStackTrace();
			}
		}
		// Send response with redirection to home screen after 2 seconds
		String response = "";
		response = response + "<meta http-equiv=\"refresh\" content=\"5; URL=http://localhost:8080/files\" />";
		response = response + "<html>File has been downloaded, thanks! <br> Returning to files screen.</html>";

		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}

	/**
	 * Sends an XML download reqest to the server to download a specified file in XML format
	 * @param request The http request data
	 * @param output The output stream given to the sendResponse function
	 */
	public void viewXML(HTTPRequest request, OutputStream output) {
		System.out.println("XML Request Received");

		// Split the request to grab the filename
		String[] splitResource1 = request.resource.split("\\?", 0);
		String[] splitResource2 = splitResource1[1].split("\\%", 0);

		String filename = "";
		try {
			// Get filename
			String[] splitfilename = splitResource2[0].split(".txt");
			filename = splitfilename[0];

			try {
				String s = new String(server.downloadFile(filename + ".txt"), StandardCharsets.UTF_8);
			} catch (Exception e) {
			}

			// Get file operations from node storage
			String content = new String(server.get(filename + ".txt"), StandardCharsets.UTF_8);

			// Create xml with the information
			String[] splitContent = content.split("/");
			String xml = "<?xml version=\"1.0\"?>\n";
			xml = xml + "<" + filename + ".xml" + ">\n";
			xml = xml + "<wordcount>" + splitContent[0] + "</wordcount>\n";
			xml = xml + "<freqword>" + splitContent[1] + "</freqword>\n";
			xml = xml + "<avgwordlen>" + splitContent[2] + "</avgwordlen>\n";
			xml = xml + "</" + filename + ".xml" + ">\n";

			// create file
			try {
				File newFile = new File("./downloads/" + filename + ".xml");
				if (newFile.createNewFile()) {
					// System.out.println("File created: " + newFile.getName());
				} else {
					// System.out.println("File already exists.");
				}
			} catch (IOException e) {
				// System.out.println("An error occurred.");
				e.printStackTrace();
			}

			// write to file
			try {
				FileWriter myWriter = new FileWriter("./downloads/" + filename + ".xml");
				myWriter.write(xml);
				myWriter.close();
				// System.out.println("Successfully wrote to the file.");

			} catch (IOException e) {
				// System.out.println("An error occurred.");
				e.printStackTrace();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Send response with redirection to home screen after 2 seconds
		String response = "";
		response = response + "<meta http-equiv=\"refresh\" content=\"5; URL=http://localhost:8080/files\" />";
		response = response + "<html>XML File has been downloaded, thanks! <br> Returning to files screen.</html>";

		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}

	/**
	 * Sends a getFiles() request to the server to retrive a list of all currently uploaded files on the system
	 * @param output The output stream given to the sendResponse function
	 */
	public void listFiles(OutputStream output) {

		String html = "";

		try {
			File[] files = server.getFiles();

			html = html + "<html>";
			html = html + "<h1> File List </h1>";
			html = html + "<body>";
			html = html + "<h5>FILES    |    DOWNLOAD OPTIONS</h5>";
			html = html + "<form action=\"/download\" method=\"GET\" enctype=\"multipart/form-data\">";

			for (int i = 0; i < files.length; i++) {
				String filename = files[i].getName();
				// System.out.println(filename);
				html = html + "<br><text>" + filename + "                                         </text>";
				html = html + "<input type=\"submit\" value=\"raw\" name=" + filename + "/>";
				if (filename.contains(".txt")) {
					html = html + "<input type=\"submit\" value=\"xml\" name=" + filename + "/>";
				}
			}

			html = html + "</form>";
			html = html + "</body>";
			html = html + "</html>";
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		sendResponse(output, RESPONSE_OK, "text/html", html.getBytes());
	}

}