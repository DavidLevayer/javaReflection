package common;

import java.io.Serializable;

public class Commande implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public final static int TYPE_LECTURE = 0;
	public final static int TYPE_ECRITURE = 1;
	public final static int TYPE_CREATION = 2;
	public final static int TYPE_CHARGEMENT = 3;
	public final static int TYPE_COMPILATION = 4;
	public final static int TYPE_APPEL = 5;
	
	public final static int COMMAND_OK = 20;
	public final static int COMMAND_ERR = 21;

	private int commandType;
	private String[] args;
	
	private Object result;
	private int resultCode;
	private String resultInformation;
	
	public Commande(int type, String[] args){
		this.commandType = type;
		this.args = args;
	}
	
	public String[] getArguments(){
		return args;
	}
	
	public int getType(){
		return commandType;
	}
	
	public Object getResult(){
		return result;
	}
	
	public void setResult(Object result){
		this.result = result;
	}

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public String getResultInformation() {
		return resultInformation;
	}

	public void setResultInformation(String resultInformation) {
		this.resultInformation = resultInformation;
	}	
	
	public String toString(){
		String res =  "";
		res += "Commande : ";
		
		switch(commandType){
		case TYPE_COMPILATION:
			res += "Compilation";
			break;
		case TYPE_CREATION:
			res += "Creation";
			break;
		case TYPE_CHARGEMENT:
			res += "Chargement";
			break;
		case TYPE_LECTURE:
			res += "Lecture";
			break;
		case TYPE_ECRITURE:
			res += "Ecriture";
			break;
		case TYPE_APPEL:
			res += "Appel";
			break;
		}
		
		res += "\n";
		for(String s: args)
			res += "--"+s+"\n";
		
		res += "Status : " + ((resultCode==COMMAND_OK)?"OK":"ERR") + "\n";
		if(result != null)
			res += "Valeur du résultat : " + result + "\n";
		
		return res;
	}
}
