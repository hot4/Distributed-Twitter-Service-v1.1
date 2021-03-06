

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class UserServer {
	/* Delimiter for UserServer encapsulation */
	public static String DELIMITER = "&";
	
	/* Directory names path delimiter */
	public static String DIRREGEX = "/";
	public static String SOURCE = "src";
	public static String DIRECTORY = "storage";
	
	public static void main(String[] args) {
		if(args.length != 2) {
	        System.out.println("FAILURE: Improper amount of arguments used");
	        System.exit(1);
	        return;
	    }
		
		new UserServer(args[0], args[1]);
	}
	
	/**
	 * @param user: Current user of the simulation
	 * @effects Creates a directory in the current path for given userName
	 * @return Amount of events that have locally occurred for this user
	 * */
	public static Integer createDirectory(User user) {
		File temp = new File("");
			
		String path = temp.getAbsolutePath() + UserServer.DIRREGEX + user.getUserName();
		File dir = new File(path);
		Integer amount = 0;
		
		/* Check if userName directory exists */
		if(!dir.exists()) {
			try {
				/* Create directory */
				dir.mkdir();
			} catch (SecurityException e) {
				System.err.println("ERROR: Could not make subdirectory.");
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			/* Check if file exists to re-populate data structures */
			File file = new File(path + UserServer.DIRREGEX + User.LOGFILE);
			
			/* No file exists */
			if (!file.exists()) return amount;

			try {
				/* Read from file */
				FileReader fileReader = new FileReader(path + UserServer.DIRREGEX + User.LOGFILE);

	            BufferedReader bufferedReader = new BufferedReader(fileReader);
	            /* Single line read from file */
	            String eventInfo  = null;
	            /* Container of single line information split by regex */
	            String[] info = null;
	            /* Container of event information */
	            ArrayList<String> eventFields = new ArrayList<String>();
	            
	            /* Read from file */
	            while((eventInfo = bufferedReader.readLine()) != null) {
	            	/* Remove padding from line */
	            	eventInfo = eventInfo.trim();
	            	
	            	/* Skip empty lines */
	            	if (eventInfo.equals("")) continue;
	            	
	            	/* Get line information */
	                info = eventInfo.split(Event.FIELDREGEX);
	                
	                /* Create event object before adding new information */
	                if (info[0].equals("Type") && !eventFields.isEmpty()) {
                		Event event = new Event(Event.typeStringToInt(eventFields.get(0)), eventFields.get(1), Integer.parseInt(eventFields.get(2)), new DateTime(eventFields.get(3)).withZone(DateTimeZone.UTC), eventFields.get(4));
                		user.addEventBasedOnType(event);
                		/* Increment the amount of times this user has generated an event */
                		if (event.getNode().equals(user.getUserName())) {
                			amount++;
                		}
                		eventFields.clear();
	                }
	                
	                /* Add event information to field container */
	                eventFields.add((String) info[1]);
	            }   
	            
	            /* Add last event from file */
        		Event event = new Event(Event.typeStringToInt(eventFields.get(0)), eventFields.get(1), Integer.parseInt(eventFields.get(2)), new DateTime(eventFields.get(3)), eventFields.get(4));
        		user.addEventBasedOnType(event);
        		/* Check if last event was causator */
        		if (event.getNode().equals(user.getUserName())) {
        			amount++;
        		}
	            
        		/* Close buffered reader */
				bufferedReader.close();
				  
			} catch (FileNotFoundException e) {
				System.err.println("ERROR: Could not read from file.");
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("ERROR: Could not close buffered reader");
				e.printStackTrace();
			}
		}
				
		return amount;
	}
	
	/**
	 * @param IP: IP to have server connect to
	 * @param port: Port to have server connect to
	 * @effects Checks if connection is available
	 * @return true if socket is able to connect to port, false otherwise 
	 * */
	public static boolean hostAvailabilityCheck(String IP, Integer port) { 
	    try (Socket s = new Socket(InetAddress.getByName(IP), port)) {
	    	s.close();
	        return true;
	    } catch (IOException ex) {
	        /* Port is not available */
	    }
	    return false;
	}
	
	public UserServer(String fileName, String userName) {		
		/* Determine current director to read file from */
		File testFile = new File("");
	    String currentPath = testFile.getAbsolutePath();
		
		/* Variables to read through *.csv file */
		BufferedReader in = null;
		String line = null;
		List<String[]> allUsers = new ArrayList<String[]>(); 
		String[] userInfo = null;
		
		User user = null;
		
		try {	
			/* Read file from current path */
			in = new BufferedReader(new FileReader(currentPath + UserServer.DIRREGEX + fileName));
			/* Parse file to get User information */
			while ((line = in.readLine()) != null) {
				/* Remove white spaces and split based on comma separator */
				userInfo = line.replace(" ", "").replace("\"", "").split(",");
				
				/* Gather all information from file */
				if (userInfo[0].equals(userName)) {
					/* Create User object based on command argument */
					user = new User(userInfo[0], userInfo[1], Integer.parseInt(userInfo[2]));
				}
				/* Store all user information */
				allUsers.add(userInfo);
			}
		} catch (FileNotFoundException e) {
			/* ERROR: Could not open file */
			System.err.println("ERROR: No file to open " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			/* ERROR: Could not parse file */
			System.err.println("ERROR: Improper file format " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} finally {
			if (in != null) {
				try {
					/* Close BufferedReader */
					in.close();
				} catch (IOException e) {
					/* ERROR: BufferedReader could not be closed */
					System.err.println("ERROR: Could not close BufferedReader " + e.getMessage());
					e.printStackTrace();
					System.exit(1);
				}	
			}
		}
		
		if (user == null) { 
			System.out.println("Username passed in through Run Configuration does not match username in CSV file");
			System.exit(1);
		}
		
		/* Follow all users */
		user.follow(allUsers);		
		
		/* Create directory */
		Integer amount = UserServer.createDirectory(user);
		user.initAmountOfEvents(amount);
		
		try {	
			/* To get input from console */
			in = new BufferedReader(new InputStreamReader(System.in));
			String command = null;
			String message = null;
			String matrixTiStr = null;
			String NPStr = null;
			SocketChannel sendSC = null;
			ByteBuffer buffer = null;
			Map<String, ArrayList<Event>> NP = new HashMap<String, ArrayList<Event>>();
			
			/* To determine timeout for input from console */
			long startTime = -1;
			
			/* Timeout to stop listening for console input or socket activity */
			Integer timeOut = 1000;
			
			/* Create a selector to check activity on port */
			Selector selector = Selector.open();
			
			/* Create a listen socket on the given port */
	        ServerSocketChannel serverSocket = ServerSocketChannel.open();
	        serverSocket.bind(new InetSocketAddress(InetAddress.getByName(user.getIP()), user.getPortNumber()));
	        serverSocket.configureBlocking(false);
	        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
	        
	        /* Return code maps to a closed connection or data received from socket connection */
	        Integer rc = -1;
	        
	        System.out.println("Hello " + user.getUserName() + ". Welcome to Twitter!");
	        
	        /* Start server */
	        while (true) {
	        	/* All incoming messages have been handled and waiting for input from console */
	        	
	        	/* Wait for User to input command into console. Timeout if no response was provided in time */
	        	startTime = System.currentTimeMillis();
	        	while ((System.currentTimeMillis() - startTime) < timeOut && !in.ready()) {}
	        	if (in.ready()) {
	        		command = in.readLine();
	        		switch(command) {
	        			case "Tweet": 
	        				System.out.print("Input Message: ");
	        				message = in.readLine();
	        				
	        				/* Send all Events that some other User needs to know about given unblocked */
	        				user.onEvent(Event.TWEETINT, message);
	        				NP = user.onSend();
	        				
	        				/* Iterate through NP to see what messages need to be sent to other User(s) */
	        				for (Map.Entry<String, ArrayList<Event>> NPEntry : NP.entrySet()) {
	        					/* Iterate through known ports until current User is found */
	        					for (Map.Entry<String, Pair<String, Integer>> portEntry : user.getPortsToSendMsg().entrySet()) {
	        						/* Check if given portEntry has the same username as the given NPEntry username */
	        						if (NPEntry.getKey().equals(portEntry.getKey()) && !user.blockedFromView(portEntry.getKey())) {
	        							/* Check if port is available to send data to */
	        							if(!UserServer.hostAvailabilityCheck(portEntry.getValue().getKey(), portEntry.getValue().getValue())) {
	        								/* Ignore port since not available */
	        								continue;
	        							}
	        							
	        							/* Open socket to current User */
	        							sendSC = SocketChannel.open(new InetSocketAddress(portEntry.getValue().getKey(), portEntry.getValue().getValue()));
	        							
	        							/* Convert this User's matrixTi to a string */
	        							matrixTiStr = user.matrixTiToString();
	        							
	        							/* Convert all Events current User needs to know about to a string */
	        							NPStr = user.NPtoString(NPEntry.getValue());
	        							
	        							/* Write this User's matrix to socket and the NP Events the current User needs to know about */
	        							message = user.getUserName() + UserServer.DELIMITER + matrixTiStr + UserServer.DELIMITER + NPStr;
	        							buffer = ByteBuffer.allocate(message.getBytes().length);
	        							buffer = ByteBuffer.wrap(message.getBytes());
	        							sendSC.write(buffer);
	        							buffer.clear();
	        							
	        							/* Close socket since all Event(s) have been sent to given port */
	        							sendSC.close();
	        							/* Since current NPEntry has been satisfied and next entry requires a different port */
	        							break;
	        						}
	        					}
	        				}
	        				break;
	        			case "Block":
	        				System.out.print("Who do you want to block? ");
	        				userName = in.readLine().trim();
	        				
	        				if (!user.blockExists(userName) && user.userNameExists(userName)) {
	        					/* Send all Events that some other User needs to know about given unblocked */
		        				user.onEvent(Event.BLOCKINT, user.getUserName() + " " + Event.BLOCKEDSTR + " " + userName);
	        				} else {
	        					System.out.println("You cannot block yourself, a nonexistent username, or a username who is already blocked");
	        				}
	        				break;
	        			case "Unblock":
	        				System.out.print("Who do you want to unblock? ");
	        				userName = in.readLine().trim();
	        				
	        				if (user.blockExists(userName) && user.userNameExists(userName)) {
	        					/* Send all Events that some other User needs to know about given unblocked */
		        				user.onEvent(Event.UNBLOCKINT, user.getUserName() + " " + Event.UNBLOCKEDSTR + " " + userName);
	        				} else {
	        					System.out.println("You cannot unblock yourself, a nonexistent username, or a username who is already unbocked");
	        				}
	        				break;
	        			case "View":
	        				user.printTweets();
	        				break;
	        			case "Log":
	        				user.printPL();
	        				break;
	        			case "Matrix":
	        				user.printMatrixTi();
	        				break;
	        			case "Help":
	        				System.out.println("Tweet: Input a message for User's to see whom you did not block.");
	        				System.out.println("Block: By inputting a username, the subsequent User will be blocked from viewing your tweets");
	        				System.out.println("Unblock: By inputting a username, the subsequent User will be unblocked from viewing your tweets.");
	        				System.out.println("View: View Tweets posted by yourself or by User's you are following and are not blocked from.");
	        				System.out.println("Log: View Events that have occurred either by yourself or by User's in the system.");
	        				System.out.println("Matrix: View Matrix of local even counter. Represented as NxN.");
	        				break;
	        			default:
	        				System.out.println("Only valid commands include: {Tweet, Block, Unblock, View, Log, Matrix, Help}");
	        				break;
	        		}
	        	}
	        	
	        	/* All commands from console has been handled and waiting for activity on socket */
	            
	        	/* Block on socket for five seconds to check for activity */
	        	selector.select(timeOut);
	        	
	        	/* Container for activity on listener socket */
	            Set<SelectionKey> selectedKeys = selector.selectedKeys();
	            Iterator<SelectionKey> itr = selectedKeys.iterator();
	            
	            /* Iterate through all keys and handle activity */
	            while (itr.hasNext()) {
	            	/* Get and remove current key from container since it only needs to be handled once */
	            	SelectionKey selKey = itr.next();
	                itr.remove();
	                
	                /* Check if this key is ready to accept a new incoming connection */
	                if (selKey.isAcceptable()) {
	                	/* Get socket channel information and accept */
	                	ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();
	                	SocketChannel sc = ssChannel.accept();
	                	
	                	/* Read data being sent through socket connection */
	                	buffer = ByteBuffer.allocate(1024);
	                	rc = sc.read(buffer);
	              
	                	/* Check return code from read() */
	                	if (rc == -1) {
	                		/* No data was sent, close socket connection */
	                		sc.close();
	                	} else {
	                		/* Format data and echo back to socket connection */
	                		buffer.flip();
	                		sc.write(buffer);
	                		
	                		user.onRecv(buffer);
	                		
	                		buffer.clear();
	                	}
	                }
	            }
	        }
		} catch (IOException e) {
			/* ERROR: System error */
			System.err.println("ERROR: System error " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
