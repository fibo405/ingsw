# Channel Manager
Channel Manager is the core of a domotic system named Her.

## Setup sistema
- Avviare Postgres
- Avviare Docker
- Da terminale, nella cartella del progetto:
  - Creare il file jar con: `mvn -Dmaven.test.skip=true install`
  - Creare il setup su docker con: `docker-compose up`

## Demo del sistema
Per testare l'autenticazione con token:

1) Effettuare una chiamata POST a localhost:8080/authenticate, inserendo nel body:
```
{
"username":"uno_username",
"password":"una_password"
}
```
Le credenziali disponibili sono:
| Username | Password |
|:--------:|:--------:|
|tizio|pass1|
|caio|pass2|

Questa chiamata controlla nel db la presenza di un utente con le credenziali inserite.
In output, si riceve un token di autenticazione.

2) Effettuare una chiamata GET a localhost:8080/hello, inserendo negli header:
```
key: Authorization
value: Bearer my_jwt_token
```
In output, si riceve una view con il messaggio "Hello World!"

3) Effettuare una chiamata POST a localhost:8080/authenticate, inserendo nel body dei dati invalidi:
```
{
"username":"bad_username",
"password":"bad_password"
}
```
In output, si riceve un messaggio 401-Unauthorized

4) Effettuare una chiamata GET a localhost:8080/hello, inserendo un token errato nell'header (o nessun token):
```
key: Authorization
value: Bearer bad_token
```
In output non si riceve niente

## Pulizia sistema
Per ripulire il setup su docker:
Da terminale, nella cartella del progetto, digitare: `docker-compose down --rmi local -v`
