package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import common.Commande;

public class ApplicationClient {
	
	private int serverPort;
	private String serverHostname;
	
	public ApplicationClient (Commande c, String hostname, int port){
		this.serverHostname = hostname;
		this.serverPort = port;
		traiteCommande(c);
	}

	/**
	 * prend le fichier contenant la liste des commandes, et le charge dans une 
	 * variable du type Commande qui est retournée
	 */
	public Commande saisisCommande(BufferedReader fichier) {
		return null;
	}

	/**
	 * initialise : ouvre les différents fichiers de lecture et écriture
	 */
	public void initialise(String fichCommandes, String fichSortie) {
		
	}

	/**
	 * prend une Commande dûment formatée, et la fait exécuter par le serveur. Le résultat de 
	 * l’exécution est retournée. Si la commande ne retourne pas de résultat, on retourne null.
	 * Chaque appel doit ouvrir une connexion, exécuter, et fermer la connexion. Si vous le
	 * souhaitez, vous pourriez écrire six fonctions spécialisées, une par type de commande
	 * décrit plus haut, qui seront appelées par traiteCommande(Commande uneCommande) 
	 */
	public Object traiteCommande(Commande uneCommande) {
		
		try {
			
			Socket mSocket = new Socket(serverHostname, serverPort);
			ObjectOutputStream oos = new ObjectOutputStream(mSocket.getOutputStream());
			oos.writeObject(uneCommande);
			//oos.close();
			
			ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
			Commande c = (Commande) ois.readObject();
			
			System.out.println(c);
			
			oos.close();
			mSocket.close();
			
			return c.getResult();
			
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}

	/**
	 * cette méthode vous sera fournie plus tard. Elle indiquera la séquence d’étapes à exécuter 
	 * pour le test. Elle fera des appels successifs à saisisCommande(BufferedReader fichier) et 
	 * traiteCommande(Commande uneCommande).
	 */
	public void scenario() {

	}

	/**
	 * programme principal. Prend 4 arguments: 
	 * 1) “hostname” du serveur, 
	 * 2) numéro de port, 
	 * 3) nom fichier commandes, 
	 * 4) nom fichier sortie. 
	 * Cette méthode doit créer une instance de la classe ApplicationClient, l’initialiser, puis 
	 * exécuter le scénario
	 */
	public static void main(String[] args) {
		
		String hostname = "localhost";
		int port = 1111;
		
		String[] mArguments = new String[2];
		mArguments[0] = "./src/server/Etudiant.java";
		mArguments[1] = "./classes";		
		new ApplicationClient(new Commande(Commande.TYPE_COMPILATION, mArguments), hostname, port);
		
		/*
		mArguments = new String[1];
		mArguments[0] = "server.Etudiant";
		new ApplicationClient(new Commande(Commande.TYPE_CHARGEMENT, mArguments), hostname, port);
		
		mArguments = new String[2];
		mArguments[0] = "server.Etudiant";
		mArguments[1] = "etudiant1";
		new ApplicationClient(new Commande(Commande.TYPE_CREATION, mArguments), hostname, port);
		
		mArguments = new String[3];
		mArguments[0] = "etudiant1";
		mArguments[1] = "nom";
		mArguments[2] = "David";
		new ApplicationClient(new Commande(Commande.TYPE_ECRITURE, mArguments), hostname, port);
		
		mArguments = new String[2];
		mArguments[0] = "etudiant1";
		mArguments[1] = "nom";
		new ApplicationClient(new Commande(Commande.TYPE_LECTURE, mArguments), hostname, port);
		
		mArguments = new String[3];
		mArguments[0] = "etudiant1";
		mArguments[1] = "isMe";
		mArguments[2] = "java.lang.String:David";
		new ApplicationClient(new Commande(Commande.TYPE_APPEL, mArguments), hostname, port);
		
		mArguments = new String[3];
		mArguments[0] = "etudiant1";
		mArguments[1] = "isMe";
		mArguments[2] = "java.lang.String:Bob";
		new ApplicationClient(new Commande(Commande.TYPE_APPEL, mArguments), hostname, port);
		*/
	}
}
