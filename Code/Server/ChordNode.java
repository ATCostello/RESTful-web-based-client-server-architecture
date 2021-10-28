
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.*;
import java.awt.*;

class Finger {
	public int key;
	public ChordNode node;
}

class Store {
	String key;
	byte[] value;
}

/**
 * A single chord node and all the functions a node can perform.
 */
public class ChordNode implements Runnable {

	// m (number of bits in the hash key)
	static final int KEY_BITS = 8;

	ChordNode successorNode;
	int successorKey;

	ChordNode predecessor;
	int predecessorKey;

	// my finger table
	int fingerTableLength;
	Finger finger[];
	int nextFingerFix;
	int next = -1;

	// The data storage for this node
	HashMap<String, byte[]> dataStore = new HashMap<String, byte[]>();

	private int myKey;

	private int counter = 0;

	/**
	 * Constructor for the chord node which will generate the key, initial successor
	 * node and finger table.
	 * 
	 * @param myKeyString Name of the chord node
	 */
	ChordNode(String myKeyString) {

		myKey = hash(myKeyString);
		// System.out.println("--- Creating Chord Node with name " + myKeyString + " and
		// key " + myKey);

		successorNode = this;
		successorKey = myKey;
		// System.out.println("--- successorNode is " + successorNode);
		// System.out.println("--- successorKey is " + successorKey);

		// initialise finger table (note all "node" links will be null!)
		finger = new Finger[KEY_BITS];
		for (int i = 0; i < KEY_BITS; i++)
			finger[i] = new Finger();
		fingerTableLength = KEY_BITS;

		// start up the periodic maintenance thread
		new Thread(this).start();
	}

	// -- API functions --

	/**
	 * Finds the correct node in the system to store the provided data and stores it
	 * in that node's storage
	 * 
	 * @param key   Name of the data to be stored
	 * @param value byte[] contents to be stored
	 */
	void put(String key, byte[] value) {
		// find the node that should hold this key and add the key and value to that
		// node's local store
		// System.out.println("--- Running 'put' with key: " + key);

		// Hash key, find first node with higher keyvalue and store there
		findSuccessor(hash(key)).dataStore.put(key, value);

		// System.out.println("--- put " + key + "'s value with key " + hash(key) + " in
		// " + findSuccessor(hash(key)).getKey());
		// dataStore.put(key, value);
	}

	/**
	 * All of the operate functions used to perform tasks on a text file
	 * 
	 * @param key        Name of the file to operate on
	 * @param value      byte[] contents of the file to work on
	 * @param operations Hash map containing all of the operations to be performed
	 */
	void operate(String key, byte[] value, HashMap<String, Integer> operations) {

		// Convert byte array to string
		String content = new String(value, StandardCharsets.UTF_8);
		// System.out.println("content is : " + content);

		// Seperate contents into seperate words array
		// s+ means at least one space
		String[] words = content.split("\\s+");
		String output = "";

		if (operations.get("wordcount") == 1) {
			// System.out.println("count");
			// Calculate total words
			int wordCount = words.length;
			output = Integer.toString(wordCount) + "/";
			// System.out.println("wordCount is : " + wordCount);
		} else {
			output = output + "null/";
		}

		if (operations.get("mostfreqword") == 1) {
			// System.out.println("freq");
			// Calculate most frequent word
			// Create hashmap which holds all words and associated counter
			HashMap<String, Integer> wordsHashMap = new HashMap<String, Integer>();

			// Go through words array and add each unique word to hashmap, increase counter
			// for each occurence.
			for (String s : words) {

				if (!wordsHashMap.containsKey(s)) {
					wordsHashMap.put(s, 1);
				} else {
					wordsHashMap.put(s, wordsHashMap.get(s) + 1);
				}
			}

			// Go through each entry and find the one with the highest value and set that as
			// "maxEntry"
			HashMap.Entry<String, Integer> maxEntry = null;
			for (HashMap.Entry<String, Integer> entry : wordsHashMap.entrySet()) {
				if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
					maxEntry = entry;
				}
			}
			output = output + maxEntry.getKey() + "/";
			// System.out.println("Most Frequent Word is: " + maxEntry.getKey() + " with a
			// count of " + maxEntry.getValue());
		} else {
			output = output + "null/";
		}

		if (operations.get("avgwordlength") == 1) {
			// System.out.println("avg");
			// Calculate average word length (total length of words/number of words)

			// calculate size of each word and store in new array
			LinkedList<Integer> wordLength = new LinkedList<Integer>();
			for (int i = 0; i < words.length; i++) {
				wordLength.add(words[i].length());
			}
			int totalLength = 0;
			for (int i = 0; i < wordLength.size(); i++) {
				totalLength = totalLength + wordLength.get(i);
			}

			Double averageWordLength = (double) totalLength / words.length;
			;

			output = output + averageWordLength.toString() + "/";
			// System.out.println("Average word length is " + averageWordLength + "
			// characters");
		} else {
			output = output + "null/";
		}

		// System.out.println(output);
		//
		// convert data to a single string, then put into a byte[] array and store in
		// node's storage
		findSuccessor(hash(key)).dataStore.put(key, output.getBytes());
	}

	/**
	 * Black and white operation for jpg files
	 * 
	 * @param key           Name of the file to operate on
	 * @param value         byte[] contents of the file to work on
	 * @param operationsJPG Hash map containing all of the operations to be
	 *                      performed
	 */
	void operatebwJPG(String key, byte[] value, HashMap<String, Integer> operationsJPG) {

		// System.out.println("blackandwhite");
		// Turn data into a byte array input stream for ImageIO manipulation
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		// Create empty buffered image
		BufferedImage image = null;
		// Read the byte array input stream and generate the image data
		try {
			image = ImageIO.read(bais);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Check that black and white is selected then,
		// For each pixel in the image, generate a corresponding greyscale value
		// Set each pixel to the greyscale value
		if (operationsJPG.get("blackandwhite").equals(1)) {
			for (int x = 0; x < image.getHeight(); x++) {
				for (int y = 0; y < image.getWidth(); y++) {
					Color c = new Color(image.getRGB(y, x));
					int r = (int) (c.getRed() * 0.299);
					int g = (int) (c.getGreen() * 0.587);
					int b = (int) (c.getBlue() * 0.114);
					Color newColor = new Color(r + g + b, r + g + b, r + g + b);
					image.setRGB(y, x, newColor.getRGB());
				}
			}
		}

		// Create a byte array output stream
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Try to create a new image with the new greyscale data
		try {
			ImageIO.write(image, "jpg", baos);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Turn the greyscale image data into a byte array for storage
		byte[] bytes = baos.toByteArray();
		// convert data to a single string, then put into a byte[] array and store in
		// node's storage
		findSuccessor(hash(key)).dataStore.put(key, bytes);

	}

	/**
	 * Flip operation for jpg files
	 * 
	 * @param key           Name of the file to operate on
	 * @param value         byte[] contents of the file to work on
	 * @param operationsJPG Hash map containing all of the operations to be
	 *                      performed
	 */
	void operatefpJPG(String key, byte[] value, HashMap<String, Integer> operationsJPG) {

		// System.out.println("flip");
		// Turn data into a byte array input stream for ImageIO manipulation
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		// Create empty buffered image
		BufferedImage image = null;
		// Read the byte array input stream and generate the image data
		try {
			image = ImageIO.read(bais);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Check that the flip operation was selected
		// For each pixel in the image, find the height and invert it.
		if (operationsJPG.get("flip").equals(1)) {
			for (int x = 0; x < image.getWidth(); x++)
				for (int y = 0; y < image.getHeight() / 2; y++) {
					int tmp = image.getRGB(x, y);
					image.setRGB(x, y, image.getRGB(x, image.getHeight() - y - 1));
					image.setRGB(x, image.getHeight() - y - 1, tmp);
				}
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Write new image data
		try {
			ImageIO.write(image, "jpg", baos);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Turn new image data into a byte array for storage
		byte[] bytes = baos.toByteArray();
		// convert data to a single string, then put into a byte[] array and store in
		// node's storage
		findSuccessor(hash(key)).dataStore.put(key, bytes);

	}

	/**
	 * Colour boost operation for jpg files
	 * 
	 * @param key           Name of the file to operate on
	 * @param value         byte[] contents of the file to work on
	 * @param operationsJPG Hash map containing all of the operations to be
	 *                      performed
	 */
	void operatecbJPG(String key, byte[] value, HashMap<String, Integer> operationsJPG) {

		// System.out.println("colourboost");
		// Turn data into a byte array input stream
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		// Create empty buffered image
		BufferedImage image = null;
		// Read image data
		try {
			image = ImageIO.read(bais);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Check that the colourboost operation is selected
		// For each pixel in the image,
		// get the original colour values and convert them to HSB format for saturation
		// boosting
		// Set the HSB values (Hue, Saturation, Brightness) and increase the saturation
		// by 1.5x
		// Convert the HSB values back to RGB values
		// Write the RGB data back to the image
		if (operationsJPG.get("colourboost").equals(1)) {
			for (int x = 0; x < image.getHeight(); x++) {
				for (int y = 0; y < image.getWidth(); y++) {
					Color c = new Color(image.getRGB(y, x));
					int r = c.getRed();
					int g = c.getGreen();
					int b = c.getBlue();
					float[] hsb = Color.RGBtoHSB(r, g, b, null);

					float hue = hsb[0];
					float saturation = hsb[1] * (float) 1.5;
					float brightness = hsb[2];

					int rgb = Color.HSBtoRGB(hue, saturation, brightness);

					r = (rgb >> 16) & 0xFF;
					g = (rgb >> 8) & 0xFF;
					b = rgb & 0xFF;

					Color newColor = new Color(r, g, b);
					image.setRGB(y, x, newColor.getRGB());
				}
			}
		}

		// Output stream of new colour data
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Write the data
		try {
			ImageIO.write(image, "jpg", baos);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Convert the new image data to a byte array for storage
		byte[] bytes = baos.toByteArray();

		// convert data to a single string, then put into a byte[] array and store in
		// node's storage
		findSuccessor(hash(key)).dataStore.put(key, bytes);
	}

	/**
	 * Stretch operation for jpg files
	 * 
	 * @param key           Name of the file to operate on
	 * @param value         byte[] contents of the file to work on
	 * @param operationsJPG Hash map containing all of the operations to be
	 *                      performed
	 */
	void operatestJPG(String key, byte[] value, HashMap<String, Integer> operationsJPG) {

		// System.out.println("stretch");
		// Convert the provided data to a byte array input stream for image manipulation
		ByteArrayInputStream bais = new ByteArrayInputStream(value);
		// Create an empty buffered image
		BufferedImage image = null;
		// Convert the data to the image for ImageIO
		try {
			image = ImageIO.read(bais);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Create a new output buffered image, which is the dimensions of the original, just 3x the width
		BufferedImage imgout = new BufferedImage(image.getWidth() * 3, image.getHeight(), image.getType());

		// Check that the stretch operation is selected
		// Then turn the image into a 2d graphic, and draw the new image to the new stretched dimensions
		if (operationsJPG.get("stretch").equals(1)) {
			Graphics2D g2d = imgout.createGraphics();
			g2d.drawImage(image, 0, 0, image.getWidth() * 3, image.getHeight(), null);
			g2d.dispose();
		}

		// Output stream for the image
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Write the image data 
		try {
			ImageIO.write(imgout, "jpg", baos);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Convert the image data to a byte array for storage
		byte[] bytes = baos.toByteArray();

		// convert data to a single string, then put into a byte[] array and store in
		// node's storage
		findSuccessor(hash(key)).dataStore.put(key, bytes);
	}

	/**
	 * Retrieve the data from the correct node's storage
	 * @param key Name of the data to retrieve
	 * @return byte array of the contents 
	 */
	byte[] get(String key) {
		// find the node that should hold this key, request the corresponding value from
		// that node's local store, and return it
		// System.out.println("--- Running 'get' with key: " + key);

		// if the value is not in this node's storage
		// hash key, find node and search that node's data store
		return findSuccessor(hash(key)).dataStore.get(key);
	}

	// -- state utilities --

	/**
	 * Get the key of the current node
	 * @return Node's key
	 */
	int getKey() {
		return myKey;
	}

	/**
	 * Get the task counter of the current node
	 * @return Node's task counter
	 */
	int getCounter() {
		return counter;
	}

	/**
	 * Increase the counter
	 */
	void addCounter() {
		counter++;
	}

	/**
	 * Decrease the counter
	 */
	void lowerCounter() {
		counter--;
	}

	/**
	 * Get the predecessor of the current node
	 * @return Chord node that is the predecessor of the current node
	 */
	ChordNode getPredecessor() {
		// System.out.println("--- Running 'getPredecessor'");
		if (predecessor != null)
			return predecessor;
		else
			return successorNode;
	}

	// -- topology management functions --

	/**
	 * Join the current node to another node
	 * @param atNode The node to join to
	 */
	void join(ChordNode atNode) {
		predecessor = null;
		predecessorKey = 0;

		// System.out.println("--- Running 'join' with key " + getKey() + " and atNode
		// of " + atNode.getKey());
		successorNode = atNode.findSuccessor(getKey());
		// System.out.println("--- 'join' " + this.getKey() + " points to " +
		// successorNode.getKey());

	}

	// -- utility functions --

	/**
	 * Find the successor node of the provided key
	 * @param key Key for the successor to find
	 * @return Chord node that is the successor of the key
	 */
	ChordNode findSuccessor(int key) {
		// System.out.println("--- Running 'findSuccessor' with key: " + key);
		// System.out.println("--- key is " + key + " getkey is " + getKey() + " succkey
		// is " + successorNode.getKey());
		// System.out.println("--- halfopen: " + isInHalfOpenRangeR(key, getKey(),
		// successorNode.getKey()));
		if (isInHalfOpenRangeR(key, getKey(), successorNode.getKey())) {
			// System.out.println("-- returning successorNode " + successorNode.getKey());
			return successorNode;
		} else {
			ChordNode ntemp = closestPrecedingNode(key);
			if (ntemp == null) {
				// System.out.println("-- it was null");
				ntemp = closestPrecedingNode(key);
			} else {
				// System.out.println("-- ntemp is " + ntemp);
				return ntemp.findSuccessor(key);
			}
			// System.out.println("-- got here D:");
			return this;
		}
	}

	/**
	 * Find the closes preceding node of the provided key
	 * @param key Key to find the closest predecessor
	 * @return Chord node that is the closest predecessor of the key
	 */
	ChordNode closestPrecedingNode(int key) {
		// System.out.println("--- Running 'closestPrecedingNode' with key: " + key);
		for (int i = KEY_BITS - 1; i > 1; i--) {
			// System.out.println("--- i'm here " + i);
			if (isInClosedRange(finger[i].key, getKey(), key)) {
				// System.out.println("--- i'm here 2 " + i);
				return finger[i].node;
			}
		}
		return this;
	}

	// -- range check functions; they deal with the added complexity of range wraps
	// --
	// x is in [a,b] ?
	boolean isInOpenRange(int key, int a, int b) {
		if (b > a)
			return key >= a && key <= b;
		else
			return key >= a || key <= b;
	}

	// x is in (a,b) ?
	boolean isInClosedRange(int key, int a, int b) {
		if (b > a)
			return key > a && key < b;
		else
			return key > a || key < b;
	}

	// x is in [a,b) ?
	boolean isInHalfOpenRangeL(int key, int a, int b) {
		if (b > a)
			return key >= a && key < b;
		else
			return key >= a || key < b;
	}

	// x is in (a,b] ?
	boolean isInHalfOpenRangeR(int key, int a, int b) {
		if (b > a)
			return key > a && key <= b;
		else
			return key > a || key <= b;
	}

	// -- hash functions --
	// this function converts a string "s" to a key that can be used with the DHT's
	// API functions
	int hash(String s) {
		int hash = 0;

		for (int i = 0; i < s.length(); i++)
			hash = hash * 31 + (int) s.charAt(i);

		if (hash < 0)
			hash = hash * -1;

		return hash % ((int) Math.pow(2, KEY_BITS));
	}

	// -- maintenance --
	void notifyNode(ChordNode potentialPredecessor) {
		// System.out.println("--- Running 'notifyNode'");
		// if(isInClosedRange(potentialPredecessor.getKey(), getPredecessor().getKey(),
		// getKey()) || getPredecessor() == null){
		// predecessor = potentialPredecessor;
		// }
	}

	void stabilise() {
		// System.out.println("--- Running 'stabilise'");
		// if(isInClosedRange(successorNode.getPredecessor().getKey(), getKey(),
		// successorKey)){
		// successorNode = successorNode.getPredecessor();
		// }
		// successorNode.notifyNode(this);
	}

	/**
	 * Create the finger table for each key with the correct node data
	 */
	void fixFingers() {
		// System.out.println("--- Running 'fixFingers'");
		next = next + 1;
		if (next > KEY_BITS - 1) {
			next = 0;
		}
		int fingerEntry = this.getKey() + (int) Math.pow(2, next);
		// System.out.println(this.getKey() + " points to " + fingerEntry);
		finger[next].node = findSuccessor(fingerEntry);
		finger[next].key = fingerEntry;
	}

	void checkPredecessor() {
		// System.out.println("--- Running 'checkPredecessor'");
	}

	void checkDataMoveDown() {
		// if I'm storing data that my current predecessor should be holding, move it
		// System.out.println("--- Running 'checkDataMoveDown'");

		// Check storage, hash all key strings, check that the successor is this node,
		// if not then remove from this node's storage and place in the correct storage
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Interrupted");
			}

			try {
				stabilise();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				fixFingers();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				checkPredecessor();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				checkDataMoveDown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * public static void main(String args[]) {
	 * 
	 * System.out.println("Creating nodes...");
	 * 
	 * ChordNode n1 = new ChordNode("yorkshire"); // new server node ChordNode n2 =
	 * new ChordNode("lancashire"); // new server node ChordNode n3 = new
	 * ChordNode("cheshire"); // new server node
	 * 
	 * System.out.println("Joining nodes to network...");
	 * 
	 * n1.join(n2); // nodes join load balancer n3.join(n1); // nodes join load
	 * balancer
	 * 
	 * // -- wait a bit for stabilisation --
	 * 
	 * System.out.println("Waiting for topology to stabilise...");
	 * 
	 * try { Thread.sleep(12000); } catch (InterruptedException e) {
	 * System.out.println("Interrupted"); }
	 * 
	 * // CLIENT SIDE STUFF
	 * 
	 * System.out.println("Inserting keys...");
	 * 
	 * String key1 = "alex"; byte[] data1 = new byte[128];
	 * 
	 * String key2 = "sam"; byte[] data2 = new byte[64];
	 * 
	 * String key3 = "jamie"; byte[] data3 = new byte[256];
	 * 
	 * n1.put(key1, data1); n1.put(key2, data2); n3.put(key3, data3);
	 * 
	 * System.out.println("All done (press ctrl-c to quit)");
	 * 
	 * 
	 * // print finger table contents
	 * System.out.println("--- printing finger table n1 120"); for (int i = 0; i <
	 * n1.fingerTableLength; i++) { System.out.println("--- key " +
	 * n1.finger[i].key); System.out.println("-- node " +
	 * n1.finger[i].node.getKey()); } // print finger table contents
	 * System.out.println("--- printing finger table n2 144"); for (int i = 0; i <
	 * n2.fingerTableLength; i++) { System.out.println("--- key " +
	 * n2.finger[i].key); System.out.println("-- node " +
	 * n2.finger[i].node.getKey()); } // print finger table contents
	 * System.out.println("--- printing finger table n3 71"); for (int i = 0; i <
	 * n3.fingerTableLength; i++) { System.out.println("--- key " +
	 * n3.finger[i].key); System.out.println("-- node " +
	 * n3.finger[i].node.getKey()); }
	 * 
	 * 
	 * System.out.println("--- Getting key " + key1 + " should get value " + data1);
	 * System.out.println(n3.get(key1));
	 * 
	 * System.out.println("--- Getting key " + key2 + " should get value " + data2);
	 * System.out.println(n2.get(key2));
	 * 
	 * System.out.println("--- Getting key " + key3 + " should get value " + data3);
	 * System.out.println(n1.get(key3)); }
	 */
}