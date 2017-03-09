import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;
import javax.xml.stream.events.EndDocument;

import org.apache.commons.io.FileUtils;

import javax.swing.text.AttributeSet.CharacterAttribute;

public class ChatClient extends JFrame implements KeyListener {

	private static final long serialVersionUID = 1L;
	JTextField msgText, Username;
	Socket socket;
	PrintWriter writer;
    String name, ClientName;
	JTextArea chatTextArea;
	Scanner reader;
	ChatServer server;
	Container loggedUsersPanel;
	private int closingSocket = 0;

	/*Kontruktor odpowiadajacy za wywolanie okna pobrania nazwy nowo utworzonego klienta,
	 * po kliknieciu w przycisk LOGIN zostaje wywolany konstruktor ChatClient(String name)*/
	public ChatClient(){
		super("Adding user to conversation...");
		Font font2Titles = new Font("Candara", Font.BOLD, 18);
		
		Username = new JTextField();
		Username.setFocusable(true);
		Username.requestFocusInWindow();
		Username.setFont(font2Titles);
		Username.setText("DefaultUser" + (int)((Math.random() + 5) * (Math.random()) * 25));
		Username.addKeyListener(this);
		
		JButton addUserButton = new JButton("LOG IN");
		addUserButton.setForeground(Color.red);
		addUserButton.setFont(font2Titles);
		addUserButton.addActionListener(new AddUserListener());
	
		Container addingOperationContainer = new JPanel();
		addingOperationContainer.setLayout(new BorderLayout());
		addingOperationContainer.add(Username, BorderLayout.CENTER);
		addingOperationContainer.add(addUserButton, BorderLayout.EAST);
		getContentPane().add(addingOperationContainer, BorderLayout.NORTH);
	
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(400,100);
		setVisible(true);
	}
	/*Konstruktor odpowiedzialny za utworzenie okna rozmowy okreslonego uzytkownika, skonfigurowanie jego sieci
	 * oraz calego mechanizmu obslugi polaczenia, wysylania i odbierania wiadomosci 
	 * Utworzone zostaje okno GUI, wraz z przyciskami oraz uszergowane zostaja wszystkie jego elementy przy uzyciu
	 * komponentu BorderLayout*/
	public ChatClient(String name){
		super("Comunicator, logged as: " + name);
		this.name = name;
		addKeyListener(this);
		Font font2Messages = new Font("Avenir Next Cyr W04 Light It", Font.BOLD, 16);
		msgText = new JTextField();
		msgText.setFont(font2Messages);
		msgText.addKeyListener(this);
		
		JButton sendButton = new JButton("send");
		JButton disconnect = new JButton("disconnect");
		JButton connect = new JButton("connect");
		
		sendButton.setBackground(Color.black);
		sendButton.setForeground(Color.white);
		sendButton.setFont(font2Messages);
		sendButton.addActionListener(new SendmessageListener());
		
		disconnect.setBackground(Color.black);
		disconnect.setForeground(Color.white);
		disconnect.setFont(font2Messages);
		disconnect.addActionListener(new endConnection());
		connect.setBackground(Color.black);
		
		connect.setForeground(Color.white);
		connect.setFont(font2Messages);
		connect.addActionListener(new beginConnection());
		
		Container msgField = new JPanel();
		Container configurationOfConnection = new JPanel();
		loggedUsersPanel = new JPanel();
		
		configurationOfConnection.setLayout(new BorderLayout());
		configurationOfConnection.add(BorderLayout.CENTER, connect);
		configurationOfConnection.add(BorderLayout.EAST, disconnect);
		
		msgField.setLayout(new BorderLayout());
		msgField.add(BorderLayout.CENTER, msgText);
		msgField.add(BorderLayout.EAST, sendButton);
		
		chatTextArea = new JTextArea();
		chatTextArea.setFocusable(true);
		chatTextArea.setEditable(false);
		chatTextArea.requestFocusInWindow();
		chatTextArea.setBackground(Color.BLACK);
		chatTextArea.setForeground(Color.WHITE);
		DefaultCaret caret = (DefaultCaret)chatTextArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		caret.setBlinkRate(200);
		chatTextArea.setFont(font2Messages);
	
		JScrollPane scrollPanel = new JScrollPane(chatTextArea);
		getContentPane().add(BorderLayout.CENTER, scrollPanel);
		getContentPane().add(BorderLayout.NORTH, msgField);
		getContentPane().add(BorderLayout.SOUTH, configurationOfConnection);
		getContentPane().add(BorderLayout.AFTER_LINE_ENDS,loggedUsersPanel);
		configureNetwork();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(true);
		setSize(400,600);
		setVisible(true);
	}
	/*Klasa obslugujaca zapis do pliku txt przyjmujaca jako argumenty sciezke oraz 
	 * dane do zapisu, w przypadku gdy plik nie istnieje - zostaje utworzony
	 * w innym przypadku plik zostaje otwarty w trybie do dopisywania*/
	public static class FileOperationWriting{
		
		public FileOperationWriting(String path, String argument){
			try{
					File list = new File(path);
					if(list.length() == 0){
					FileWriter writer = new FileWriter(list);
					BufferedWriter buf = new BufferedWriter(writer);
					buf.write(argument);
					buf.newLine();
					buf.close();
					}
					else{
						FileWriter writer = new FileWriter(list, true);
						BufferedWriter buf = new BufferedWriter(writer);
						buf.write(argument);
						buf.newLine();
						buf.close();
					}
				}catch(Exception e){System.out.println("B³¹d podczas tworzenia pliku");}
			}
		//
		public FileOperationWriting(String pathTemporary, String pathOriginal, String argumentForTemporary, int x) {
			File temp = new File(pathTemporary);
			try {
				FileWriter fwritemp = new FileWriter(temp, true);
				BufferedWriter buffer = new BufferedWriter(fwritemp);
				
				buffer.write(argumentForTemporary);
				buffer.newLine();
				buffer.close();
				fwritemp.close();
				buffer.close();
				
			} catch (IOException e) {
				System.out.println("B³¹d podczas tworzenia pliku lub operacji na plikach");
				e.printStackTrace();
			}
		}
	}
	/*Metoda "czysczaca" plik tekstowy o wskazanej sciezce*/
	public static void clearTheFile(String path){
		try {
			FileWriter fw = new FileWriter(path, false);
			PrintWriter pw = new PrintWriter(fw, false);
				pw.flush();
				pw.close();
				fw.close();
		} catch (IOException e) {
			System.out.println("B³¹d czyszczenia pliku");
			e.printStackTrace();
		}
	}
	/* Klasa odpowiedzalana za znajdowanie oraz usuwanie ze wskazanego pliku zrodlowego 
	 * klienta ktory konczy aktualnie polaczenie oraz usuniecie go z pliku tekstowego serwera
	 * ktory zawiera obecnie zalogowanych klientow
	 * */
	public static class FileOperationReadingAndRemoving{
		
		public FileOperationReadingAndRemoving(String pathForOriginal, String pathForTemporary, String StringToRemove){
			BufferedReader input;
			File original = new File(pathForOriginal);
			File temporary = new File(pathForTemporary);
			String line = "";
			try {
				input = new BufferedReader(new FileReader(pathForOriginal));
				while((line = input.readLine())!= null){
					if(line.contains(StringToRemove)){
						System.out.println("Usuniêto z listy po³¹czonych: " + StringToRemove);
					}
					else{
						new FileOperationWriting(pathForTemporary, pathForOriginal, line, 0);
					}
				} 	
				/*==================================================================================*/
				FileUtils.copyFile(temporary,original); // przepisanie pliku temp.txt na orygina³ dokonuje sie dopiero po uprzednim przeczytaniu ca³ego pliku oryginalnego pod k¹tem szukanego s³owa do usuniêcia
				clearTheFile(pathForTemporary); // po odczycie ka¿dej linii listOfLogged.txt oraz pe³nej aktualizacji temp.txt a tym samym listOfLogged.txt, nastêpuje usuniêcie zawartosci temp.txt
				/*==================================================================================*/
			}catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		}
/*Metoda konfigurujaca gniazdo, ustanawia po³aczenie pomiedzy klientem a serwerem na okreslonym porcie, 
 * utworzone zostaja takze obiekty odpowiedzialne za odczyt oraz zapis strumienia*/

	private void configureNetwork(){
		try{
			socket = new Socket("192.168.0.192", 5000);
			writer = new PrintWriter(socket.getOutputStream());
			reader = new Scanner(socket.getInputStream());
			new Thread(new ServerListener()).start();
		}
		catch(Exception e){}
	}
/*klasa odpowiedzialna za dodawanie nowego klienta
 * definiuje */
	public class AddUserListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent e) {

			if(Username.getText() != null){
				String InputString="";
				new ChatClient(Username.getText());
				try {
					Socket socket = new Socket("192.168.0.192", 5001);
					DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());// przez wspóldzielone gniazdo wysy³ana jest (do komputera bed¹cego serwerem) strumieniem jest nazwa u¿ytkownika nawi¹zuj¹cego po³aczenie
					dataOutput.writeUTF(Username.getText()); // pobierana zostaje wartosc z pola tekstowego oraz zapisana do strumienia wyjsciowego
					dataOutput.flush(); // wiadomosc zostaje wyslana
					dataOutput.close(); // strumien zostaje zamkniety
					socket.close(); // gniazdo zostaje zamkniete
				
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				new FileOperationWriting("listOfLogged.txt", Username.getText());
				setVisible(false);
				dispose();
			}
			else {
			System.err.println("Podaj prawid³ow¹ nazwê u¿ytkownika!");
			}	
		}
	}
	/* Klasa monitorujaca zdarzenie zakonczenie polaczenia przez klienta*/
	public class endConnection implements ActionListener{
		
		@Override
		public void actionPerformed(ActionEvent e) {	
			try {
				if(JOptionPane.showConfirmDialog(null, "Do you really want to disconnect?") == 0)
				{
					try {
						
						Socket socket = new Socket("192.168.0.192", 5003);
						DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());// przez wspóldzielone gniazdo wysy³ana jest (do komputera bed¹cego serwerem) strumieniem jest nazwa u¿ytkownika nawi¹zuj¹cego po³aczenie
						dataOutput.writeUTF(name);
						dataOutput.flush();
						dataOutput.close();
						socket.close();
					
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				socket.close();
				++closingSocket;
				new FileOperationReadingAndRemoving("listOfLogged.txt","temp.txt", name);
				}
				if(socket.isClosed() == true && closingSocket > 1){ JOptionPane.showMessageDialog(null, "You're already disconnected");}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	/*klasa odpowiedzialna za nawiazywanie polaczenia, nasluchujaca czy przycisk w GUI zostal klikniety
	 * oraz w razie potrzeby wywolujaca odpowiednie funkcje
	 */
	public class beginConnection implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			if(socket.isClosed() == true){
			configureNetwork();
			try {
				
				Socket socket = new Socket("192.168.0.192", 5002);
				DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());// przez wspóldzielone gniazdo wysy³ana jest (do komputera bed¹cego serwerem) strumieniem jest nazwa u¿ytkownika nawi¹zuj¹cego po³aczenie
				dataOutput.writeUTF(name);
				dataOutput.flush();
				dataOutput.close();
				socket.close();
			
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println(name);
			new FileOperationWriting("listOfLogged.txt", name);
			closingSocket = 0;
			}
			else{
				JOptionPane.showMessageDialog(null, "You are already connected!");
			}
		}
	}
	/*Klasa monitorujaca wysylanie wiadomosci przez klientow, do przycisku send jest dolaczona domyslna akcja 
	 * ktora realizuje metoda actionPerformed()*/
	public class SendmessageListener implements ActionListener { // klasa wewnêtrzna jest prywatna, uniemozliwia to dostêp do niej z innej klasy ni¿ otaczajaca

		@Override
		public void actionPerformed(ActionEvent arg0) {
			writer.println(name + " : " + msgText.getText());
			writer.flush();
			msgText.setText("");
			msgText.requestFocusInWindow();
		}
	}
	/*Klasa odpowiadajaca za nasluch, sprawdzajaca czy ktos wyslal wiadomosc
	 * jesli tak - aktualizowane zostaje pole tekstowe czatu kazdego z uzytkownikow*/
	public class ServerListener implements Runnable{

		@Override
		public void run() {
			try{
				String txt;
				while(reader.hasNextLine() && (txt = reader.nextLine()) != null) {
					chatTextArea.append(txt + "\n");
				}
				
			}catch(Exception e){e.printStackTrace();}
		}
	}
/*ponizsze metody pochodza od interfejsu KeyListener ktorego uzywamy aby dla przycisku send w GUI ustawic domyslnie przycisk ENTER*/ 
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if(writer == null){
				new ChatClient(Username.getText());
				setVisible(false);
				dispose();
			}
			else{
			writer.println(name + " : " + msgText.getText());
			writer.flush();
			msgText.setText("");
			msgText.requestFocusInWindow();
			}
        }
	}
	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
	}
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
	}
	public static void main(String[] args){
		new ChatClient();
	}
}