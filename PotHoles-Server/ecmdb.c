#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <postgresql/libpq-fe.h>
#include "ecmdb.h"
#include "ecmprotocol.h"

/*
Effettua la connessione al database.
Nessun parametro può essere NULL.

Parametri:
    PGconn **connection : puntatore per creare la variabile per la connessione al database.
    char *dbUser : puntatore alla stringa contenente l'username per la coneessione al database.
    char *dbPassword : puntatore alla stringa contenente la password per la connessione al database.
    char *dbName : puntatore alla stringa contenente il nome del database.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECMDB_CONNECTION_DB_SUCCESSFUL : connessione al db riuscita.
    ECMDB_CONNECTION_DB_FAIL : connessione al db fallita.
    ECMDB_CONNECTION_DB_NULL_POINTER_ERROR : uno dei puntatori dei parametri è NULL.
    ECMDB_CONNECTION_DB_USER_LEN_ERROR : l'user ha lunghezza 0.
    ECMDB_CONNECTION_DB_PASSWORD_LEN_ERROR : la password utente ha lunhezza 0.
    ECMDB_CONNECTION_DB_NAME_LEN_ERROR : il nome del db ha lunghezza 0.
*/
int ecmdb_connectToDb(PGconn **connection, char *dbUser, char *dbPassword, char *dbName)
{
    if (dbUser == NULL)
        return ECMDB_CONNECTION_DB_NULL_POINTER_ERROR;
    if (dbPassword == NULL)
        return ECMDB_CONNECTION_DB_NULL_POINTER_ERROR;
    if (dbName == NULL)
        return ECMDB_CONNECTION_DB_NULL_POINTER_ERROR;
    if (strlen(dbUser) == 0)
        return ECMDB_CONNECTION_DB_USER_LEN_ERROR;
    if (strlen(dbPassword) == 0)
        return ECMDB_CONNECTION_DB_PASSWORD_LEN_ERROR;
    if (strlen(dbName) == 0)
        return ECMDB_CONNECTION_DB_NAME_LEN_ERROR;

    char *userCmd = "user=";
    char *passwordCmd = "password=";
    char *dbNameCmd = "dbname=";
    char *host = "host=localhost";
    int cmdSize = strlen(userCmd) +
                  strlen(dbUser) +
                  strlen(passwordCmd) +
                  strlen(dbPassword) +
                  strlen(dbNameCmd) +
                  strlen(dbName) +
                  strlen(host);
    char *connectionInfo = (char *)calloc(cmdSize + 4, sizeof(char));

    strcat(connectionInfo, userCmd);
    strcat(connectionInfo, dbUser);
    strcat(connectionInfo, " ");
    strcat(connectionInfo, passwordCmd);
    strcat(connectionInfo, dbPassword);
    strcat(connectionInfo, " ");
    strcat(connectionInfo, dbNameCmd);
    strcat(connectionInfo, dbName);
    strcat(connectionInfo, " ");
    strcat(connectionInfo, host);

    //printf("ConnInfo -> %s\n", connectionInfo);

    PGconn *newConnection = PQconnectdb(connectionInfo);
    if (PQstatus(newConnection) == CONNECTION_BAD)
    {
        //printf("Connessione al db non riuscita -> %s\n", PQerrorMessage(newConnection));
        *connection = NULL;
        PQfinish(newConnection);
        return ECMDB_CONNECTION_DB_FAIL;
    }
    //printf("Connessione al db riuscita -> %s\n", PQerrorMessage(newConnection));
    free(connectionInfo);
    *connection = newConnection;
    return ECMDB_CONNECTION_DB_SUCCESSFUL;
}

/*
Effettua la disconnessione dal database.
Nessun parametro può essere NULL.

Parametri:
    PGconn *connection : puntatore per creare la variabile per la connessione al database.

Ritorno:
    void.
*/
void ecm_closeDbConnection(PGconn *connection){
    PQfinish(connection);
    connection = NULL;
}


/*
Registra un nuovo utente sul db.
Nessun parametro può essere NULL.

Se un utente risulta già registrato restituisce un flag di errore.

Parametri:
    PGconn *connection : puntatore per creare la variabile per la connessione al database.
    ecm_usert_t* : puntatore alla struttura contenente i dati del nuovo utente.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECMDB_REG_USER_SUCCESS : l'utente è stato registrato con successo.
    ECMDB_REG_USER_FAIL : la registrazione dell'utente è fallita.
    ECMDB_REG_CONNECTION_NULL_POINTER_ERRROR : il puntatore alla variabile per la connessione al db è NULL.
    ECMDB_REG_USER_NULL_POINTER_ERROR 230 : il puntatore alla struttura user è NULL.
    ECMDB_REG_USER_GENERIC_ERROR : errore generico.
*/
int ecmdb_registerUser(PGconn *connection, ecm_user_t *user)
{
    if (connection == NULL)
        return ECMDB_REG_CONNECTION_NULL_POINTER_ERRROR;
    if (user == NULL)
        return ECMDB_REG_USER_NULL_POINTER_ERROR;

    //printf("Sono in ecmdb_registerUser.\n");

    char *statement = "INSERT INTO users VALUES ($1, $2, $3, $4, $5)";
    const char *paramValues[5];
    paramValues[0] = user->user;
    paramValues[1] = user->password;
    paramValues[2] = user->email;
    paramValues[3] = user->name;
    paramValues[4] = user->surname;

    PGresult *result = PQexecParams(connection, statement, 5, NULL, paramValues, NULL, 0, 0);
    if (PQresultStatus(result) != PGRES_COMMAND_OK)
    {
        //printf("Errore salvataggio user -> %s $$ %s\n", PQresultErrorMessage(result), PQresultErrorField(result, PG_DIAG_SQLSTATE));
        if (strcmp(PQresultErrorField(result, PG_DIAG_SQLSTATE), "23505") == 0) // Errore utente già esiste.
        {
            PQclear(result);
            return ECMDB_REG_USER_FAIL;
        }
        PQclear(result);
        return ECMDB_REG_USER_GENERIC_ERROR;
    }
    //printf("Salvataggio user riuscito -> %s\n", PQerrorMessage(connection));
    PQclear(result);
    return ECMDB_REG_USER_SUCCESS;
}

/*
Verifica l'esistenza e la password dell'uente.
Nessun parametro può essere NULL.

Se un utente risulta errato restituisce un flag di errore.

Parametri:
    PGconn *connection : puntatore per creare la variabile per la connessione al database.
    ecm_usert_t* : puntatore alla struttura contenente i dati del nuovo utente.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECMDB_LOGIN_USER_SUCCESS : l'utente è stato verificato con successo.
    ECMDB_LOGIN_USER_FAIL : l'utente ha fallito la verifica.
    ECMDB_LOGIN_CONNECTION_NULL_POINTER_ERRROR : il puntatore alla variabile per la connessione al db è NULL.
    ECMDB_LOGIN_USER_NULL_POINTER_ERROR : il puntatore alla struttura user è NULL.
    ECMDB_LOGIN_USER_GENERIC_ERROR : errore generico.
*/
int ecmdb_loginUser(PGconn *connection, ecm_user_t *user)
{
    if (connection == NULL)
        return ECMDB_LOGIN_CONNECTION_NULL_POINTER_ERRROR;
    if (user == NULL)
        return ECMDB_LOGIN_USER_NULL_POINTER_ERROR;

    //printf("Sono in ecmdb_loginUser.\n");

    char *statement = "SELECT * FROM users WHERE user_id=$1 AND user_password=$2";
    const char *paramValues[2];
    paramValues[0] = user->user;
    paramValues[1] = user->password;

    PGresult *result = PQexecParams(connection, statement, 2, NULL, paramValues, NULL, 0, 0);
    if (PQresultStatus(result) != PGRES_TUPLES_OK)
    {
        //printf("Errore nella login -> %s $$ %s\n", PQresultErrorMessage(result), PQresultErrorField(result, PG_DIAG_SQLSTATE));
        PQclear(result);
        return ECMDB_LOGIN_USER_GENERIC_ERROR;
    }
    // if (PQgetvalue(result, 0, 0) == NULL)
    //     return ECMDB_LOGIN_USER_FAIL;
    // return ECMDB_LOGIN_USER_SUCCESS;
    if (PQntuples(result) == 0)
    {
        PQclear(result);
        return ECMDB_LOGIN_USER_FAIL;
    }
    PQclear(result);
    return ECMDB_LOGIN_USER_SUCCESS;
}

/*
Recupera la lista di ECMNearEvent.
Nessun parametro può essere NULL.

Se non è presente nessun evento restituisce un flag apposito.

Parametri:
    PGconn *connection : puntatore per creare la variabile per la connessione al database.
    ecm_usert_t* : puntatore alla struttura contenente i dati del nuovo utente.
    ecm_accelerometer_coords_t *input : puntatore alla struttura contenente le coordinate di riferimento.
    uint32_t *maxRange : puntatore all'intero contenente il raggio di ricerca espresso in Km.
    ecm_accelerometer_coords_t **outputArray : puntatore per la creazione dell'array con le coordinare da inviare.
    uint32_t *arraySize : puntatore alla variabile che contiene il numero di elementi (risultati) contenuti nell'array di risposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECMDB_GET_NEAR_EVENT_SUCCESS : sono stati trovati eventi.
    ECMDB_GET_NEAR_EVENT_FAIL : non sono stati trovati eventi
    ECMDB_GET_NEAR_EVENT_CONNECTION_NULL_POINTER_ERRROR : il puntatore alla variabile per la connessione al db è NULL.
    ECMDB_GET_NEAR_EVENT_USER_NULL_POINTER_ERROR : il puntatore alla struttura user è NULL.
    ECMDB_GET_NEAR_EVENT_INPUT_COORDS_NULL_POINTER_ERROR : il puntatore alla struttura contenente le coordinate di riferimento è NULL.
    ECMDB_GET_NEAR_EVENT_OUTPUT_ARRAY_NULL_POINTER_ERROR : il puntatore per l'array di risposta delle coordinate è NULL.
    ECMDB_GET_NEAR_EVENT_ARRAY_SIZE_NULL_POINTER_ERROR : il puntatore all'intero per il numero di elementi (risultati) contenuti nell'array di risposta è NULL.
    ECMDB_GET_NEAR_EVENT_GENERIC_ERROR : errore generico.
*/
int ecmdb_getNearEvent(PGconn *connection, ecm_user_t *user, ecm_accelerometer_coords_t *input, uint32_t *maxRange, ecm_accelerometer_coords_t **outputArray, uint32_t *arraySize)
{
    if (connection == NULL)
        return ECMDB_GET_NEAR_EVENT_CONNECTION_NULL_POINTER_ERRROR;
    if (user == NULL)
        return ECMDB_GET_NEAR_EVENT_USER_NULL_POINTER_ERROR;
    if (input == NULL)
        return ECMDB_GET_NEAR_EVENT_INPUT_COORDS_NULL_POINTER_ERROR;
    if (outputArray == NULL)
        return ECMDB_GET_NEAR_EVENT_OUTPUT_ARRAY_NULL_POINTER_ERROR;
    if (arraySize == NULL)
        return ECMDB_GET_NEAR_EVENT_ARRAY_SIZE_NULL_POINTER_ERROR;

    char *statement = "SELECT h.latitude, h.longitude, h.accelerometer FROM hole_location AS h WHERE calculate_distance_between_coords($1, $2, h.latitude, h.longitude) <= $3";

    char latitudeStr[20];
    char longitudeStr[20];
    char maxRangeStr[20];
    const char *paramValues[3];
    sprintf(latitudeStr, "%lf", input->latitude);
    sprintf(longitudeStr, "%lf", input->longitude);
    sprintf(maxRangeStr, "%u", (unsigned int)*maxRange);
    paramValues[0] = latitudeStr;
    paramValues[1] = longitudeStr;
    paramValues[2] = maxRangeStr;

    PGresult *result = PQexecParams(connection, statement, 3, NULL, paramValues, NULL, NULL, 0);
    if (PQresultStatus(result) != PGRES_TUPLES_OK)
    {
        //printf("Errore nel trovare eventi vicini -> %s $$ %s\n", PQresultErrorMessage(result), PQresultErrorField(result, PG_DIAG_SQLSTATE));
        return ECMDB_GET_NEAR_EVENT_GENERIC_ERROR;
    }

    int rowNumber = PQntuples(result);
    if (rowNumber == 0)
        return ECMDB_GET_NEAR_EVENT_FAIL;

    ecm_accelerometer_coords_t *array = (ecm_accelerometer_coords_t *)calloc(rowNumber, sizeof(ecm_accelerometer_coords_t));
    for (int i = 0; i < rowNumber; i++)
    {
        //printf("%lf - %lf - %lf\n", atof(PQgetvalue(result, i, 0)), atof(PQgetvalue(result, i, 1)), atof(PQgetvalue(result, i, 2)));
        array[i].latitude = atof(PQgetvalue(result, i, 0));
        array[i].longitude = atof(PQgetvalue(result, i, 1));
        array[i].accelerometer = atof(PQgetvalue(result, i, 2));
    }
    *outputArray = array;
    *arraySize = rowNumber;
    return ECMDB_GET_NEAR_EVENT_SUCCESS;
}

/*
Recupera la lista di ECMNearEvent.
Nessun parametro può essere NULL.

Se non è presente nessun evento restituisce un flag apposito.

Parametri:
    PGconn *connection : puntatore per creare la variabile per la connessione al database.
    ecm_usert_t* : puntatore alla struttura contenente i dati del nuovo utente.
    ecm_accelerometer_coords_t *input : puntatore alla struttura contenente le coordinate di riferimento.
    uint32_t *maxRange : puntatore all'intero contenente il raggio di ricerca espresso in Km.
    ecm_accelerometer_coords_t **outputArray : puntatore per la creazione dell'array con le coordinare da inviare.
    uint32_t *arraySize : puntatore alla variabile che contiene il numero di elementi (risultati) contenuti nell'array di risposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECMDB_SAVE_COORDS_SUCCESS : le coordinate sono state salvate con successo.
    ECMDB_SAVE_COORDS_FAIL : il salvataggio delle coordinate è fallito.
    ECMDB_SAVE_COORDS_CONNECTION_NULL_POINTER_ERRROR : il puntatore alla variabile per la connessione al db è NULL.
    ECMDB_SAVE_COORDS_USER_NULL_POINTER_ERROR : il puntatore alla struttura user è NULL.
    ECMDB_SAVE_COORDS_ARRAY_NULL_POINTER_ERROR : il puntatore all'array di coordinate da salvare è NULL.
    ECMDB_SAVE_COORDS_ARRAY_SIZE_NULL_POINTER_ERROR : il puntatore al numero di elementi dell'arrau è NULL.
*/
int ecmdb_saveCoords(PGconn *connection, ecm_user_t *user, ecm_accelerometer_coords_t *array, uint32_t *arraySize)
{
    if (connection == NULL)
        return ECMDB_SAVE_COORDS_CONNECTION_NULL_POINTER_ERRROR;
    if (user == NULL)
        return ECMDB_SAVE_COORDS_USER_NULL_POINTER_ERROR;
    if (array == NULL)
        return ECMDB_SAVE_COORDS_ARRAY_NULL_POINTER_ERROR;
    if (arraySize == NULL)
        return ECMDB_SAVE_COORDS_ARRAY_SIZE_NULL_POINTER_ERROR;

    for (uint32_t i = 0; i < (*arraySize); i++)
    {
        char *statement = "INSERT INTO hole_location VALUES ($1, $2, $3, $4)";

        char latitudeStr[20];
        char longitudeStr[20];
        char accelerometerStr[20];
        const char *paramValues[4];
        sprintf(latitudeStr, "%lf", array[i].latitude);
        sprintf(longitudeStr, "%lf", array[i].longitude);
        sprintf(accelerometerStr, "%lf", array[i].accelerometer);
        paramValues[0] = user->user;
        paramValues[1] = latitudeStr;
        paramValues[2] = longitudeStr;
        paramValues[3] = accelerometerStr;

        PGresult *result = PQexecParams(connection, statement, 4, NULL, paramValues, NULL, NULL, 0);
        if (PQresultStatus(result) != PGRES_COMMAND_OK)
        {
            //printf("Errore salvataggio eventi -> %s $$ %s\n", PQresultErrorMessage(result), PQresultErrorField(result, PG_DIAG_SQLSTATE));
            return ECMDB_SAVE_COORDS_FAIL;
        }
    }

    return ECMDB_SAVE_COORDS_SUCCESS;
}