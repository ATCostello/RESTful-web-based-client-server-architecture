import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.*;

/**
 * The master server and load balancer used to handle chord nodes, and receive requests from the clients
 */
public class Server implements IFileServer {

    // Array of all currently active nodes
    static ArrayList<ChordNode> ll = new ArrayList<ChordNode>();

    public Server() {
        super();
    }

    private ChordNode n1;

    /**
     * API function to place data within the chord node's storage
     * @param name Name of the data to be stored
     * @param data byte[] data to be stored
     */
    public void put(String name, byte[] data) {
        System.out.println("Put received for " + name);

        ChordNode n = ll.get(0);
        // Check for node with least tasks
        for (int x = 0; x < ll.size(); x++) {
            ChordNode ntemp = ll.get(x);
            if (ntemp.getCounter() == 0) {
                n = ntemp;
                break;
            } else if (ntemp.getCounter() < n.getCounter()) {
                n = ntemp;
            }
        }
        n.addCounter();
        n.put(name, data);
        System.out.println("client request handled");
        n.lowerCounter();
    }

    /**
     * API function to perform operations on text documents
     * @param filename Name of the file to operate on
     * @param content byte[] contents of the file to operate on
     * @param operations Hash map containing the operations to be performed on the data
     */
    public void operate(String filename, byte[] content, HashMap<String, Integer> operations) {
        System.out.println("Operate received for " + filename);

        ChordNode n = ll.get(0);
        // Check for node with least tasks
        for (int x = 0; x < ll.size(); x++) {
            ChordNode ntemp = ll.get(x);
            if (ntemp.getCounter() == 0) {
                n = ntemp;
                break;
            } else if (ntemp.getCounter() < n.getCounter()) {
                n = ntemp;
            }
        }
        n.addCounter();
        n.operate(filename, content, operations);
        System.out.println("client request handled");
        n.lowerCounter();
    }

    /**
     * API function to perform operations on jpg files
     * @param filename Name of the file to operate on
     * @param content byte[] contents of the file to operate on
     * @param operationsJPG Hash map containing the operations to be performed on the data
     */
    public void operateJPG(String filename, byte[] content, HashMap<String, Integer> operationsJPG) {
        System.out.println("OperateJPG received for " + filename);

        ChordNode n = ll.get(0);
        // Check for node with least tasks
        for (int x = 0; x < ll.size(); x++) {
            ChordNode ntemp = ll.get(x);
            if (ntemp.getCounter() == 0) {
                n = ntemp;
                break;
            } else if (ntemp.getCounter() < n.getCounter()) {
                n = ntemp;
            }
        }
        n.addCounter();
        if (operationsJPG.get("blackandwhite").equals(1)) {
            n.operatebwJPG(filename, content, operationsJPG);
        }
        if (operationsJPG.get("flip").equals(1)) {
            if (operationsJPG.get("blackandwhite").equals(1)) {
                n.operatefpJPG(filename, get(filename), operationsJPG);
            } else {
                n.operatefpJPG(filename, content, operationsJPG);
            }
        }
        if (operationsJPG.get("colourboost").equals(1)) {
            if (operationsJPG.get("blackandwhite").equals(1) || operationsJPG.get("flip").equals(1)) {
                n.operatecbJPG(filename, get(filename), operationsJPG);
            } else {
                n.operatecbJPG(filename, content, operationsJPG);
            }
        }
        if (operationsJPG.get("stretch").equals(1)) {
            if (operationsJPG.get("blackandwhite").equals(1) || operationsJPG.get("flip").equals(1) || operationsJPG.get("colourboost").equals(1)) {
                n.operatestJPG(filename, get(filename), operationsJPG);
            } else {
                n.operatestJPG(filename, content, operationsJPG);
            }
        }
        System.out.println("client request handled");
        n.lowerCounter();
    }

    /**
     * Copies the given file data to the /uploads folder in the server
     * @param filename Name of the file to upload
     * @param contents Contents of the file to upload
     */
    public void uploadFile(String filename, byte[] contents) {
        System.out.println("Upload request received");

        if (filename.contains(".txt")) {
            // ADDED to create file once uploaded
            try {
                File newFile = new File("./uploads/" + filename);
                if (newFile.createNewFile()) {
                    // System.out.println("File created: " + newFile.getName());
                } else {
                    // System.out.println("File already exists.");
                }
            } catch (IOException e) {
                // System.out.println("An error occurred.");
                e.printStackTrace();
            }

            // ADDED to write to file
            try {
                String s = new String(contents, StandardCharsets.UTF_8);
                FileWriter myWriter = new FileWriter("./uploads/" + filename);
                myWriter.write(s);
                myWriter.close();
                // System.out.println("Successfully wrote to the file.");

            } catch (IOException e) {
                // System.out.println("An error occurred.");
                e.printStackTrace();
            }
        } else {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(contents);
                BufferedImage image = ImageIO.read(bais);
                File outputfile = new File("./uploads/" + filename);
                ImageIO.write(image, "jpg", outputfile);
            } catch (IOException e) {
                // System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }
        System.out.println("Upload request completed");
    }

    /**
     * Gets a list of all files within the uploads folder and returns them.
     * @return Array of files in the uploads folder.
     */
    public File[] getFiles() {
        File f = new File("./uploads/");

        File[] files = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("");
            }
        });

        return files;
    }

    /**
     * Download a given file from the uploads folder to the client
     * @param filename Name of the file to download
     * @return byte[] of the contents of the file
     */
    public byte[] downloadFile(String filename) {
        System.out.println("Download request received");
        try {
            Path path = Paths.get("./uploads/" + filename);
            byte[] contents = Files.readAllBytes(path);
            return contents;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * API function to retrieve data from the chord node network.
     * @param name Name of the data to search for
     * @return byte[] of the contents of this data
     */
    public byte[] get(String name) {
        System.out.println("Get received for " + name);
        System.out.println("client request handled");

        ChordNode n = ll.get(0);
        // Check for node with least tasks
        for (int x = 0; x < ll.size(); x++) {
            ChordNode ntemp = ll.get(x);
            if (ntemp.getCounter() == 0) {
                n = ntemp;
                break;
            } else if (ntemp.getCounter() < n.getCounter()) {
                n = ntemp;
            }
        }
        n.addCounter();
        n.lowerCounter();
        return n.get(name);
    }

    /**
     * Function to create the nodes for the chord network, and to join them together.
     */
    public static void createNodes() {
        try {
            String name = "yorkshire";
            ChordNode n1 = new ChordNode(name);
            ll.add(n1);
            System.out.println("Node " + name + " ready");

            name = "lancashire";
            ChordNode n2 = new ChordNode(name);
            ll.add(n2);
            System.out.println("Node " + name + " ready");

            name = "cheshire";
            ChordNode n3 = new ChordNode(name);
            ll.add(n3);
            System.out.println("Node " + name + " ready");

            System.out.println("Joining nodes to network...");
            n1.join(n2);
            n3.join(n1);

            System.out.println("Waiting for topology to stabilise...");
            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }
        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }
/**
 * Main method to create the server and the nodes once the Server.java file is run.
 * @param args
 */
    public static void main(String args[]) {
        try {
            System.out.println("Creating Server...");

            String lname = "masterserver";
            Server ms = new Server();
            IFileServer stub = (IFileServer) UnicastRemoteObject.exportObject(ms, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(lname, stub);

            System.out.println("Creating Nodes...");

            createNodes();

            System.out.println("Chord Nodes are ready!");

            System.out.println("Server " + lname + " ready");

        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }

}