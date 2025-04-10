/*
ECM Protocol V2
Enhanced Coordinate Messaging Protocol
Protocollo migliorato di messaggistica delle coordinate
*/

#include "ecmprotocol.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/********** RECEIVE **********/

/*
Prepara la struttura ausiliaria per la registrazione di un nuovo utente.
Nessun parametro può essere NULL.

Parametri:
    char *residualBuffer : puntatore per il buffer residuo con i dati da recuperare.
    ecm_auxiliary_data_t **auxData : puntatore per la struttura dati ausiliaria.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_REQUEST_USR_REG : codice identificativo per la richiesta di registrazione di un nuovo utente.
*/
static int _ecm_userRegistration(char *residualBuffer, ecm_auxiliary_data_t *auxData)
{
    char *savePointer;

    auxData->user = (ecm_user_t *)calloc(1, sizeof(ecm_user_t));
    auxData->user->user = strtok_r(residualBuffer, "\n", &savePointer);
    auxData->user->password = strtok_r(NULL, "\n", &savePointer);
    auxData->user->email = strtok_r(NULL, "\n", &savePointer);
    auxData->user->name = strtok_r(NULL, "\n", &savePointer);
    auxData->user->surname = strtok_r(NULL, "\n", &savePointer);

    return ECM_REQUEST_USR_REG;
}

/*
Prepara la struttura ausiliaria per il login di un utente.
Nessun parametro può essere NULL.

Parametri:
    char *residualBuffer : puntatore per il buffer residuo con i dati da recuperare.
    ecm_auxiliary_data_t **auxData : puntatore per la struttura dati ausiliaria.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_REQUEST_LOGIN : codice identificativo per la richiesta di login di un utente.
*/
static int _ecm_userLogin(char *residualBuffer, ecm_auxiliary_data_t *auxData)
{
    char *savePointer;

    auxData->user = (ecm_user_t *)calloc(1, sizeof(ecm_user_t));
    auxData->user->user = strtok_r(residualBuffer, "\n", &savePointer);
    auxData->user->password = strtok_r(NULL, "\n", &savePointer);

    return ECM_REQUEST_LOGIN;
}

/*
Prepara la struttura ausiliaria per una richiesta near event.
Nessun parametro può essere NULL.

Parametri:
    char *residualBuffer : puntatore per il buffer residuo con i dati da recuperare.
    ecm_auxiliary_data_t **auxData : puntatore per la struttura dati ausiliaria.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_REQUEST_NEAR_EVENT : codice identificativo per la richiesta near event.
*/
static int _ecm_nearEvent(char *residualBuffer, ecm_auxiliary_data_t *auxData)
{
    auxData->maxRange = (uint32_t *)calloc(1, sizeof(uint32_t));
    memcpy(auxData->maxRange, residualBuffer, sizeof(uint32_t));
    auxData->coordsArray = (ecm_accelerometer_coords_t *)calloc(1, sizeof(ecm_accelerometer_coords_t));
    memcpy(auxData->coordsArray, residualBuffer + sizeof(uint32_t), sizeof(ecm_accelerometer_coords_t));
    auxData->coordsArrayLength = (uint32_t *)calloc(1, sizeof(uint32_t));
    *(auxData->coordsArrayLength) = 1;

    return ECM_REQUEST_NEAR_EVENT;
}

/*
Prepara la struttura ausiliaria per il salvataggio di coordinate.
Nessun parametro può essere NULL.
Attenzione: residualBuffer deve contenere almeno una coordinata da salvare.

Parametri:
    char *residualBuffer : puntatore per il buffer residuo con i dati da recuperare.
    ecm_auxiliary_data_t **auxData : puntatore per la struttura dati ausiliaria.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_REQUEST_SAVE_COORDS : codice identificativo per la richiesta di salvataggio coordinate.S
*/
static int _ecm_saveCoords(char *residualBuffer, ecm_auxiliary_data_t *auxData)
{
    auxData->coordsArrayLength = (uint32_t *)calloc(1, sizeof(uint32_t));
    memcpy(auxData->coordsArrayLength, residualBuffer, sizeof(uint32_t));
    auxData->coordsArray = (ecm_accelerometer_coords_t *)calloc(*(auxData->coordsArrayLength), sizeof(ecm_accelerometer_coords_t));
    memcpy(auxData->coordsArray, residualBuffer + sizeof(uint32_t), *(auxData->coordsArrayLength) * sizeof(ecm_accelerometer_coords_t));
    return ECM_REQUEST_SAVE_COORDS;
}

/*
Decodifica il buffer in entrata, prepara la struttura ausiliaria con i dati e restituisce il codice di richiesta.
Il parametro incomeBuffer non può essere NULL.

Parametri:
    char *incomeBuffer : puntatore al buffer contenente i dati ricevuti.
    ecm_auxiliary_data_t **auxData : puntatore per la struttra dati ausiliaria.

Ritorno:
    ECM_DECODE_RECEIVED_REQUEST_NOT_VALID : codice identificativo per la ricezione di un comando non valido.
    ECM_DECODE_PROTOCOL_NOT_VALID : codice identificatico per la ricezione di un pacchetto con protocollo non valido.
    ECM_DECODE_ERROR : codice identificativo per l'errore generico nella decodifica.
    ECM_DECODE_NULL_POINTER_ERROR : codice identificativo per l'errore di puntatore nullo.
    ECM_REQUEST_CHECK : codice identificativo per il controllo di diponibilità del server.
    ECM_REQUEST_USR_REG : codice identificativo per la richiesta di registrazione di un nuovo utente.
    ECM_REQUEST_LOGIN : codice identificativo per la richiesta di login di un utente.
    ECM_REQUEST_NEAR_EVENT : codice identificativo per la richiesta near event.
    ECM_REQUEST_SAVE_COORDS : codice identificativo per la richiesta di salvataggio coordinate.
    ECM_REQUEST_ECHO : codice identificativo per la richiesta echo reply.
    ECM_CONN_CLOSE_ANNOUNCEMENT : codice identificativo per l'annuncio di chiusura della connessione.
    ECM_RECEIVED_ECHO_RESPONSE : codice identificativo per il ricevimento della risposta echo.
*/
int ecm_incomeDecoder(char *incomeBuffer, ecm_auxiliary_data_t *auxData)
{
    if (incomeBuffer == NULL)
        return ECM_DECODE_NULL_POINTER_ERROR;
    if (auxData == NULL)
        return ECM_DECODE_NULL_POINTER_ERROR;

    char *savePointer;
    char *request;

    request = strtok_r(incomeBuffer, "\n", &savePointer);

    // Check
    if (strcmp(request, "CHECK") == 0)
        return ECM_REQUEST_CHECK;

    // User registration
    if (strcmp(request, "USR_REG_REQUEST") == 0)
        return _ecm_userRegistration(savePointer, auxData);

    // User login
    if (strcmp(request, "LOGIN_REQUEST") == 0)
        return _ecm_userLogin(savePointer, auxData);

    // Near event
    if (strcmp(request, "NEAR_EVENT_REQUEST") == 0)
        return _ecm_nearEvent(savePointer, auxData);

    // Accelerometer thereshold
    if (strcmp(request, "ACCELEROMETER_REQUEST") == 0)
        return ECM_REQUEST_ACCELETOMETER_THRESHOLD;

    // Save coords
    if (strcmp(request, "SAVE_COORDS_REQUEST") == 0)
        return _ecm_saveCoords(savePointer, auxData);

    // Echo
    if (strcmp(request, "ECHO_REQUEST") == 0)
        return ECM_REQUEST_ECHO;

    // Echo received
    if (strcmp(request, "ECHO_REPLY") == 0)
        return ECM_RECEIVED_ECHO_RESPONSE;

    // Close connection
    if (strcmp(request, "CONN_CLOSE_ANNOUNCEMENT") == 0)
        return ECM_CONN_CLOSE_ANNOUNCEMENT;

    return ECM_DECODE_ERROR;
}

/********** SEND **********/

/*
Prepara il buffer di risposta a una richiesta CHECK.
Nessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.
    int reponseCode : codice che indica il tipo stringa di riposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di responso:
    ECM_SEND_CHECK_RESPONSE_CODE_OK : risponde che il check è positivo.
    ECM_SEND_CHECK_RESPONSE_CODE_FAIL : risponde che il check è negativo.

Codici di ritorno:
    ECM_SEND_CHECK_NULL_POINTER_ERROR : il puntatore del buffer è NULL.
    ECM_SEND_CHECK_CODE_ERROR : è stato passato un codice di risposta non valido.
*/
int ecm_checkResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_CHECK_NULL_POINTER_ERROR;

    uint32_t payloadSize = 0;

    switch (responseCode)
    {
    case ECM_SEND_CHECK_RESPONSE_CODE_OK:
        payloadSize = 9;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "CHECK_OK\n", 9);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    case ECM_SEND_CHECK_RESPONSE_CODE_FAIL:
        payloadSize = 11;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "CHECK_FAIL\n", 11);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    default:
        return ECM_SEND_CHECK_CODE_ERROR;
    }

    return ECM_SEND_CHECK_OK;
}

/*
Prepara il buffer di risposta a una richiesta USR_REG.
Nessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.
    int reponseCode : codice che indica il tipo stringa di riposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di responso:
    ECM_SEND_USR_REG_RESPONSE_CODE_OK : risponde che la registrazione ha dato positivo.
    ECM_SEND_USR_REG_RESPONSE_CODE_FAIL : risponde che la registrazione ha dato negativo.

Codici di ritorno:
    ECM_SEND_USR_REG_NULL_POINTER_ERROR : il puntatore del buffer è NULL.
    ECM_SEND_USR_REG_CODE_ERROR : è stato passato un codice di risposta non valido.
*/
int ecm_userRegistrationResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_USR_REG_NULL_POINTER_ERROR;

    int payloadSize = 0;

    switch (responseCode)
    {
    case ECM_SEND_USR_REG_RESPONSE_CODE_OK:
        payloadSize = 11;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "USR_REG_OK\n", 11);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    case ECM_SEND_USR_REG_RESPONSE_CODE_FAIL:
        payloadSize = 13;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "USR_REG_FAIL\n", 13);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    default:
        return ECM_SEND_USR_REG_CODE_ERROR;
    }

    return ECM_SEND_USR_REG_OK;
}

/*
Prepara il buffer di risposta a una richiesta di LOGIN.
Nuessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.
    int reponseCode : codice che indica il tipo stringa di riposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di responso:
    ECM_SEND_LOGIN_RESPONSE_CODE_OK : risponde che il login è positivo.
    ECM_SEND_LOGIN_RESPONSE_CODE_FAIL : risponde che il login è negativo.

Codici di ritorno:
    ECM_SEND_LOGIN_NULL_POINTER_ERROR : il puntatore dell buffer è NULL.
    ECM_SEND_LOGIN_CODE_ERROR : è stato passato un codice di risposta non valido.
*/
int ecm_loginResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_LOGIN_NULL_POINTER_ERROR;

    int payloadSize = 0;

    switch (responseCode)
    {
    case ECM_SEND_LOGIN_RESPONSE_CODE_OK:
        payloadSize = 9;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "LOGIN_OK\n", 9);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    case ECM_SEND_LOGIN_RESPONSE_CODE_FAIL:
        payloadSize = 11;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE, +sizeof(uint32_t) + payloadSize);
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "LOGIN_FAIL\n", 11);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    case ECM_SEND_LOGIN_RESPONSE_CODE_USR_NOT_LOGGED:
        payloadSize = 15;
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE, +sizeof(uint32_t) + payloadSize);
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "USR_NOT_LOGGED\n", 15);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    default:
        return ECM_SEND_LOGIN_CODE_ERROR;
    }

    return ECM_SEND_LOGIN_OK;
}

/*
Prepara il buffer di risposta a una richiesta di NEAR EVENT.
Il parametro "*array" può essere NULL solo se il codice di risposta è ECM_SEND_LOGIN_RESPONSE_CODE_FAIL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.
    ecm_accelerometer_coords_t *array : puntatore all'array di strutture ecm_accelerometer_coords_t.
    uint32_t arraySize : dimensione dell'array di strutture ecm_accelerometer_coords_t.
    int reponseCode : codice che indica il tipo stringa di riposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di responso:
    ECM_SEND_NEAR_EVENT_RESPONSE_CODE_REPLY : risponde che sono presenti eventi vicini.
    ECM_SEND_NEAR_EVENT_RESPONSE_CODE_NOT_FOUND : risponde che sono non presenti eventi vicini.

Codici di ritorno:
    ECM_SEND_NEAR_EVENT_NULL_POINTER_ERROR : il puntatore dell buffer è NULL.
    ECM_SEND_NEAR_EVENT_CODE_ERROR : è stato passato un codice di risposta non valido.
    ECM_SEND_NEAR_EVENT_EMPTY_ARRAY_ERROR : la dimensione dell'array, passata come parametro, non è valida.
*/
int ecm_nearEventResponse(char **responseBuffer, uint32_t *bufferSize, ecm_accelerometer_coords_t *array, uint32_t arraySize, int responseCode)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_NEAR_EVENT_NULL_POINTER_ERROR;
    if ((array == NULL) && (responseCode == ECM_SEND_NEAR_EVENT_RESPONSE_CODE_REPLY))
        return ECM_SEND_NEAR_EVENT_NULL_POINTER_ERROR;
    if ((arraySize == 0) && (responseCode == ECM_SEND_NEAR_EVENT_RESPONSE_CODE_REPLY))
        return ECM_SEND_NEAR_EVENT_EMPTY_ARRAY_ERROR;

    int payloadSize = 0;

    switch (responseCode)
    {
    case ECM_SEND_NEAR_EVENT_RESPONSE_CODE_REPLY:
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 17 + sizeof(uint32_t) + (sizeof(ecm_accelerometer_coords_t) * arraySize), sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        payloadSize = 17 + sizeof(uint32_t) + (sizeof(ecm_accelerometer_coords_t) * arraySize);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "NEAR_EVENT_REPLY\n", 17);
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 17, &arraySize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 17 + sizeof(uint32_t), array, (sizeof(ecm_accelerometer_coords_t) * arraySize));
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    case ECM_SEND_NEAR_EVENT_RESPONSE_CODE_NOT_FOUND:
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 21, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        payloadSize = 21;
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint16_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "NEAR_EVENT_NOT_FOUND\n", 21);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    default:
        return ECM_SEND_NEAR_EVENT_CODE_ERROR;
    }

    return ECM_SEND_NEAR_EVENT_OK;
}

/*
Prepara il buffer di risposta a una richiesta ACCELEROMETER_THRESHOLD.
Nessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.
    double accelerometerThreshold : valore soglia dell'accelerometro da inviare al client.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_SEND_ACCELEROMETER_THRESHOLD_OK : risponde che la preparazione del buffer ha avuto esito positivo.
    ECM_SEND_ACCELEROMETER_THRESHOLD_NULL_POINTER_ERROR : il puntatore del buffer è nullo.
*/
int ecm_accelerometerThresholdResponse(char **responseBuffer, uint32_t *bufferSize, double accelerometerThreshold)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_ACCELEROMETER_THRESHOLD_NULL_POINTER_ERROR;

    int payloadSize = 23 + sizeof(double);

    *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize, sizeof(char));
    memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "ACCELEROMETER_RESPONSE\n", 23);
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 23, &accelerometerThreshold, sizeof(double));
    *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;

    return ECM_SEND_ACCELEROMETER_THRESHOLD_OK;
}

/*
Prepara il buffer di risposta a una richiesta SAVE_COORDS.
Nessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.
    int reponseCode : codice che indica il tipo stringa di riposta.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di responso:
    ECM_SEND_SAVE_COORDS_RESPONSE_CODE_OK : risponde che il salvataggio ha dato esito positivo.
    ECM_SEND_SAVE_COORDS_RESPONSE_CODE_FAIL : risponde che il salvataggio ha dato esito negativo.

Codici di ritorno:
    ECM_SEND_SAVE_COORDS_NULL_POINTER_ERROR : il puntatore del buffer è NULL.
    ECM_SEND_SAVE_COORDS_CODE_ERROR : è stato passato un codice di risposta non valido.
*/
int ecm_saveCoordinatesResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_SAVE_COORDS_NULL_POINTER_ERROR;

    int payloadSize = 0;

    switch (responseCode)
    {
    case ECM_SEND_SAVE_COORDS_RESPONSE_CODE_OK:
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 15, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        payloadSize = 15;
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "SAVE_COORDS_OK\n", 15);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    case ECM_SEND_SAVE_COORDS_RESPONSE_CODE_FAIL:
        *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 15, sizeof(char));
        memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
        payloadSize = 17;
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
        memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "SAVE_COORDS_FAIL\n", 17);
        *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
        break;

    default:
        return ECM_SEND_SAVE_COORDS_CODE_ERROR;
    }

    return ECM_SEND_SAVE_COORDS_OK;
}

/*
Prepara il buffer di risposta a una richiesta CONN_CLOSE_ANNOUNCEMENT.
Nessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_SEND_CLOSE_CONN_NULL_POINTER_ERROR : il puntatore del buffer è NULL.
    ECM_SEND_CLOSE_CONN_OK : il buffer è stato preparato correttamente.
*/
int ecm_closeConnectionResponse(char **responseBuffer, uint32_t *bufferSize)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_CLOSE_CONN_NULL_POINTER_ERROR;

    int payloadSize = 0;

    *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 15, sizeof(char));
    memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
    payloadSize = 14;
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(int));
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "CONN_CLOSE_OK\n", 14);
    *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
    return ECM_SEND_CLOSE_CONN_OK;
}

/*
Prepara il buffer di risposta a una richiesta ECHO_REQUEST.
Nessun parametro può essere NULL.

Parametri:
    char **responseBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_SEND_ECHO_REPLY_NULL_POINTER_ERROR : il puntatore del buffer è NULL.
    ECM_SEND_ECHO_REPLY_OK : il buffer è stato preparato correttamente.
*/
int ecm_echoReplyResponse(char **responseBuffer, uint32_t *bufferSize)
{
    if ((responseBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_ECHO_REPLY_NULL_POINTER_ERROR;

    int payloadSize = 0;

    *responseBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 15, sizeof(char));
    memcpy(*responseBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
    payloadSize = 11;
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
    memcpy((*responseBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "ECHO_REPLY\n", 11);
    *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
    return ECM_SEND_ECHO_REPLY_OK;
}

/*
Prepara il buffer di risposta a una richiesta ECHO_REQUEST.
Nessun parametro può essere NULL.

Parametri:
    char **sendBuffer : puntatore per il buffer di risposta.
    uint_32 *buffersize : puntatore alla variabile per la specifica della dimensione del buffer.

Ritorno:
    int : indica lo stato di uscita della funzione.

Codici di ritorno:
    ECM_SEND_ECHO_REQUEST_NULL_POINTER_ERROR : il puntatore del buffer è NULL.
    ECM_SEND_ECHO_REQUEST_OK : il buffer è stato preparato correttamente.
*/
int ecm_echoRequest(char **sendBuffer, uint32_t *bufferSize)
{
    if ((sendBuffer == NULL) || (bufferSize == NULL))
        return ECM_SEND_ECHO_REQUEST_NULL_POINTER_ERROR;

    int payloadSize = 0;

    *sendBuffer = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + 15, sizeof(char));
    memcpy(*sendBuffer, ECM_PROTOCOL_HEADING, ECM_PROTOCOL_HEADING_SIZE);
    payloadSize = 13;
    memcpy((*sendBuffer) + ECM_PROTOCOL_HEADING_SIZE, &payloadSize, sizeof(uint32_t));
    memcpy((*sendBuffer) + ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t), "ECHO_REQUEST\n", 13);
    *bufferSize = ECM_PROTOCOL_HEADING_SIZE + sizeof(uint32_t) + payloadSize;
    return ECM_SEND_ECHO_REQUEST_OK;
}

/***********************************/

/*
Restituisce il flag che indica il tipo di endian del sistema.
Ritorna valore del flag in caso di succeso.
Ritorna -1 in caso di errore.
*/
// static int _ecm_getByteOrderString() {
//     switch (__BYTE_ORDER) {
//         case __LITTLE_ENDIAN:
//             return ECM_LITTLE_ENDIAN;
//         case __BIG_ENDIAN:
//             return ECM_BIG_ENDIAN;
//         case __PDP_ENDIAN:
//             return ECM_PDP_ENDIAN;
//         default:
//             return -1;
//     }

//     // if (__BYTE_ORDER == __LITTLE_ENDIAN)
//     //     return ECM_LITTLE_ENDIAN;
//     // if (__BYTE_ORDER == __BIG_ENDIAN)
//     //     return ECM_BIG_ENDIAN;
//     // if (__BYTE_ORDER == __PDP_ENDIAN)
//     //     return ECM_PDP_ENDIAN;
//     // else
//     //     return NULL;
// }