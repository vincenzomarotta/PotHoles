#include <arpa/inet.h>
#include <errno.h>
#include <netinet/ip.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <unistd.h>
#include <signal.h>
#include <fcntl.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <poll.h>
#include "ecmprotocol.h"
#include "ecmdb.h"
#include "threadpool.h"
#include "ecmdb.h"

#define DB_USER "postgres"
//#define DB_PASS "prova"
#define DB_PASS "CiaoCiao.0000"
#define DB_NAME "PotHoleDb"
#define TIMEOUT_CONNECTION 6000

typedef struct clientDescription_t
{
    int clientFileDescriptor;
    struct sockaddr_in clientAddress;
    socklen_t clientSocketlength;
} clientDescription_t;

void checkArgs(int argc, char *argv[]);
void closeServerHandler(int);
void manageClient(void *args);
static void _printMessageStdOutput(time_t *currentTime, char *message, char *ipString);

pthread_mutex_t stdoutMutex = PTHREAD_MUTEX_INITIALIZER;

int runServer = 1;
int numberOfParallelThreads = 4;
int workQueueSize = 20;
double accelerometerThreshold = 10.0;

int main(int argc, char *argv[])
{
    checkArgs(argc, argv);

    time_t currentTime;
    time(&currentTime);

    fprintf(stdout, "**************************************************\n");
    fprintf(stdout, "*                                                *\n");
    fprintf(stdout, "*                POTHOLE SERVER                  *\n");
    fprintf(stdout, "*               with ECM_Protocol                *\n");
    fprintf(stdout, "*                                                *\n");
    fprintf(stdout, "**************************************************\n\n");
    fprintf(stdout, " Starting time: %s\n\n", ctime(&currentTime));
    fprintf(stdout, "   Current settings:\n");
    fprintf(stdout, "       -> Number of parallel threads: %d\n", numberOfParallelThreads);
    fprintf(stdout, "       -> Queue size: %d\n", workQueueSize);
    fprintf(stdout, "       -> Accelerometer threshold: %.2lf\n\n", accelerometerThreshold);
    fprintf(stdout, "--------------------------------------------------\n\n");
    fprintf(stdout, "LOG:\n\n");

    // Gestico i segnali
    signal(SIGINT, closeServerHandler);
    signal(SIGTERM, closeServerHandler);

    // Creo la socket
    int socketDescriptor = socket(PF_INET, SOCK_STREAM, 0);
    if (socketDescriptor < 0)
    {
        time(&currentTime);
        fprintf(stdout, "%s -> Socket creation error.\n\n", ctime(&currentTime));
        exit(EXIT_FAILURE);
    }

    // Setto i dati per l'ascolto
    struct sockaddr_in myAdress;
    myAdress.sin_family = AF_INET;
    myAdress.sin_port = htons(5200);
    myAdress.sin_addr.s_addr = htonl(INADDR_ANY);

    // Eseguo il bind
    if (bind(socketDescriptor, (struct sockaddr *)&myAdress, sizeof(myAdress)) < 0)
    {
        time(&currentTime);
        fprintf(stdout, "%s -> Bind error.\n\n", ctime(&currentTime));
        close(socketDescriptor);
        exit(EXIT_FAILURE);
    }

    // Avvio l'ascolto.
    if (listen(socketDescriptor, 1) < 0)
    {
        time(&currentTime);
        fprintf(stdout, "%s -> Listen error.\n\n", ctime(&currentTime));
        close(socketDescriptor);
        exit(EXIT_FAILURE);
    }

    // Creo il threadpool
    threadpool_t *myPool = threadpool_create(numberOfParallelThreads, workQueueSize, THREADPOOL_RUN);

    // Stampa info.
    time(&currentTime);
    fprintf(stdout, "%s -> Server running...\n\n", ctime(&currentTime));

    // Cliclo principale - accettazione client.
    clientDescription_t *client;
    while (1)
    {
        client = (clientDescription_t *)calloc(1, sizeof(clientDescription_t));
        client->clientSocketlength = sizeof(client->clientAddress);
        client->clientFileDescriptor = accept(socketDescriptor, (struct sockaddr *)&(client->clientAddress), &(client->clientSocketlength));
        char ipStr[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &(client->clientAddress.sin_addr), ipStr, INET_ADDRSTRLEN);
        if ((strcmp(ipStr, "127.0.0.1") == 0) || (strcmp(ipStr, "0.0.0.0") == 0))
        {
            close(client->clientFileDescriptor);
            free(client);
            break;
        }
        time(&currentTime);
        _printMessageStdOutput(&currentTime, "Accepted connection", ipStr);
        if(threadpool_addWork(myPool, manageClient, client) == 0) { //Chiudo connessione in caso di coda piena.
            close(client->clientFileDescriptor);
            free(client);
        }
    }
    threadpool_killAll(&myPool);
    close(socketDescriptor);

    pthread_mutex_destroy(&stdoutMutex);

    return 0;
}

/*
Controlla gli argomenti e setta le variabili globali.
*/
void checkArgs(int argc, char *argv[])
{
    if (argc == 1)
        return;

    if ((argc > 1) && ((argc - 1) % 2 != 0))
    {
        fprintf(stdout, "Invalid number of arguments.\n");
        exit(EXIT_FAILURE);
    }

    for (int i = 1; i < argc; i += 2)
    {
        char *subArg_1 = argv[i];
        char *subArg_2 = argv[i + 1];
        if (strcmp(subArg_1, "-t") == 0)
        {
            int tempThread = atoi(subArg_2);
            if ((tempThread < 1) || (tempThread > 20))
                fprintf(stdout, "Invalid number of parallel thread. System will use default value [%d].\n", numberOfParallelThreads);
            else
                numberOfParallelThreads = tempThread;
            continue;
        }
        else if (strcmp(subArg_1, "-q") == 0)
        {
            int tempQueue = atoi(subArg_2);
            if ((tempQueue < 1) || (tempQueue > 100))
                fprintf(stdout, "Invalid queue size. System will use default value [%d].\n", workQueueSize);
            else
                workQueueSize = tempQueue;
            continue;
        }
        else if (strcmp(subArg_1, "-a") == 0)
        {
            double tempAccelerometer = atof(subArg_2);
            if ((tempAccelerometer <= 0) || (tempAccelerometer > 20))
                fprintf(stdout, "Invalid accelerometer threshold. System will use default value [%lf].\n", accelerometerThreshold);
            else
                accelerometerThreshold = tempAccelerometer;
            continue;
        }
        else
        {
            fprintf(stdout, "Argument not valid -> %s %s.\n", subArg_1, subArg_2);
            continue;
        }
    }
}

/*
Specifica le azioni da compiere quando viene rilevato un segnale di terminazione.
Crea una finta connessione che viene interpretato come flag per uscire dal ciclo while del
thread principale.
*/
void closeServerHandler(int sigID)
{
    time_t currentTime;
    time(&currentTime);
    char message[50] = "Riceived server termination segnal (";
    char id[10];
    sprintf(id, "%d", sigID);
    strcat(message, id);
    strcat(message, ")");
    _printMessageStdOutput(&currentTime, message, "localhost");
    runServer = 0;

    // Connessione fake per chiudere il server uscendo dal while.
    struct sockaddr_in serv_addr;
    int sock = socket(PF_INET, SOCK_STREAM, 0);
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(5200);
    inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr);
    int fd = connect(sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr));
    close(fd);
    close(sock);
}

/*
Funzione che gestisce la comunicazione server-client.
*/
void manageClient(void *args)
{
    // Variabili globali del thread.
    clientDescription_t *client = (clientDescription_t *)args;
    char ipString[INET_ADDRSTRLEN];
    inet_ntop(AF_INET, &(client->clientAddress.sin_addr), ipString, INET_ADDRSTRLEN);
    time_t currentTime;
    int servingClient = 1;
    int dbStatus;
    int userLogged = 0; // Riportare a 0.
    PGconn *dbConnection = NULL;
    ecm_auxiliary_data_t *aux = (ecm_auxiliary_data_t *)calloc(1, sizeof(ecm_auxiliary_data_t));

    // Variabili di ricezione.
    char *protocol = (char *)calloc(ECM_PROTOCOL_HEADING_SIZE, sizeof(char));
    uint32_t payloadSize;
    char *payload;

    // Variabili di invio.
    char *buffer;
    uint32_t bufferSize;
    ecm_accelerometer_coords_t *array;
    uint32_t arraySize;

    //Variabili di timeout
    struct pollfd pollFileDescription;
    pollFileDescription.fd = client->clientFileDescriptor;
    pollFileDescription.events = POLLIN;

    while (runServer && servingClient)
    {
        if(poll(&pollFileDescription, 1, TIMEOUT_CONNECTION) == 0)
        {time(&currentTime);
            _printMessageStdOutput(&currentTime, "Timeout", ipString);
            break;
        }
        if (read(client->clientFileDescriptor, protocol, ECM_PROTOCOL_HEADING_SIZE) != ECM_PROTOCOL_HEADING_SIZE)
        {
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Client send an invalid number of bytes", ipString);
            break;
        }
        if (strcmp(protocol, ECM_PROTOCOL_HEADING) != 0)
        {
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Packet header not valid", ipString);
            break;
        }
        if (read(client->clientFileDescriptor, &payloadSize, sizeof(uint32_t)) != sizeof(uint32_t))
        {
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Client send an invalid number of bytes", ipString);
            break;
        }
        payload = (char *)calloc(payloadSize, sizeof(char));
        if (read(client->clientFileDescriptor, payload, payloadSize) != payloadSize)
        {
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Client send an invalid number of bytes", ipString);
            break;
        }

        int returnCode = ecm_incomeDecoder(payload, aux);
        switch (returnCode)
        {
        case ECM_DECODE_ERROR:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Decode error", ipString);
            break;

        case ECM_DECODE_NULL_POINTER_ERROR:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Decode request error", ipString);
            break;

        case ECM_DECODE_RECEIVED_REQUEST_NOT_VALID:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Request non valid", ipString);
            break;

        case ECM_REQUEST_CHECK:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Check request", ipString);
            ecm_checkResponse(&buffer, &bufferSize, ECM_SEND_CHECK_RESPONSE_CODE_OK);
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(buffer);
            break;

        case ECM_REQUEST_USR_REG:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "User registration request", ipString);
            ecmdb_connectToDb(&dbConnection, DB_USER, DB_PASS, DB_NAME);
            dbStatus = ecmdb_registerUser(dbConnection, aux->user);
            switch (dbStatus)
            {
            case ECMDB_REG_USER_SUCCESS:
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "Successful user registration", ipString);
                ecm_userRegistrationResponse(&buffer, &bufferSize, ECM_SEND_USR_REG_RESPONSE_CODE_OK);
                break;
            default:
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "User registration failed", ipString);
                ecm_userRegistrationResponse(&buffer, &bufferSize, ECM_SEND_USR_REG_RESPONSE_CODE_FAIL);
                break;
            }
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(buffer);
            break;

        case ECM_REQUEST_LOGIN:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "User login request", ipString);
            if (dbConnection == NULL)
                ecmdb_connectToDb(&dbConnection, DB_USER, DB_PASS, DB_NAME);
            dbStatus = ecmdb_loginUser(dbConnection, aux->user);
            switch (dbStatus)
            {
            case ECMDB_LOGIN_USER_SUCCESS:
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "Successful user login", ipString);
                ecm_loginResponse(&buffer, &bufferSize, ECM_SEND_LOGIN_RESPONSE_CODE_OK);
                userLogged = 1;
                break;

            default:
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "User login failed", ipString);
                ecm_loginResponse(&buffer, &bufferSize, ECM_SEND_LOGIN_RESPONSE_CODE_FAIL);
                break;
            }
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(buffer);
            break;

        case ECM_REQUEST_NEAR_EVENT:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Near event request", ipString);
            int resultFound = 0;
            if (userLogged)
            {
                dbStatus = ecmdb_getNearEvent(dbConnection, aux->user, aux->coordsArray, aux->maxRange, &array, &arraySize);
                switch (dbStatus)
                {
                case ECMDB_GET_NEAR_EVENT_SUCCESS:
                    time(&currentTime);
                    _printMessageStdOutput(&currentTime, "Near event found", ipString);
                    ecm_nearEventResponse(&buffer, &bufferSize, array, arraySize, ECM_SEND_NEAR_EVENT_RESPONSE_CODE_REPLY);
                    resultFound = 1;
                    break;

                default:
                    time(&currentTime);
                    _printMessageStdOutput(&currentTime, "Near event not found", ipString);
                    ecm_nearEventResponse(&buffer, &bufferSize, array, arraySize, ECM_SEND_NEAR_EVENT_RESPONSE_CODE_NOT_FOUND);
                    break;
                }
            }
            else
            {
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "User not logged error", ipString);
                ecm_loginResponse(&buffer, &bufferSize, ECM_SEND_LOGIN_RESPONSE_CODE_USR_NOT_LOGGED);
            }
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(aux->maxRange);
            free(aux->coordsArray);
            free(aux->coordsArrayLength);
            if (resultFound)
                free(array);
            free(buffer);
            break;

        case ECM_REQUEST_ACCELETOMETER_THRESHOLD:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Accelerometer threshold request", ipString);
            if (userLogged)
            {
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "Response to accelerometer request", ipString);
                ecm_accelerometerThresholdResponse(&buffer, &bufferSize, accelerometerThreshold);
            }
            else
            {
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "User not logged error", ipString);
                ecm_loginResponse(&buffer, &bufferSize, ECM_SEND_LOGIN_RESPONSE_CODE_USR_NOT_LOGGED);
            }
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(buffer);
            break;

        case ECM_REQUEST_SAVE_COORDS:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Event save request", ipString);
            if (userLogged)
            {
                dbStatus = ecmdb_saveCoords(dbConnection, aux->user, aux->coordsArray, aux->coordsArrayLength);
                switch (dbStatus)
                {
                case ECMDB_SAVE_COORDS_SUCCESS:
                    time(&currentTime);
                    _printMessageStdOutput(&currentTime, "Succesful event saving", ipString);
                    ecm_saveCoordinatesResponse(&buffer, &bufferSize, ECM_SEND_SAVE_COORDS_RESPONSE_CODE_OK);
                    break;

                default:
                    time(&currentTime);
                    _printMessageStdOutput(&currentTime, "Failed to save event", ipString);
                    ecm_saveCoordinatesResponse(&buffer, &bufferSize, ECM_SEND_SAVE_COORDS_RESPONSE_CODE_FAIL);
                    break;
                }
            }
            else
            {
                time(&currentTime);
                _printMessageStdOutput(&currentTime, "User not logged error", ipString);
                ecm_loginResponse(&buffer, &bufferSize, ECM_SEND_LOGIN_RESPONSE_CODE_USR_NOT_LOGGED);
            }
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(aux->coordsArray);
            free(aux->coordsArrayLength);
            free(buffer);
            break;

        case ECM_CONN_CLOSE_ANNOUNCEMENT:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Received close connection announcement", ipString);
            ecm_closeConnectionResponse(&buffer, &bufferSize);
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(buffer);
            servingClient = 0;
            break;

        case ECM_REQUEST_ECHO:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Echo request", ipString);
            ecm_echoReplyResponse(&buffer, &bufferSize);
            write(client->clientFileDescriptor, buffer, bufferSize);
            free(buffer);
            break;

        case ECM_RECEIVED_ECHO_RESPONSE:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Received echo response", ipString);
            break;

        default:
            time(&currentTime);
            _printMessageStdOutput(&currentTime, "Generic error", ipString);
            break;
        }
    }
    if (dbConnection != NULL)
        ecm_closeDbConnection(dbConnection);
    close(client->clientFileDescriptor);
    free(client);
    free(aux);
    time(&currentTime);
    _printMessageStdOutput(&currentTime, "End of service", ipString);
}

static void _printMessageStdOutput(time_t *currentTime, char *message, char *ipString)
{
    pthread_mutex_lock(&stdoutMutex);
    fprintf(stdout, "%s -> %s [%s]\n\n", ctime(currentTime), message, ipString);
    pthread_mutex_unlock(&stdoutMutex);
}