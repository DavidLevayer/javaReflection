package server;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import common.Commande;

public class ApplicationServeur {

	private int port;
	
	private boolean alive;
	private ServerSocket mServerSocket;
	private Socket mClientSocket;
	private PrintStream sortieWriter;

	/**
	 * de la forme : <identifieur, référence vers l'objet>
	 */
	private Map<String,Object> mObjects;

	private Commande currentCommand;

	/**
	 * prend le numéro de port, crée un SocketServer sur le port
	 */
	public ApplicationServeur (int mPort, String sourceDir, String classDir, String logFilename) {

		this.port = mPort;
		
		try {
			sortieWriter = new PrintStream(new FileOutputStream(logFilename,false));
		} catch (FileNotFoundException e) {
			System.out.println("Fichier "+logFilename+ " introuvable");
		}
		
		try {
			mServerSocket = new ServerSocket(port);
			alive = true;
			mObjects = new HashMap<String, Object>();
			aVosOrdres();
		} catch (IOException e) {
			sortieWriter.println("ApplicationServeur: Something went terribly wrong...");
		}
	}

	/**
	 * Se met en attente de connexions des clients. Suite aux connexions, elle lit
	 * ce qui est envoyé à travers la Socket, recrée l’objet Commande envoyé par 
	 * le client, et appellera traiterCommande(Commande uneCommande)
	 */
	public void aVosOrdres() {

		sortieWriter.println("Serveur démarré");

		try {

			while(alive){

				// en attente de connexion
				sortieWriter.println("En attente de client...");
				mClientSocket = mServerSocket.accept();

				// Reception de la commande
				BufferedInputStream bis = new BufferedInputStream(mClientSocket.getInputStream());
				ObjectInputStream isr = new ObjectInputStream(bis);
				currentCommand = (Commande) isr.readObject();

				// Traitement de la commande
				if(currentCommand!=null)
					traiteCommande(currentCommand);

				mClientSocket.close();
			}

			mServerSocket.close();

		} catch (IOException | ClassNotFoundException e) {
			sortieWriter.println("aVosOrdres: Something went terribly wrong...");
		}
	}

	/**
	 * prend uneCommande dument formattée, et la traite. Dépendant du type de commande, 
	 * elle appelle la méthode spécialisée
	 * @param uneCommande
	 */
	public void traiteCommande(Commande uneCommande) {

		String[] args = uneCommande.getArguments();
		Object o;

		switch(uneCommande.getType()){
		case Commande.TYPE_LECTURE:
			o = mObjects.get(args[0]);
			traiterLecture(o,args[1]);
			break;
		case Commande.TYPE_ECRITURE:
			o = mObjects.get(args[0]);
			traiterEcriture(o, args[1], args[2]);
			break;
		case Commande.TYPE_CREATION:
			try {
				traiterCreation(Class.forName(args[0]), args[1]);
			} catch (ClassNotFoundException e) {
				sortieWriter.println("Impossible d'instancier la classe "+args[0]);
			}
			break;
		case Commande.TYPE_COMPILATION:
			traiterCompilation(args[0], args[1]);
			break;
		case Commande.TYPE_CHARGEMENT:
			traiterChargement(args[0]);
			break;
		case Commande.TYPE_APPEL:

			o = mObjects.get(args[0]);

			if(args[2] != null){
				String[] params = args[2].split(",");
				String[] types = new String[params.length];
				String[] values = new String[params.length];

				for(int i=0; i< params.length; i++){
					String[] temp = params[i].split(":");
					types[i] = temp[0];
					values[i] = temp[1];
				}

				traiterAppel(o, args[1], types, computeParameters(types, values));

			} else {
				traiterAppel(o, args[1], null, null);
			}
			break;
		}

	}

	/**
	 * traiterLecture : traite la lecture d’un attribut. Renvoies le résultat par le 
	 * socket
	 */
	public void traiterLecture(Object pointeurObjet, String attribut) {

		// On récupère la classe de l'objet
		Class<?> mClass = pointeurObjet.getClass();
		boolean criticalFailure = false;
		Object result = null;

		try {

			// On identifie l'attribut
			Field mField = mClass.getField(attribut);
			// On récupère la valeur de l'attribut
			result = mField.get(pointeurObjet);

		} catch (NoSuchFieldException e1) {

			// Si l'attribut est introuvable, on essaye le getter
			String getter = "get" + attribut.substring(0, 1).toUpperCase() + attribut.substring(1);

			try {

				// Récupération de la méthode correspondante au getter
				Method mMethod = mClass.getMethod(getter);
				// Appel du getter
				result = mMethod.invoke(pointeurObjet);

			} catch (NoSuchMethodException | SecurityException |
					IllegalAccessException | IllegalArgumentException |
					InvocationTargetException e) {
				criticalFailure = true;
			} 

		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			criticalFailure = true;
		}

		if(!criticalFailure){
			sortieWriter.println("Lecture réussie");
			sortieWriter.println("Attribut : "+attribut);
			currentCommand.setResult(result);
		} else {
			sortieWriter.println("Erreur lors de la lecture de l'attribut "+attribut);
		}

		currentCommand.setResultCode(criticalFailure?Commande.COMMAND_ERR:Commande.COMMAND_OK);
		sendResult();
	}

	/**
	 * traiterEcriture : traite l’écriture d’un attribut. Confirmes au client que l’écriture
	 * s’est faite correctement.
	 */
	public void traiterEcriture(Object pointeurObjet, String attribut, Object valeur) {

		// On récupère la classe de l'objet
		Class<?> mClass = pointeurObjet.getClass();
		boolean criticalFailure = false;

		try {

			// On identifie l'attribut
			Field mField = mClass.getField(attribut);
			// On récupère la valeur de l'attribut
			mField.set(pointeurObjet,valeur);

		} catch (NoSuchFieldException e1) {

			// Si l'attribut est introuvable, on essaye le setter
			String setter = "set" + attribut.substring(0, 1).toUpperCase() + attribut.substring(1);

			try {

				// Récupération de la méthode correspondante au setter
				Method[] mMethods = mClass.getMethods();
				boolean found = false;
				for(Method m : mMethods){
					if(m.getName().equals(setter)){
						found = true;
						// Appel du setter
						m.invoke(pointeurObjet, valeur);
					}
				}

				if(!found)
					throw new NoSuchMethodException();

			} catch (NoSuchMethodException | SecurityException |
					IllegalAccessException | IllegalArgumentException |
					InvocationTargetException e) {
				criticalFailure = true;
			} 

		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e1) {
			criticalFailure = true;
		}

		if(!criticalFailure){
			sortieWriter.println("Ecriture réussie");
			sortieWriter.println("Attribut : "+attribut+" et valeur : "+valeur);
		} else {
			sortieWriter.println("Erreur lors de l'écriture dans l'attribut "+attribut);
		}

		currentCommand.setResultCode(criticalFailure?Commande.COMMAND_ERR:Commande.COMMAND_OK);
		sendResult();
	}

	/**
	 * traiterCreation : traite la création d’un objet. Confirme au client que la création 
	 * s’est faite correctement.
	 */
	public void traiterCreation(Class<?> classeDeLobjet, String identificateur) {

		boolean criticalFailure = false;

		try {
			Object o = classeDeLobjet.newInstance();
			mObjects.put(identificateur, o);
			sortieWriter.println("Classe "+identificateur+":"
					+classeDeLobjet.getName()+" créée avec succès");
		} catch (InstantiationException | IllegalAccessException e) {
			sortieWriter.println("Erreur lors de la création de "+classeDeLobjet.getName());
			criticalFailure = true;
		}

		currentCommand.setResultCode(criticalFailure?Commande.COMMAND_ERR:Commande.COMMAND_OK);
		sendResult();
	}

	/**
	 * traiterChargement : traite le chargement d’une classe. Confirmes au client que la création 
	 * s’est faite correctement.
	 */
	public void traiterChargement(String nomQualifie) {

		ClassLoader mClassLoader = ApplicationServeur.class.getClassLoader();
		boolean criticalFailure = false;

		try {
			Class<?> c = mClassLoader.loadClass(nomQualifie);
			sortieWriter.println("Classe "+c.getName()+" chargée");
		} catch (ClassNotFoundException e) {
			sortieWriter.println("Erreur lors du chargement de "+nomQualifie);
			criticalFailure = true;
		}

		currentCommand.setResultCode(criticalFailure?Commande.COMMAND_ERR:Commande.COMMAND_OK);
		sendResult();
	}

	/**
	 * traiterCompilation : traite la compilation d’un fichier source java. Confirme au client 
	 * que la compilation s’est faite correctement. Le fichier source est donné par son chemin 
	 * relatif par rapport au chemin des fichiers sources.
	 */
	public void traiterCompilation(String cheminRelatifFichierSource, String classPath) {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		String[] files = cheminRelatifFichierSource.split(",");

		boolean criticalFailure = false;
		for(String file : files){

			int result = compiler.run(null, null, null, "-cp", classPath,
					"-d", classPath, file);

			if(result == 0) {
				sortieWriter.println(file+" compilé avec succès");
			}
			else{
				sortieWriter.println("Erreur lors de la compilation de "+file);
				criticalFailure = true;
				break;
			}
		}

		currentCommand.setResultCode(criticalFailure?Commande.COMMAND_ERR:Commande.COMMAND_OK);
		sendResult();		
	}

	/**
	 * traiterAppel : traite l’appel d’une méthode, en prenant comme argument l’objet
	 * sur lequel on effectue l’appel, le nom de la fonction à appeler, un tableau de nom de 
	 * types des arguments, et un tableau d’arguments pour la fonction. Le résultat de la
	 * fonction est renvoyé par le serveur au client (ou le message que tout s’est bien
	 * passé)
	 */
	public void traiterAppel(Object pointeurObjet, String nomFonction, String[] types,
			Object[] valeurs) {

		Class<?> mClass = pointeurObjet.getClass();
		boolean criticalFailure = false;

		boolean hasArguments = true;
		if(types == null || valeurs == null)
			hasArguments = false;

		Class<?>[] classes = null;

		if(hasArguments){

			// ETAPE 1 : Récupération des classes liées aux types d'arguments
			classes = new Class[types.length];

			for(int i=0; i<types.length; i++){
				try{
					String type = types[i];
					classes[i] = Class.forName(type);

				} catch (ClassNotFoundException e){
					// On essaye de trouver un type primitif correspondant
					Class<?> mPrimitiveClass = matchPrimitive(types[i]);
					if(mPrimitiveClass == null){
						// Si aucun type primitif ne correspond, c'est une erreur
						sortieWriter.println("Impossible de trouver la classe "+types[i]+ " ("+nomFonction+")");
						criticalFailure = true;
					} else {
						// Si un type primitif correspond, alors on prend la classe correspondante
						classes[i] = mPrimitiveClass;
					}
				}
			}
		}

		if(!criticalFailure){
			try {
				// ETAPE 2 : Récupération de la méthode correspondante
				Method mMethod = mClass.getMethod(nomFonction, classes);
				Object result = mMethod.invoke(pointeurObjet, valeurs);

				currentCommand.setResult(result);
				sortieWriter.println("Appel de la fonction " + nomFonction + " réussi");

			} catch (NoSuchMethodException | SecurityException | IllegalAccessException |
					IllegalArgumentException | InvocationTargetException e) {
				criticalFailure = true;
				sortieWriter.println("Erreur lors de l'appel de la fonction "+nomFonction);
			} 
		}

		currentCommand.setResultCode(criticalFailure?Commande.COMMAND_ERR:Commande.COMMAND_OK);
		sendResult();

	}

	/**
	 * Envoie le résultat au client (renvoie de la commande, avec
	 * certains attributs complétés)
	 */
	private void sendResult(){

		if(currentCommand != null && mClientSocket != null){
			try {
				ObjectOutputStream oos = new ObjectOutputStream(mClientSocket.getOutputStream());
				oos.writeObject(currentCommand);
				oos.close();
			} catch (IOException e) {
				sortieWriter.println("sendResult: Something went terribly wrong...");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Modifie les valeurs des paramètres afin qu'ils soient
	 * traités correctement par traiteAppel
	 */
	private Object[] computeParameters(String[] types, Object[] values){

		if(types == null || values == null)
			return null;

		Object[] result = new Object[values.length];

		for(int i=0; i<types.length; i++){

			String type = types[i];
			String value = (String)values[i];

			// On parse la valeur si besoin (de type ID(objet))
			if(type != "java.lang.String" && 
					value.contains("ID(")){
				value = value.substring(3, value.length()-1);
				result[i] = mObjects.get(value);
			}
			
			// On cast s'il s'agit d'un type primitif
			else if(matchPrimitive(type) != null){
				result[i] = castToPrimitiveType(type, value);
			}

			// Sinon : valeur inchangée
			else {
				result[i] = values[i];
			}			
		}
		return result;
	}

	/**
	 * Vérifie si la chaine passée en paramètre correspond
	 * à un type primitif de Java (et renvoie la classe associée)
	 */
	private Class<?> matchPrimitive(String type){
		if( type.equals("boolean") ) return boolean.class;
	    if( type.equals("byte") ) return byte.class;
	    if( type.equals("short") ) return short.class;
	    if( type.equals("int") ) return int.class;
	    if( type.equals("long") ) return long.class;
	    if( type.equals("float") ) return float.class;
	    if( type.equals("double") ) return double.class;
		return null;
	}
	
	/**
	 * Cast la valeur passée en paramètre en fonction du type
	 * primitif passé en paramètre
	 */
	private Object castToPrimitiveType(String type, String value){
		if( type.equals("boolean") ) return Boolean.valueOf(value).booleanValue();
	    if( type.equals("byte") ) return Byte.valueOf(value).byteValue();
	    if( type.equals("short") ) return Short.valueOf(value).shortValue();
	    if( type.equals("int") ) return Integer.valueOf(value).intValue();
	    if( type.equals("long") ) return Long.valueOf(value).longValue();
	    if( type.equals("float") ) return Float.valueOf(value).floatValue();
	    if( type.equals("double") ) return Double.valueOf(value).doubleValue();
		return null;
	}
	
	/**
	 * programme principal. Prend 4 arguments: 
	 * 1) numéro de port, 
	 * 2) répertoire source, 
	 * 3) répertoire classes, 
	 * 4) nom du fichier de traces (sortie)
	 * Cette méthode doit créer une instance de la classe ApplicationServeur, l’initialiser 
	 * puis appeler aVosOrdres sur cet objet
	 */
	public static void main(String[] args) {

		new ApplicationServeur(Integer.valueOf(args[0]),args[1],args[2],args[3]);
	}	
}
