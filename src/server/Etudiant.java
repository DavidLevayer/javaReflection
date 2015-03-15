package server;

import java.util.*;
/**
 * Title:        Classes for TP1
 * Description:
 * Copyright:    Copyright (c) 2009
 * Company:      UQAC
 * @author Hamid Mcheick et Hafedh Mili
 * @version 1.0
 */

public class Etudiant {

	private String nom;

	private Vector cours;

	public Etudiant() {
		cours = new Vector();
	}

	public Etudiant(String unNom) {
		this();
		nom = unNom;
	}

	/*
  public void inscrisDansCours(Cours unCours) {
    cours.add(unCours);
    unCours.ajouteEtudiant(this);
  }
	 */

	public void setNom(String unNom) {
		nom = unNom;
	}

	public String getNom() {
		return nom;
	}

	/*
	public float getMoyenne() {
		float totalNotes = 0;
		int nombreCours = 0;
		Iterator lesCours = cours.iterator();
		while (lesCours.hasNext()) {
			totalNotes = totalNotes + ((Cours)lesCours.next()).getNote(this);
			nombreCours++;
		}
		return totalNotes/nombreCours;
	}
	*/

	public boolean isMe(String nom){
		return this.nom.equals(nom);
	}

	public String toString() {
		return nom;
		//return nom + "[" + getMoyenne() + "]";
	}
}