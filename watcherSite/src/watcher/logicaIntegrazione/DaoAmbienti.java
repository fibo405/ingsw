package watcher.logicaIntegrazione;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import java.util.logging.Level;
import java.util.logging.Logger;

import watcher.logicaBusiness.entit�.Utente;


/**
 * Classe per il recupero e la modifica di dati relativi agli ambienti
 * 
 * @author Graziano Accogli, Ivan Lamparelli
 * */
class DaoAmbienti {
	
	/**La sessione di connessione con il database*/
	private final DBManager dbm = DBManager.getIstanza();
	
	/**L'oggetto usato per stampare i messaggi d'errore*/
	 private final static Logger LOGGER = Logger.getLogger(DaoAmbienti.class.getName());
	
	//I template delle query da eseguire
	private final static String TEMPLATE_MOSTRA_NOMI_AMBIENTI_CLIENTE = 
			"SELECT nome FROM ambiente WHERE proprietario = ?;";
	
	private final static String TEMPLATE_INSERISCI_AMBIENTE = 
			"INSERT INTO ambiente(proprietario, tipo, nome) VALUES (?, ?, ?);";
	
	private final static String TEMPLATE_ELIMINA_AMBIENTE = 
			"DELETE FROM ambiente WHERE proprietario = ? AND nome = ?;";
	
	private final static String TEMPLATE_CERCA_AMBIENTE = 
			"SELECT * FROM ambiente WHERE proprietario = ? AND nome = ?;";
	
	/**La query finale (costruita a partire dai template)*/
	private PreparedStatement query = null;
	
	/**L'oggetto ResultSet in cui vengono temporaneamente salvati i risultati delle query*/
	private ResultSet risultatoQuery = null;
	


	
	/**Il costruttore*/
	DaoAmbienti() {}
	
	
	/** 
	 * Restituisce i nomi ambiente appartenenti ad un cliente selezionato  
	 * @param unProprietario Inserisce il parametro per la ricerca
	 * @return Restituisce la lista dei nomi degli ambienti 
	 * @throws SQLException Eccezione in caso di cliente senza ambienti
	 */
	LinkedList<String> mostraNomiAmbienti(String unProprietario) throws SQLException {
		
		/**Il risultato da restituire per le query su una lista di nomi di ambienti*/
		LinkedList<String> listaNomi = null;
		
		listaNomi = new LinkedList<String>();
		query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_NOMI_AMBIENTI_CLIENTE);
		query.setString(1, unProprietario);
		
		risultatoQuery = query.executeQuery();
		
		while(risultatoQuery.next()) {
			String nomeAmbiente = risultatoQuery.getString("nome");
			listaNomi.add(nomeAmbiente);
		}
		return listaNomi;
	}
	/**
	 * Registra un nuovo ambiente nel sistema
	 * @param unProprietario Il proprietario dell'ambiente
	 * @param unTipo Il tipo dell'ambiente
	 * @param unNome Il nome dell'ambiente
	 * @return true se l'ambiente non era presente ed � stato registrato, false se l'ambiente era gi� presente
	 */
	boolean inserisciAmbiente(String unProprietario, String unTipo, String unNome) {
	 	
		/**Il dao a cui delegare i controlli sulla validit� dell'utente*/
		DaoUtenti daoUT = new DaoUtenti();
		
		boolean ambienteInserito = false;
		
		Utente proprietario = null;
		try {
			proprietario = daoUT.cercaUtente(unProprietario);
		} catch (SQLException e1) {
			LOGGER.log(Level.SEVERE, "Problemi nel cercare l'utente");
		}
		
		//Se il proprietario cercato esiste
		if (proprietario != null) {
			
			//Se � un cliente
			if (proprietario.�Amministratore() == false) {
				//SE NON esiste gi� un ambiente con questo nome
				if(!this.�PresenteAmbiente(unProprietario, unNome)) {
					
					query = dbm.ottieniPreparedStatement(TEMPLATE_INSERISCI_AMBIENTE);
					try {
						query.setString(1, unProprietario);
						query.setString(2, unTipo);
						query.setString(3, unNome);
						query.executeUpdate();
				
						ambienteInserito = true;
					} catch (SQLException e) {
						LOGGER.log(Level.SEVERE, "Problemi con l'inserimento dell'ambiente");
					}
				}
				
			}
		}
		
		return ambienteInserito;
	}
	
	/**
	 * Viene eliminato un ambiente dal sistema
	 * @param unProprietario Il proprietario dell'ambiente
	 * @param unNome Il nome dell'ambiente
	 * @return true se l'ambiente era presente e viene eliminato, false se l'ambiente non � presente nel sistema
	 */
	boolean eliminaAmbiente(String unProprietario, String unNome) {
		boolean ambienteEliminato = false;
		
		if(this.�PresenteAmbiente(unProprietario, unNome)) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_ELIMINA_AMBIENTE);
			try {
				query.setString(1, unProprietario);
				query.setString(2, unNome);
				query.executeUpdate();
				
				ambienteEliminato = true;
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Problemi con l'eliminazione dell'ambiente");
			}
			
		}
		return ambienteEliminato;
	}
	
	/**
	 * Metodo per controllare se un ambiente � presente nel sistema
	 * @param unProprietario Il proprietario dell'ambiente
	 * @param unNome Il nome dell'ambiente
	 * @return true nel caso in cui � presente l'ambiente cercato, false altrimenti
	 */
	boolean �PresenteAmbiente(String unProprietario, String unNome) {
		boolean ambientePresente = false;
		
		query = dbm.ottieniPreparedStatement(TEMPLATE_CERCA_AMBIENTE);
		
		try {
			query.setString(1, unProprietario);
			query.setString(2, unNome);
			risultatoQuery = query.executeQuery();
			
			if(risultatoQuery.next()) { //Se esiste l'ambiente cercato 
				ambientePresente = true;
			}
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Problemi con la ricerca di un ambiente");
		}
		
		return ambientePresente;
	}
	
	
//	public static void main(String[] args) {
//		
//		DaoAmbienti dao = new DaoAmbienti();
//		LinkedList<String> nomi;
//		
//		System.out.println("cerco nomi ambienti cliente valido");
//		try {
//			nomi = dao.mostraNomiAmbienti("lamparelli.ivan@gmail.com");
//			for(String nome : nomi) {
//				System.out.println(nome);
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		
//		
//		System.out.println("---");
//		System.out.println("cerco nomi ambienti cliente NON valido");
//		try {
//			nomi = dao.mostraNomiAmbienti("lamparelli.ivaDAn@gmail.com");
//			for(String nome : nomi) {
//				System.out.println(nome);
//			}
//		} catch (SQLException e) {
//			System.err.println("Il cliente non esiste");
//			e.printStackTrace();
//		}
//		
//		
//		System.out.println("---");
//		System.out.println("cerco ambiente valido");
//		System.out.println(dao.�PresenteAmbiente("scay93g@gmail.com", "bari vigneto 1"));
//		
//		
//		System.out.println("---");
//		System.out.println("cerco ambiente NON valido");
//		System.out.println(dao.�PresenteAmbiente("scay93g@gmail.com", "bari vigneto 100"));
//		
//		
//		System.out.println("---");
//		System.out.println("inserisco ambiente valido");
//		System.out.println(dao.inserisciAmbiente("scay93g@gmail.com", "campo agricolo", "vigneto tasso2"));
//		
//		
//		System.out.println("---");
//		System.out.println("inserisco ambiente gi� presente");
//		System.out.println(dao.inserisciAmbiente("scay93g@gmail.com", "campo agricolo", "vigneto tasso2"));
//		
//		System.out.println("---");
//		System.out.println("elimino ambiente presente");
//		System.out.println(dao.eliminaAmbiente("scay93g@gmail.com", "vigneto tasso2"));
//		
//		System.out.println("---");
//		System.out.println("elimino ambiente NON presente");
//		System.out.println(dao.eliminaAmbiente("scay93g@gmail.com", "vigneto tasso2"));
//		
//		
//	
//	}
//	
}
