package client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;

import common.Commande;

public class ApplicationClient {

	private int serverPort;
	private String serverHostname;
	private PrintStream sortieWriter;
	private BufferedReader commandesReader;

	public ApplicationClient (String hostname, int port, String entrees, String sortie){
		
		this.serverHostname = hostname;
		this.serverPort = port;
		
		initialise(entrees, sortie);
		scenario();
	}

	/**
	 * prend le fichier contenant la liste des commandes, et le charge dans une 
	 * variable du type Commande qui est retournée
	 */
	public Commande saisisCommande(BufferedReader fichier) {
		
		try {
			String commandLine = fichier.readLine();
			
			if(commandLine == null)
				return null;
			
			String[] commmandComponents = commandLine.split("#");
			
			Commande c = null;
			String[] args;
			switch(commmandComponents[0]){
			case "compilation":
				args = new String[2];
				args[0] = commmandComponents[1];
				args[1] = commmandComponents[2];
				c = new Commande(Commande.TYPE_COMPILATION, args);
				break;
			case "chargement":
				args = new String[1];
				args[0] = commmandComponents[1];
				c = new Commande(Commande.TYPE_CHARGEMENT, args);
				break;
			case "lecture":
				args = new String[2];
				args[0] = commmandComponents[1];
				args[1] = commmandComponents[2];
				c = new Commande(Commande.TYPE_LECTURE, args);
				break;
			case "ecriture":
				args = new String[3];
				args[0] = commmandComponents[1];
				args[1] = commmandComponents[2];
				args[2] = commmandComponents[3];
				c = new Commande(Commande.TYPE_ECRITURE, args);
				break;
			case "creation":
				args = new String[2];
				args[0] = commmandComponents[1];
				args[1] = commmandComponents[2];
				c = new Commande(Commande.TYPE_CREATION, args);
				break;
			case "fonction":
				args = new String[3];
				args[0] = commmandComponents[1];
				args[1] = commmandComponents[2];
				if(commmandComponents.length == 4)
					args[2] = commmandComponents[3];
				else
					args[2] = null;
				c = new Commande(Commande.TYPE_APPEL, args);
				break;
			}
			
			return c;
			
		} catch (IOException e) {
			System.out.println("Erreur lors de la lecture dans "+fichier);
		}
		return null;
	}

	/**
	 * initialise : ouvre les différents fichiers de lecture et écriture
	 */
	public void initialise(String fichCommandes, String fichSortie) {
		
		try {
			commandesReader = new BufferedReader(new FileReader(fichCommandes));
		} catch (FileNotFoundException e) {
			System.out.println("Fichier "+fichCommandes+ " introuvable");
		}
		
		try {
			sortieWriter = new PrintStream(new FileOutputStream(fichSortie,false));
		} catch (FileNotFoundException e) {
			System.out.println("Fichier "+fichSortie+ " introuvable");
		}
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

			ObjectInputStream ois = new ObjectInputStream(mSocket.getInputStream());
			Commande c = (Commande) ois.readObject();

			oos.close();
			mSocket.close();

			return c;

		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Erreur lors du traitement de la commande");
		} 
		return null;
	}

	/**
	 * cette méthode vous sera fournie plus tard. Elle indiquera la séquence d’étapes à exécuter 
	 * pour le test. Elle fera des appels successifs à saisisCommande(BufferedReader fichier) et 
	 * traiteCommande(Commande uneCommande).
	 */
	public void scenario() {
		sortieWriter.println("Debut des traitements:\n");
		Commande prochaine = saisisCommande(commandesReader);
		while (prochaine != null) {
			sortieWriter.println("\tTraitement de la commande...");
			Object resultat = traiteCommande(prochaine);
			sortieWriter.println(resultat);
			prochaine = saisisCommande(commandesReader);
		}
		sortieWriter.println("Fin des traitements");
		sortieWriter.close();
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

		new ApplicationClient(args[0], Integer.valueOf(args[1]), args[2], args[3]);		
	}
}
