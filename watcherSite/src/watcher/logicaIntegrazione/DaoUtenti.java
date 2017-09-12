package watcher.logicaIntegrazione;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import java.util.logging.Level;
import java.util.logging.Logger;

import watcher.logicaBusiness.entit�.Utente;

/**
 * Classe per il recupero e la modifica di dati degli utenti sul database
 * 
 * @author Ivan Lamparelli, Graziano Accogli
 * */
public class DaoUtenti {
	
	/**Il gestore della connessione con il database*/
	private final DBManager dbm = DBManager.getIstanza();
	
	/**L'oggetto usato per stampare i messaggi d'errore*/
	 private final static Logger LOGGER = Logger.getLogger(DaoUtenti.class.getName());
	
	//I template delle query da eseguire
	private final static String TEMPLATE_VERIFICA_CREDENZIALI = 
			"SELECT email, flagadmin FROM utente WHERE email = ? AND password = SHA2(?, 256);";
	
	private final static String TEMPLATE_CERCA_UTENTE = 
			"SELECT email, flagadmin FROM utente WHERE email = ?;";
	
	private final static String QUERY_MOSTRA_ELENCO_CLIENTI = 
			"SELECT email, flagadmin FROM utente WHERE flagadmin = 0;";  
	
	private final static String TEMPLATE_INSERISCI_CLIENTE = 
			"INSERT INTO utente(email, password, flagadmin) VALUES (?, SHA2(?, 256), '0');";
	
	private final static String TEMPLATE_ELIMINA_CLIENTE = 
			"DELETE FROM utente WHERE email = ? AND flagadmin = 0;";
	
	/**La query finale (costruita a partire dai template)*/
	private PreparedStatement query = null;
	
	/**L'oggetto ResultSet in cui vengono temporaneamente salvati i risultati delle query*/
	private ResultSet risultatoQuery = null;
	
	/**Il risultato da restituire per le query su un singolo utente*/
	private Utente utente = null;
	
	/**Il costruttore*/
	public DaoUtenti() {}
	
	/**
	 * Controlla le credenziali di un utente
	 * 
	 * @param unaMail la mail da cercare
	 * @param unaPassword la password da cercare
	 * 
	 * @return Un oggetto utente popolato in caso di credenziali valide, null altrimenti
	 * @throws SQLException Eccezione in caso di utente non presente
	 */
	public Utente validaUtente(String unaMail, String unaPassword) throws SQLException {
		
		query = dbm.ottieniPreparedStatement(TEMPLATE_VERIFICA_CREDENZIALI);
		query.setString(1, unaMail);
		query.setString(2, unaPassword);
		
		risultatoQuery = query.executeQuery();
		
		utente = this.popolaUtente(risultatoQuery);
		
		return utente;
	}
	
	/**
	 * Metodo interno per cercare le informazioni di un utente nel database
	 * 
	 * @param unaMail la mail dell'utente da cercare
	 * 
	 * @return Un oggetto utente popolato in caso di credenziali valide, null altrimenti
	 * @throws SQLException Eccezione in caso di utente non presente
	 */
		
	Utente cercaUtente(String unaMail) throws SQLException {
		
		query = dbm.ottieniPreparedStatement(TEMPLATE_CERCA_UTENTE);
		query.setString(1, unaMail);
		
		risultatoQuery = query.executeQuery();
		
		utente = this.popolaUtente(risultatoQuery);
				
		return utente;
	}
	
	/**
	 * Metodo interno per verificare se un utente � presente nel database
	 * @param unaMail La mail dell'utente da cercare
	 * @return true se l'utente � presente, false altrimenti
	 */
	boolean �PresenteUtente(String unaMail) {
		boolean presente = false;
		
		try {
			if (cercaUtente(unaMail) != null)
				presente = true;
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Problemi nel cercare l'utente");
		}
		return presente;
	}
	
	/**
	 * Metodo per controllare se l'utente � cliente o amministratore
	 * @param unaMail La Mail da controllare
	 * @return true se � un cliente, false altrimenti
	 */
	boolean �Cliente(String unaMail) {
		boolean cliente = false;
		
		try {
			Utente utente = this.cercaUtente(unaMail);
			if (utente != null && utente.�Amministratore() == false) {
				cliente = true;
			}
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Problemi nella verifica se � un cliente o amministratore");
		}
		
		return cliente;
	}
	/**Restituisce l'elenco dei clienti
	 * @return Una lista con l'elenco dei clienti
	 * @throws SQLException Eccezione in caso di lista vuota
	 * */
	public LinkedList<Utente> mostraClienti() throws SQLException {

		/**Il risultato da restituire per le query su una lista di utenti*/
		LinkedList<Utente> listaUtenti = new LinkedList<Utente>();
		
		query = dbm.ottieniPreparedStatement(QUERY_MOSTRA_ELENCO_CLIENTI);
		risultatoQuery = query.executeQuery();

		//Finch� ci sono altri clienti 
		while(risultatoQuery.next()) {
			String email = risultatoQuery.getString("email");
			boolean flagAdmin = risultatoQuery.getBoolean("flagadmin");
			utente = new Utente(email, flagAdmin);
			listaUtenti.add(utente);
		}
		return listaUtenti;
	}
	
	
	/**
	 * Registra un nuovo cliente nel database
	 * 
	 * @param unaMail la mail del cliente da registrare
	 * @param unaPassword la password del cliente da registrare
	 * 
	 * @return true se il cliente � stato inserito, false se il cliente � gi� presente
	 * */
	public boolean inserisciNuovoCliente(String unaMail, String unaPassword) {
		boolean clienteInserito = false;
		try {
			utente = this.cercaUtente(unaMail);
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Problemi nella ricerca dell'utente");
		}
		
		//Se NON esiste un utente con l'email da inserire, procedo con l'inserimento
		if(utente == null) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_INSERISCI_CLIENTE);
			try {
				query.setString(1, unaMail);
				query.setString(2, unaPassword);
				
				query.executeUpdate();
				clienteInserito = true;
				
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Problemi nell'inserimento del nuovo cliente");
			}
			
		}

		return clienteInserito;
	}
	
	/**
	 * Elimina un cliente nel database
	 * @param unaMail la mail del cliente da eliminare
	 * @return true se il cliente � stato eliminato, false se la mail non � presente o appartiene ad un amministratore
	 * */
	public boolean eliminaCliente(String unaMail) {
		boolean clienteEliminato = false;
		try {
			utente = this.cercaUtente(unaMail);
		} catch (SQLException e1) {
			LOGGER.log(Level.SEVERE, "Problemi nella ricerca dell'utente");
		}
		
		if (utente != null) { //Se l'utente cercato � presente 
			if (utente.�Amministratore() == false) { //Se l'utente � un cliente
				query = dbm.ottieniPreparedStatement(TEMPLATE_ELIMINA_CLIENTE);
				try {
					query.setString(1, unaMail);
					query.executeUpdate();
					
				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, "Problemi nell'eliminazione dell'utente");
				}
				clienteEliminato = true;		
			}
		}
		
		return clienteEliminato;
	}
	
	/**
	 * Metodo interno per la conversione da ResultSet a Utente
	 * 
	 * @param rsUtente Il risultato di una query di ricerca
	 * @return Un oggetto utente popolato in caso di utente presente, null altrimenti
	 * @throws SQLException Eccezione in caso di ResultSet vuoto
	 */
	private Utente popolaUtente(ResultSet rsUtente) throws SQLException {
		
		//Se il ResultSet contiene un utente
		if(rsUtente.next()) { 
			String email = rsUtente.getString("email");
			boolean flagAdmin = rsUtente.getBoolean("flagadmin");
			utente = new Utente(email, flagAdmin);			
		
		//Se il ResultSet � vuoto
		} else {
			utente = null;	//Reimposto a null la variabile utente (riazzerando gli eventuali contenuti presenti)
		}
		
		return utente;
	}
	
	
//	
//	public static void main(String[] args) {
//		DaoUtenti dao = new DaoUtenti();
//		
//		//validaUtente, utente presente
//		Utente utv1 = null;
//		System.out.println("valida utente presente");
//		try {
//			utv1 = dao.validaUtente("lamparelli.ivan@gmail.com", "pass2");
//			if (utv1 != null) {
//				System.out.println(utv1.getEmail() + ", " + utv1.getFlagAdmin());
//			} else {
//				System.out.println("� null");
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println("--------");
//		
//		//validaUtente, utente non presente
//		Utente utv2 = null;
//		try {
//			utv2 = dao.validaUtente("vdfbbbdblamparelli.ivan@gmail.com", "pass2");
//			System.out.println("valida utente NON presente");
//			if (utv2 != null) {
//				System.out.println(utv2.getEmail() + ", " + utv2.getFlagAdmin());
//			} else {
//				System.out.println("� null");
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} 
//		
//		System.out.println("-------------");
//		
//		
//		//Prova cerca utente
//		
//		Utente ut2 = null;
//		try {
//			ut2 = dao.cercaUtente("lamparelli.ivan@gmail.com");
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println("cerco utente presente");
//		System.out.println(ut2.getEmail());
//		
//		System.out.println("----------");
//		
//		
//		//Prova mostra clienti
//		LinkedList<Utente> l;
//		try {
//			System.out.println("Mostra i clienti");
//			l = dao.mostraClienti();
//			for(Utente u : l) {
//				System.out.println(u.getEmail());
//			}
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println("----------");
//		
//		//Prova inserire NUOVO utente
//		boolean prova = false;
//		System.out.println("inserisci nuovo cliente");
//		prova = dao.inserisciNuovoCliente("ciccio2@gmail.com", "pass2");
//		System.out.println(prova);
//		
//		System.out.println("----------");
//		
//		//Prova inserire utente gi� presente
//		prova = false;
//		System.out.println("inserisci cliente gi� presente");
//		prova = dao.inserisciNuovoCliente("ciccio2@gmail.com", "pass2");
//		System.out.println(prova);
//		
//		System.out.println("---------");
//		
//		//ELIM
//		System.out.println("elimina cliente esistente");
//		boolean prova2 = dao.eliminaCliente("ciccio2@gmail.com");
//		System.out.println(prova2);
//	
//		System.out.println("------------");
//		
//		System.out.println("elimina cliente NON esistente");
//		prova2 = dao.eliminaCliente("ciccio2@gmail.com");
//		System.out.println(prova2);	
//		
//	}
	
}
