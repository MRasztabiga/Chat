import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;

public class ChatServer {
	Socket socket, socketAddingUser, socketRenewingConnection, socketEndingConnection;
	public static ArrayList<PrintWriter> ChatUsers = new ArrayList<>(); 
	public static  ArrayList<String> arrayOfConnecting = new ArrayList<String>();
	public static int x = 0, y = 0, z = 0;
	@SuppressWarnings("unchecked")
	public ChatServer(){
		ServerSocket server;
		try {
			server = new ServerSocket(5000);
			new Thread(new MonitorTheClientsAdd()).start();
			new Thread(new MonitorStartingConnection()).start();
			new Thread(new MonitorEndingConnection()).start();
			while(true){
				socket = server.accept(); //nas³uchuje czy ktos chce sie polaczyc
				new Thread(new ClientID(socket)).start(); //jesli ktos sie laczy, nowy w¹tek zostaje utworzony
				PrintWriter w = new PrintWriter(socket.getOutputStream());
				ChatUsers.add(w);
			}
		} catch (IOException e) {e.printStackTrace();}	
	}
	
	/**************************************************************************************************/
	/*Metoda odpowiadajaca za przeslanie wiadomosci napisanej przez jednego uzytkownika do wszystkich 
	 * pozostalych ktorzy sa zalogowani*/
	private void Print4all(String txt){
		for(PrintWriter w : ChatUsers){
			try{
				w.println(txt);
				w.flush();
			}catch(Exception e){}
		}
	}
	/*Klasa odpowiadajaca za usuwanie z pliku tekstowego serwera uzytkownika ktory wlasnie konczy polaczenie*/
	private class MonitorEndingConnection implements Runnable{
		ServerSocket server;

		public MonitorEndingConnection(){
			if(z == 0) {
				try {
					server = new ServerSocket(5003);
					z++;
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			DataInputStream dataInput;
			String original = "C:/Users/Micha³/Desktop/list.txt";
			String temporary = "C:/Users/Micha³/Desktop/tempList.txt";
			try {

				while(true){
				socketEndingConnection = server.accept();
				dataInput = new DataInputStream(new BufferedInputStream(socketEndingConnection.getInputStream()));
				arrayOfConnecting.add(dataInput.readUTF().toString());
				System.out.println("Deleting user:" +arrayOfConnecting.get(arrayOfConnecting.size()-1));
				dataInput.close(); 
				
				new ChatClient.FileOperationReadingAndRemoving(original, temporary, arrayOfConnecting.get(arrayOfConnecting.size()-1).toString());
				
				}
				} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/*Klasa odpowiadajaca za usuwanie z pliku tekstowego serwera uzytkownika ktory wlasnie rozpoczyna/wznawia polaczenie*/
	private class MonitorStartingConnection implements Runnable{
		ServerSocket server;
		public MonitorStartingConnection() {
				// TODO Auto-generated constructor stub
			if(y == 0) {
				try {
				server = new ServerSocket(5002);
				y ++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		}
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			DataInputStream dataInput;
			try {

				while(true){
				socketRenewingConnection = server.accept(); // gniazdo serwera ustawione w tryb nasluchu
				dataInput = new DataInputStream(new BufferedInputStream(socketRenewingConnection.getInputStream())); //wejsciowy bufor danych zapisany do Stringa
				arrayOfConnecting.add(dataInput.readUTF().toString());//dodanie do listy zalogowanych
				System.out.println("One index has:" +arrayOfConnecting.get(arrayOfConnecting.size()-1));
				dataInput.close(); 
			
				new ChatClient.FileOperationWriting("C:/Users/Micha³/Desktop/list.txt", arrayOfConnecting.get(arrayOfConnecting.size()-1).toString());
				
				}
				} catch (Exception e) {
				
				e.printStackTrace();
			}
			
	}
	}
	/*Klasa odpowiadajaca za usuwanie z pliku tekstowego serwera uzytkownika ktory wlasnie rozpoczyna polaczenie(otwiera okno dodawania do konwersacji)**/
	private class MonitorTheClientsAdd implements Runnable  {
		ServerSocket socketServer;
	
		public MonitorTheClientsAdd() {
			// TODO Auto-generated constructor stub
			
		if(x == 0) {
				try {
				socketServer = new ServerSocket(5001);
				x ++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		}
		@SuppressWarnings({ "unchecked", "deprecation" })
		@Override
		public void run(){
			// TODO Auto-generated method stub
			DataInputStream dataInput;
			try {
				while(true){
				socketAddingUser = socketServer.accept();
				dataInput = new DataInputStream(new BufferedInputStream(socketAddingUser.getInputStream()));
				arrayOfConnecting.add(dataInput.readUTF().toString());
				System.out.println("One index has:" +arrayOfConnecting.get(arrayOfConnecting.size()-1));
	
				new ChatClient.FileOperationWriting("C:/Users/Micha³/Desktop/list.txt", arrayOfConnecting.get(arrayOfConnecting.size()-1).toString());
				dataInput.close();
				}
				} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	}
	}
	/*Klasa odpowiedzialna za odczyt strumienia przez klienta */
	private class ClientID implements Runnable {
		Scanner reader;
		
		public ClientID(Socket socket){
			
				try {
						reader = new Scanner(socket.getInputStream());
				} catch (IOException e) {e.printStackTrace();}
			}
			@Override
			public void run() {
					
						try{
							String msg;
							while(reader.hasNextLine() && (msg = reader.nextLine()) != null){
								System.out.println("Received from " + msg);
								Print4all(msg);
								
							}
						}catch(Exception e){e.printStackTrace();}	
			}		}
	
	public static void main(String[] args){
		new ChatServer();	
	}
}
