package watcher.logicaIntegrazione;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import java.util.logging.Logger;
import java.util.logging.Level;

import watcher.logicaBusiness.entit�.DatoRilevazione;

/**
 * Classe per la lettura e il salvataggio delle impostazioni del trasferimento automatico
 * 
 * @author Ivan Lamparelli, Graziano Accogli
 * */
public class DaoTrasferimento {
	
	/**Il gestore della connessione con il database*/
	private final DBManager dbm = DBManager.getIstanza();
	
	/**L'oggetto usato per stampare i messaggi d'errore*/
	 private final static Logger LOGGER = Logger.getLogger(DaoTrasferimento.class.getName());
	
	/**Il dao a cui delegare le query sugli ambienti*/
	private final DaoAmbienti daoAMB = new DaoAmbienti();
	
	/**La query finale (costruita a partire dai template)*/
	private PreparedStatement query = null;
	
	/**L'oggetto ResultSet in cui vengono temporaneamente salvati i risultati delle query*/
	private ResultSet risultatoQuery = null;
	
	
	
	/**Il risultato da restituire per le query sugli ambienti*/
	private LinkedList<String> listaAmbientiFiltrati = null;
	
	
	//I template delle query
	
	private final static String TEMPLATE_MOSTRA_STATO_TRASFERIMENTO_CLIENTE =
			"SELECT * FROM trasferimentoautomatico WHERE idcliente = ?;";

	private final static String TEMPLATE_REGISTRAZIONE_UTENTE_TRASFERIMENTO = 
			"INSERT INTO trasferimentoautomatico(idcliente, flagattivazione, flagterzaparte) VALUES (?, 0, 0);";
	
	private final static String TEMPLATE_IMPOSTA_STATO_ATTIVAZIONE =
			"UPDATE trasferimentoautomatico SET flagattivazione = ? WHERE idcliente = ?;";
		
	private final static String TEMPLATE_AGGIUNGI_AMBIENTE_FILTRATO = 
			"INSERT INTO trasferimentopersonalizzato(proprietarioambiente, nomeambiente) VALUES (?, ?);";
	
	private final static String TEMPLATE_RIMUOVI_AMBIENTI_FILTRATI = 
			"DELETE FROM trasferimentopersonalizzato WHERE proprietarioambiente = ?;";
	
	private final static String TEMPLATE_IMPOSTA_STATO_TERZA_PARTE =
			"UPDATE trasferimentoautomatico SET flagterzaparte = ?, terzaparte = ? WHERE idcliente = ?;";
	
	private final static String TEMPLATE_MOSTRA_CLIENTI_PER_TRASFERIMENTO = 
			"SELECT idcliente FROM trasferimentoautomatico WHERE flagattivazione = 1";
	
	private final static String TEMPLATE_MOSTRA_AMBIENTI_FILTRATI = 
			"SELECT nomeambiente FROM trasferimentopersonalizzato WHERE proprietarioambiente = ?;";
	

	/**
	 * Metodo per attivare il trasferimento automatico	
	 * @param unCliente Il cliente per cui attivare il trasferimento
	 * @return Restituisce true se il trasferimento � stato attivato, false in caso di cliente non valido
	 */
	public boolean attivaTrasferimento(String unCliente) {
	
		/**Il dao a cui delegare le query sugli utenti*/
		DaoUtenti daoUT = new DaoUtenti();
		
		boolean attivo = false;
		
		//Se il cliente in input � valido, procedo con l'attivazione
		if (daoUT.�PresenteUtente(unCliente) && daoUT.�Cliente(unCliente)) {
			
			//Se il cliente in input non ha mai utilizzato il trasferimento, lo registro fra gli utilizzatori del trasferimento
			if (!this.�Presente(unCliente)) {
				query = dbm.ottieniPreparedStatement(TEMPLATE_REGISTRAZIONE_UTENTE_TRASFERIMENTO);
				try {
					query.setString(1, unCliente);
					query.executeUpdate();
				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, "Problemi nella registrazione del cliente per il trasferimento");
				}
			}
			
			//Attivo il trasferimento
			query = dbm.ottieniPreparedStatement(TEMPLATE_IMPOSTA_STATO_ATTIVAZIONE);
			try {
				query.setBoolean(1, true);
				query.setString(2, unCliente);
				query.executeUpdate();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Problemi nell'attivazione dello stato del trasferimento");
			}
			
			//Imposto il trasferimento dei dati di tutti gli ambienti del cliente
			try {
				impostaAmbienti(unCliente, daoAMB.mostraNomiAmbienti(unCliente));
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Problemi nell'impostare l'ambiente del cliente inserito per il trasferimento dei dati");
			}
			
			attivo = true;
		}
	
		return attivo;
	}
	
	
	/**
	 * Metodo per disattivare il trasferimento automatico
	 * @param unCliente Il cliente per cui disattivare il trasferimento
	 * @return Restituisce true se il trasferimento � stato disattivato, false in caso di cliente non trovato
	 */
	public boolean disattivaTrasferimento(String unCliente) {
		
		boolean disattivo = false;
		
		if(�Presente(unCliente)) {
			if(�AttivoTrasferimento(unCliente)) {
				
				query = dbm.ottieniPreparedStatement(TEMPLATE_IMPOSTA_STATO_ATTIVAZIONE);			
				
				try {
					
					query.setBoolean(1, false);
					query.setString(2, unCliente);
					
					query.executeUpdate();
					
					//Cancello le scelte in merito agli ambienti per cui ricevere il trasferimento
					cancellaAmbientiFiltrati(unCliente);
					
					disattivo = true;
					
				} catch (SQLException e) {
					LOGGER.log(Level.SEVERE, "Problemi nell'impostazione dello stato di attivazione");
				}
				
				//Se � gi� disattivo
			} else {
				disattivo = true;
			}
		}
		
		return disattivo;
	}
	
	/**
	 * Metodo per controllare lo stato del trasferimento
	 * @param unCliente Il cliente di cui controllare lo stato del trasferimento
	 * @return Restituisce true se lo stato � attivo, false se lo stato � disattivo o il cliente non � valido
	 */
	public boolean �AttivoTrasferimento(String unCliente) {
		boolean statoAttivazione = false;
		
		if (�Presente(unCliente)) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_STATO_TRASFERIMENTO_CLIENTE);
			try {
				query.setString(1, unCliente);
				risultatoQuery = query.executeQuery();
				
				risultatoQuery.next();
				statoAttivazione = risultatoQuery.getBoolean("flagattivazione");
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Problemi nel mostrare lo stato del trasferimento del cliente");
			}			
		}
		
		return statoAttivazione;
		
	}

	
	/**
	 * Metodo interno per controllare se � presente un cliente
	 * @param unCliente Il cliente da cercare
	 * @return Restituisce true se � presente un cliente che ha interagito con il trasferimento, false altrimenti
	 */
	private boolean �Presente(String unCliente) {
		
		boolean presente = false;
		query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_STATO_TRASFERIMENTO_CLIENTE);
		try {
			query.setString(1, unCliente);
			risultatoQuery = query.executeQuery();
			
			// Se il cliente � presente
			if(risultatoQuery.next()) {
				presente = true;
			}
			
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Problemi nel verificare la presenza del cliente");
		}
		return presente;
		
	}
	
	
	/**
	 * Metodo per impostare gli ambienti selezionati dal cliente su cui avviene il trasferimento automatico
	 * @param unCliente Il cliente su cui avviene l'operazione
	 * @param deiNomiAmbienti L'ambiente su cui avviente l'operazione
	 * @return true se viene impostato il trasferimento, false in caso di trasferimento disattivo o di ambienti in input non validi
	 */
	public boolean impostaAmbienti(String unCliente, LinkedList<String> deiNomiAmbienti) {
		boolean impostato = false;
	
		if (�Presente(unCliente) && �AttivoTrasferimento(unCliente) && deiNomiAmbienti != null) {
							
			//Elimino la precedente scelta di ambienti filtrati
			cancellaAmbientiFiltrati(unCliente);
			
			//Se il chiamante del metodo decide di disattivare tutti gli ambienti per il cliente
			if(deiNomiAmbienti.size() == 0) {
				impostato = true;
			} 
			
			else {
				//Inserisco i nuovi ambienti scelti
				for(String ambiente : deiNomiAmbienti) {
					query = dbm.ottieniPreparedStatement(TEMPLATE_AGGIUNGI_AMBIENTE_FILTRATO);
					
					// Se l'ambiente appartiene al cliente
					if(daoAMB.�PresenteAmbiente(unCliente, ambiente)) {
						try {
							query.setString(1, unCliente);
							query.setString(2, ambiente);
							query.executeUpdate();
							impostato = true;
							
						} catch (SQLException e) {
							LOGGER.log(Level.SEVERE,"Problemi nell'aggiunta dell'ambiente filtrato");
						}
					}
				}
				
			}
		
		}
		
		return impostato;
	}
	
	
	/**
	 * Metodo per mostrare tutti gli ambienti del cliente su cui avviene il trasferimento automatico
	 * @param unCliente Il cliente selezionato 
	 * @return Una LinkedList di tutti gli ambienti
	 * @throws SQLException Eccezione nel caso in cui il cliente non ha ambienti
	 */
	public LinkedList<String> mostraAmbienti(String unCliente) throws SQLException {
		return daoAMB.mostraNomiAmbienti(unCliente);
	}
	
	
	/**
	 * Metodo per visualizzare gli ambienti selezionati per il trasferimento automatico
	 * @param unCliente Un cliente (con il trasferimento attivo) 
	 * @return Una LinkedList degli ambienti selezionati 
	 * @throws SQLException Eccezione nel caso in cui il cliente non ha ambienti
	 */
	public LinkedList<String> mostraAmbientiFiltrati(String unCliente) throws SQLException{
		
		listaAmbientiFiltrati = new LinkedList<String>();
				
		if(�Presente(unCliente) && �AttivoTrasferimento(unCliente)) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_AMBIENTI_FILTRATI);
			query.setString(1, unCliente);
			
			risultatoQuery = query.executeQuery();
			
			while(risultatoQuery.next()) {
				String nomeAmbiente = risultatoQuery.getString("nomeambiente");
				listaAmbientiFiltrati.add(nomeAmbiente);
			}
		
		}
		return listaAmbientiFiltrati;
	}
	
	/**
	 * Metodo interno per cancellare gli ambienti da trasferire di un cliente
	 * @param unCliente Il cliente di cui cancellare gli ambienti
	 * @return true in caso di cancellazione avvenuta, false in caso di cliente non valido
	 */
	private boolean cancellaAmbientiFiltrati(String unCliente) {
		boolean cancellato = false;
		
		//Se il cliente � valido
		if (�Presente(unCliente)) {
			
			query = dbm.ottieniPreparedStatement(TEMPLATE_RIMUOVI_AMBIENTI_FILTRATI);
			try {
				query.setString(1, unCliente);
				query.executeUpdate();
				cancellato = true;
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE,"Problemi nell'eliminazione dell'ambiente filtrato");
			}
		}
			
		return cancellato;
	}
	
	
	/**
	 * Metodo per controllare lo stato dell'inoltro del trasferimento
	 * @param unCliente Il cliente di cui controllare lo stato dell'inoltro
	 * @return true se il trasferimento e l'inoltro sono attivi, false se il trasferimento � disattivo o se l'inoltro � disattivo
	 */
	public boolean �AttivaTerzaParte(String unCliente) {
		boolean flagTerzaParte = false;

		if (�Presente(unCliente) && �AttivoTrasferimento(unCliente)) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_STATO_TRASFERIMENTO_CLIENTE);
			try {
				query.setString(1, unCliente);
				risultatoQuery = query.executeQuery();
				
				risultatoQuery.next();
				flagTerzaParte = risultatoQuery.getBoolean("flagterzaparte");
								
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE,"Problemi nel mostrare lo stato della terza parte del cliente");
			}
			
		}
		
		return flagTerzaParte;

	}

	
	/**
	 * Metodo per mostrare l'indirizzo della terza parte associato ad un cliente con l'inoltro attivo
	 * @param unCliente Il cliente di cui controllare l'indirizzo dell'inoltro
	 * @return L'indirizzo dell'inoltro del trasferimento
	 * @throws SQLException Eccezione in caso di cliente non presente o inoltro non attivo
	 */
	public String mostraTerzaParte(String unCliente) throws SQLException {
		String terzaParte = "";
		
		if (�Presente(unCliente) && �AttivoTrasferimento(unCliente) && �AttivaTerzaParte(unCliente)) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_STATO_TRASFERIMENTO_CLIENTE);
			query.setString(1, unCliente);
			risultatoQuery = query.executeQuery();
			risultatoQuery.next();
			terzaParte = risultatoQuery.getString("terzaparte");
		}
		
		return terzaParte;
	}
	
	
	/**
	 * Metodo per inserire l'inoltro dei dati su un cliente con il trasferimento attivo
	 * @param unCliente Il cliente per il quale impostare l'inoltro
	 * @param unaTerzaParte La terza parte verso cui inoltrare i dati
	 * @return true se l'inoltro � stato attivato, false in caso di trasferimento disattivato o cliente non trovato o terza parte non valida
	 */
	public boolean impostaTerzaParte(String unCliente, String unaTerzaParte) {
		boolean terzaParteInserita = false;		
		
		if (�Presente(unCliente) && �AttivoTrasferimento(unCliente) && unaTerzaParte != null) {
			
			query = dbm.ottieniPreparedStatement(TEMPLATE_IMPOSTA_STATO_TERZA_PARTE);
			try {
				query.setBoolean(1, true);
				query.setString(2, unaTerzaParte);
				query.setString(3, unCliente);
				query.executeUpdate();
				terzaParteInserita = true;
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE,"Problemi nelle impostazioni di stato della terza parte");
			}
			
		}
		
		return terzaParteInserita;
	}
	
	
	/**
	 * Metodo per disattivare l'inoltro verso una terza parte dei dati del trasferimento
	 * @param unCliente Il cliente per cui disattivare l'inoltro
	 * @return true se l'inoltro � stato disattivato, false in caso di cliente non valido
	 */
	public boolean disattivaTerzaParte(String unCliente) {
		boolean terzaParteDisattivata = false;
		
		if(�Presente(unCliente) && �AttivoTrasferimento(unCliente)) {
			query = dbm.ottieniPreparedStatement(TEMPLATE_IMPOSTA_STATO_TERZA_PARTE);
			try {
				query.setBoolean(1, false);
				
				//Imposta il valore a null (specificando in input il tipo di valore da impostare a null)
				query.setNull(2, java.sql.Types.VARCHAR);
				
				query.setString(3, unCliente);
				
				query.executeUpdate();
				terzaParteDisattivata = true;
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE,"Problemi nella disattivazione della terza parte del cliente");
			}		
		}
				
		return terzaParteDisattivata;
	}
	
	
	/**
	 * Metodo per mostrare i clienti con il trasferimento attivo
	 * @return La lista di clienti con il trasferimento attivo
	 * @throws SQLException Eccezione in caso di lista vuota (nessun cliente con il trasferimento attivo)
	 */
	public LinkedList<String> mostraClientiConTrasferimentoAttivo() throws SQLException {
		
		/**Il risultato da restituire per le query su una lista di clienti*/
		LinkedList<String> clientiPerTrasferimento = new LinkedList<String>();
		
		query = dbm.ottieniPreparedStatement(TEMPLATE_MOSTRA_CLIENTI_PER_TRASFERIMENTO);
		risultatoQuery = query.executeQuery();
		
		while (risultatoQuery.next()) {
			clientiPerTrasferimento.add(risultatoQuery.getString("idcliente"));
		}
		
		return clientiPerTrasferimento;
	}
	
	
	/**
	 * Metodo per mostrare i dati richiesti da un cliente per il trasferimento automatico
	 * @param unCliente Il cliente di cui servono i dati
	 * @return Una lista dei dati rilevazioni del cliente che rispecchiano i filtri scelti dal cliente
	 * @throws SQLException Eccezione in caso di lista vuota (cliente non valido, o cliente senza dati da trasferire)
	 */
	public LinkedList<DatoRilevazione> mostraDatiDaTrasferire(String unCliente) throws SQLException {
		
		/**Il dao a cui delegare le query sulle rilevazioni*/
		DaoDatiRilevazioni daoDR = new DaoDatiRilevazioni();
		
		/**Il risultato da restituire per le query su una lista di dati rilevazioni*/
		LinkedList<DatoRilevazione> datiDaTrasferire = new LinkedList<DatoRilevazione>();
		
		listaAmbientiFiltrati = new LinkedList<String>();
		
		if (�Presente(unCliente) && �AttivoTrasferimento(unCliente)) {
						
			//Controllo gli ambienti per cui l'utente ha richiesto l'invio dei dati
			listaAmbientiFiltrati = mostraAmbientiFiltrati(unCliente);
			
			//Per ogni ambiente richiesto, raccolgo i dati, e li aggiungo ai dati da inviare
			for (String ambiente : listaAmbientiFiltrati) {
				LinkedList<DatoRilevazione> datiAmbiente = daoDR.visualizzaDatiRilevazioni(unCliente, ambiente, "");
				
				for (DatoRilevazione dato : datiAmbiente) {
					datiDaTrasferire.add(dato);
				}
			}
				
		}
		
		return datiDaTrasferire;
	}
		
	
	public static void main(String[] args) {
		
		DaoTrasferimento daoTrasf = new DaoTrasferimento();
		
		LinkedList<String> ambientiFiltri = new LinkedList<String>();
		LinkedList<String> ambientiStampa = new LinkedList<String>();
		LinkedList<String> clientiTrasf = new LinkedList<String>();
		LinkedList<DatoRilevazione> datiRil = new LinkedList<DatoRilevazione>();
		
				
		System.out.println("\n �Presente() : cliente non registrato (tizio)");
		System.out.println(daoTrasf.�Presente("tizio"));
		
		
		System.out.println("\n �Presente() : cliente registrato (hotmail)");
		System.out.println(daoTrasf.�Presente("ivanlamparelli@hotmail.it"));
		
		
		System.out.println("\n private: imposto hotmail con trasf disattivo, per fare prove");
		daoTrasf.disattivaTrasferimento("ivanlamparelli@hotmail.it");
		
		
		System.out.println("\n private: cancello gmail dai registrati, per provare a inserirlo");
		daoTrasf.query = daoTrasf.dbm.ottieniPreparedStatement("DELETE FROM trasferimentoautomatico WHERE idcliente = 'lamparelli.ivan@gmail.com';");
		try {
			daoTrasf.query.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
				
		System.out.println("\n attiva() : cliente non registrato (gmail)");
		System.out.println(daoTrasf.attivaTrasferimento("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n attiva() : cliente gi� attivo (gmail)");
		System.out.println(daoTrasf.attivaTrasferimento("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n disattiva() : cliente attivo (gmail)");
		System.out.println(daoTrasf.disattivaTrasferimento("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n disattiva() : cliente gi� disattivo (gmail)");
		System.out.println(daoTrasf.disattivaTrasferimento("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n attiva() : cliente registrato ma non attivo (gmail)");
		System.out.println(daoTrasf.attivaTrasferimento("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n disattiva() : cliente non registrato (tizio)");
		System.out.println(daoTrasf.disattivaTrasferimento("tizio"));
		
		
		System.out.println("\n �Attivo() : cliente attivo (gmail)");
		System.out.println(daoTrasf.�AttivoTrasferimento("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n �Attivo() : cliente non registrato (tizio)");
		System.out.println(daoTrasf.�AttivoTrasferimento("tizio"));
		
		
		System.out.println("\n �Attivo() : cliente disattivo (hotmail)");
		System.out.println(daoTrasf.�AttivoTrasferimento("ivanlamparelli@hotmail.it"));
		
		
		System.out.println("\n---\n---");
		
						
		System.out.println("\n impostaAmb() cliente non attivo (hotmail)");
		ambientiFiltri = new LinkedList<String>();
		ambientiFiltri.add("granaio divella bari 2");
		ambientiFiltri.add("monticchio");
		System.out.println(daoTrasf.impostaAmbienti("ivanlamparelli@hotmail.it", ambientiFiltri));
		
		
		System.out.println("\n mostraAmb() cliente attivo, default con tutti gli ambienti (gmail)");
		try {
			ambientiStampa = daoTrasf.mostraAmbientiFiltrati("lamparelli.ivan@gmail.com");
			for (String amb : ambientiStampa) {
				System.out.print(amb + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n impostaAmb() cliente attivo, ambienti validi (gmail) [granaio 2, monticchio]");
		System.out.println(daoTrasf.impostaAmbienti("lamparelli.ivan@gmail.com", ambientiFiltri));
		
					
		System.out.println("\n mostraAmb() cliente attivo (gmail)");
		try {
			ambientiStampa = daoTrasf.mostraAmbientiFiltrati("lamparelli.ivan@gmail.com");
			for (String amb : ambientiStampa) {
				System.out.print(amb + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n mostraAmb() cliente disattivo (hotmail)");
		try {
			ambientiStampa = daoTrasf.mostraAmbientiFiltrati("ivanlamparelli@hotmail.it");
			for (String amb : ambientiStampa) {
				System.out.print(amb + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n impostaAmb() cliente attivo, ambienti NON validi (gmail) [luogo]");
		ambientiFiltri = new LinkedList<String>();
		ambientiFiltri.add("luogo");
		System.out.println(daoTrasf.impostaAmbienti("lamparelli.ivan@gmail.com", ambientiFiltri));
		
		
		System.out.println("\n mostraAmb() cliente attivo con ambienti NON validi (gmail)");
		try {
			ambientiStampa = daoTrasf.mostraAmbientiFiltrati("lamparelli.ivan@gmail.com");
			for (String amb : ambientiStampa) {
				System.out.print(amb + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n impostaAmb() cliente attivo a cui d� NESSUN ambiente (gmail)");
		ambientiFiltri = new LinkedList<String>();
		System.out.println(daoTrasf.impostaAmbienti("lamparelli.ivan@gmail.com", ambientiFiltri));
		
		
		System.out.println("\n mostraAmb() cliente attivo con ZERO ambienti (gmail)");
		try {
			ambientiStampa = daoTrasf.mostraAmbientiFiltrati("lamparelli.ivan@gmail.com");
			for (String amb : ambientiStampa) {
				System.out.print(amb + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
			
		
		System.out.println("\n\n\n---\n\n\n");
		

		System.out.println("\n �AttivaTerza() cliente non registrato (tizio)");
		System.out.println(daoTrasf.�AttivaTerzaParte("ivanlamparelli@hotmail.it"));
		

		System.out.println("\n �AttivaTerza() cliente con trasf non attivo (hotmail)");
		System.out.println(daoTrasf.�AttivaTerzaParte("tizio"));
		
		
		System.out.println("\n �AttivaTerza() cliente con trasf attivo ma senza inoltro (gmail)");
		System.out.println(daoTrasf.�AttivaTerzaParte("lamparelli.ivan@gmail.com"));

		
		System.out.println("\n attivaTerza() cliente NON esistente (tizio)");
		System.out.println(daoTrasf.impostaTerzaParte("tizio", "terza"));

		
		System.out.println("\n attivaTerza() cliente con trasf NON attivo (hotmail)");
		System.out.println(daoTrasf.impostaTerzaParte("ivanlamparelli@hotmail.it", "terza"));
		
		
		System.out.println("\n attivaTerza() cliente con trasf attivo MA inoltro disattivo (gmail) [terza]");
		System.out.println(daoTrasf.impostaTerzaParte("lamparelli.ivan@gmail.com", "terza"));

		
		System.out.println("\n �AttivaTerza() cliente con inoltro attivo (gmail)");
		System.out.println(daoTrasf.�AttivaTerzaParte("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n mostraTerza() cliente non presente (tizio)");
		try {
			System.out.println(daoTrasf.mostraTerzaParte("tizio"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n mostraTerza() cliente con trasf non attivo (hotmail)");
		try {
			System.out.println(daoTrasf.mostraTerzaParte("ivanlamparelli@hotmail.it"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
 

		System.out.println("\n mostraTerza() cliente con trasf attivo e inoltro attivo (gmail)");
		try {
			System.out.println(daoTrasf.mostraTerzaParte("lamparelli.ivan@gmail.com"));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n disattivaTerza() cliente non presente (tizio)");
		System.out.println(daoTrasf.disattivaTerzaParte("tizio"));


		System.out.println("\n disattivaTerza() cliente con trasf non attivo (hotmail)");
		System.out.println(daoTrasf.disattivaTerzaParte("ivanlamparelli@hotmail.it"));

		
		System.out.println("\n disattivaTerza() cliente con trasferimento e inoltro attivi (gmail)");
		System.out.println(daoTrasf.disattivaTerzaParte("lamparelli.ivan@gmail.com"));

		
		System.out.println("\n disattivaTerza() cliente con trasferimento attivo ma inoltro non attivo (gmail)");
		System.out.println(daoTrasf.disattivaTerzaParte("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n �AttivaTerza() cliente con trasf attivo ma inoltro appena disattivo (gmail)");
		System.out.println(daoTrasf.�AttivaTerzaParte("lamparelli.ivan@gmail.com"));
		
		
		System.out.println("\n private: attivo hotmail");
		daoTrasf.attivaTrasferimento("ivanlamparelli@hotmail.it");
		
		System.out.println("\n mostraClientiTrasf() se ci sono clienti con trasf attivo");
		try {
			clientiTrasf = daoTrasf.mostraClientiConTrasferimentoAttivo();
			for (String cliente : clientiTrasf) {
				System.out.print(cliente + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n private: disattivo hotmail e gmail");
		daoTrasf.disattivaTrasferimento("ivanlamparelli@hotmail.it");
		daoTrasf.disattivaTrasferimento("lamparelli.ivan@gmail.com");
		
		
		System.out.println("\n mostraClientiTrasf() se ci sono clienti registrati, ma NON ci sono clienti con trasf attivo");
		try {
			clientiTrasf = daoTrasf.mostraClientiConTrasferimentoAttivo();
			for (String cliente : clientiTrasf) {
				System.out.print(cliente + ", ");
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		
		
		System.out.println("\n mostraDatiTrasf() per cliente non registrato (tizio)");
		try {
			datiRil = daoTrasf.mostraDatiDaTrasferire("tizio");
			for (DatoRilevazione dato : datiRil) {
				System.out.print(dato.getNomeAmbiente() + ", " + dato.getCodiceSensore() + ", " + dato.getData() + ", " + dato.getValore());
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n mostraDatiTrasf() per cliente registrato con trasf NON attivo (hotmail)");
		try {
			datiRil = daoTrasf.mostraDatiDaTrasferire("ivanlamparelli@hotmail.it");
			for (DatoRilevazione dato : datiRil) {
				System.out.print(dato.getNomeAmbiente() + ", " + dato.getCodiceSensore() + ", " + dato.getData() + ", " + dato.getValore());
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		System.out.println("\n private: riattivo gmail");
		daoTrasf.attivaTrasferimento("lamparelli.ivan@gmail.com");
		
		
		System.out.println("\n mostraDatiTrasf() per cliente con trasf attivo (gmail)");
		try {
			datiRil = daoTrasf.mostraDatiDaTrasferire("lamparelli.ivan@gmail.com");
			for (DatoRilevazione dato : datiRil) {
				System.out.print(dato.getNomeAmbiente() + ", " + dato.getCodiceSensore() + ", " + dato.getData() + ", " + dato.getValore());
				System.out.println();
			}
			System.out.println();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	
	
	
	}
}
