#ifndef ecmdb_h
#define ecmdb_h

#include <postgresql/libpq-fe.h>
#include "ecmprotocol.h"

#define ECMDB_CONNECTION_DB_SUCCESSFUL 100
#define ECMDB_CONNECTION_DB_FAIL 110
#define ECMDB_CONNECTION_DB_NULL_POINTER_ERROR 120
#define ECMDB_CONNECTION_DB_USER_LEN_ERROR 130
#define ECMDB_CONNECTION_DB_PASSWORD_LEN_ERROR 130
#define ECMDB_CONNECTION_DB_NAME_LEN_ERROR 130

#define ECMDB_REG_USER_SUCCESS 200
#define ECMDB_REG_USER_FAIL 210
#define ECMDB_REG_CONNECTION_NULL_POINTER_ERRROR 220
#define ECMDB_REG_USER_NULL_POINTER_ERROR 230
#define ECMDB_REG_USER_GENERIC_ERROR 240

#define ECMDB_LOGIN_USER_SUCCESS 300
#define ECMDB_LOGIN_USER_FAIL 310
#define ECMDB_LOGIN_CONNECTION_NULL_POINTER_ERRROR 320
#define ECMDB_LOGIN_USER_NULL_POINTER_ERROR 330
#define ECMDB_LOGIN_USER_GENERIC_ERROR 340

#define ECMDB_GET_NEAR_EVENT_SUCCESS 400
#define ECMDB_GET_NEAR_EVENT_FAIL 410
#define ECMDB_GET_NEAR_EVENT_CONNECTION_NULL_POINTER_ERRROR 420
#define ECMDB_GET_NEAR_EVENT_USER_NULL_POINTER_ERROR 430
#define ECMDB_GET_NEAR_EVENT_INPUT_COORDS_NULL_POINTER_ERROR 440
#define ECMDB_GET_NEAR_EVENT_OUTPUT_ARRAY_NULL_POINTER_ERROR 450
#define ECMDB_GET_NEAR_EVENT_ARRAY_SIZE_NULL_POINTER_ERROR 460
#define ECMDB_GET_NEAR_EVENT_GENERIC_ERROR 470

#define ECMDB_SAVE_COORDS_SUCCESS 500
#define ECMDB_SAVE_COORDS_FAIL 510
#define ECMDB_SAVE_COORDS_CONNECTION_NULL_POINTER_ERRROR 520
#define ECMDB_SAVE_COORDS_USER_NULL_POINTER_ERROR 530
#define ECMDB_SAVE_COORDS_ARRAY_NULL_POINTER_ERROR 540
#define ECMDB_SAVE_COORDS_ARRAY_SIZE_NULL_POINTER_ERROR 550

int ecmdb_connectToDb(PGconn **connection, char *dbUser, char *dbPassword, char *dbName);
void ecm_closeDbConnection(PGconn *connection);
int ecmdb_registerUser(PGconn *connection, ecm_user_t *user);
int ecmdb_loginUser(PGconn *connection, ecm_user_t *user);
int ecmdb_getNearEvent(PGconn *connection, ecm_user_t *user, ecm_accelerometer_coords_t *input, uint32_t *maxRange, ecm_accelerometer_coords_t **outputArray, uint32_t *arraySize);
int ecmdb_saveCoords(PGconn *connection, ecm_user_t *user, ecm_accelerometer_coords_t *array, uint32_t *arraySize);

#endif