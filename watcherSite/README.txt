
Requisiti per eseguire il progetto:
-Scaricare e installare xampp
-Dopo l'installazione avviare i server apache e mysql
-Cliccare su Admin di mysql e importare il db presente nella cartella db della root del progetto

-Nella root del progetto aprire la cartella db
-Copiare la cartella watcher in C:/



Per configurare il server:
-Avviare eclipse
-Selezionare window -> show view -> servers
-Selezionare new server -> apache/tomcat 7.0
-Installarlo nella cartella C:/Xampp/tomcat
-Aggiungere il progetto al server

NB: Se eclipse viene chiuso, con conseguente arresto di Tomcat, il progetto non si avvierà



Per avviare il progetto:
-Avviare il server tomcat su eclipse
-Aprire il browser e digitare il seguente link http://localhost:8080/watcherSite/index.jsp
Per l'accesso da altri dispositivi sulla stessa rete wifi:
-Controllare dal prompt con "ipconfig" l'IPv4 del pc, e digitare il link con l'ip al posto di "localhost" (lasciando :8080)