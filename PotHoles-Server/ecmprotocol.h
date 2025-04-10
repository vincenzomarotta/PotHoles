#ifndef ecmprotocol_h
#define ecmprotocol_h

#include <stdint.h>

#define ECM_LITTLE_ENDIAN 0
#define ECM_BIG_ENDIAN 1
#define ECM_PDP_ENDIAN 2

/********** GLOBAL COSTANTS **********/

#define ECM_PROTOCOL_HEADING "ECM_PROT_V1"
#define ECM_PROTOCOL_HEADING_SIZE 11

/********** RECEIVE COSTANTS **********/

#define ECM_DECODE_RECEIVED_REQUEST_NOT_VALID 100
#define ECM_DECODE_ERROR 101
#define ECM_DECODE_NULL_POINTER_ERROR 102
#define ECM_DECODE_GENERIC_ERROR 103
#define ECM_REQUEST_CHECK 200
#define ECM_REQUEST_USR_REG 201
#define ECM_REQUEST_LOGIN 202
#define ECM_REQUEST_NEAR_EVENT 203
#define ECM_REQUEST_ACCELETOMETER_THRESHOLD 204
#define ECM_REQUEST_SAVE_COORDS 205
#define ECM_REQUEST_ECHO 206
#define ECM_RECEIVED_ECHO_RESPONSE 300
#define ECM_CONN_CLOSE_ANNOUNCEMENT 400

/********** SEND COSTANTS **********/

#define ECM_SEND_CHECK_OK 500
#define ECM_SEND_CHECK_RESPONSE_CODE_OK 501
#define ECM_SEND_CHECK_RESPONSE_CODE_FAIL 502
#define ECM_SEND_CHECK_CODE_ERROR 503
#define ECM_SEND_CHECK_NULL_POINTER_ERROR 504

#define ECM_SEND_USR_REG_OK 600
#define ECM_SEND_USR_REG_RESPONSE_CODE_OK 601
#define ECM_SEND_USR_REG_RESPONSE_CODE_FAIL 602
#define ECM_SEND_USR_REG_CODE_ERROR 603
#define ECM_SEND_USR_REG_NULL_POINTER_ERROR 604

#define ECM_SEND_LOGIN_OK 700
#define ECM_SEND_LOGIN_RESPONSE_CODE_OK 701
#define ECM_SEND_LOGIN_RESPONSE_CODE_FAIL 702
#define ECM_SEND_LOGIN_RESPONSE_CODE_USR_NOT_LOGGED 703
#define ECM_SEND_LOGIN_CODE_ERROR 704
#define ECM_SEND_LOGIN_NULL_POINTER_ERROR 705

#define ECM_SEND_NEAR_EVENT_OK 800
#define ECM_SEND_NEAR_EVENT_RESPONSE_CODE_REPLY 801
#define ECM_SEND_NEAR_EVENT_RESPONSE_CODE_NOT_FOUND 802
#define ECM_SEND_NEAR_EVENT_EMPTY_ARRAY_ERROR 803
#define ECM_SEND_NEAR_EVENT_CODE_ERROR 804
#define ECM_SEND_NEAR_EVENT_NULL_POINTER_ERROR 805

#define ECM_SEND_ACCELEROMETER_THRESHOLD_OK 900
#define ECM_SEND_ACCELEROMETER_THRESHOLD_NULL_POINTER_ERROR 910

#define ECM_SEND_SAVE_COORDS_OK 1000
#define ECM_SEND_SAVE_COORDS_RESPONSE_CODE_OK 1001
#define ECM_SEND_SAVE_COORDS_RESPONSE_CODE_FAIL 1002
#define ECM_SEND_SAVE_COORDS_CODE_ERROR 1003
#define ECM_SEND_SAVE_COORDS_NULL_POINTER_ERROR 1004

#define ECM_SEND_CLOSE_CONN_OK 1100
#define ECM_SEND_CLOSE_CONN_NULL_POINTER_ERROR 1101

#define ECM_SEND_ECHO_REPLY_OK 1200
#define ECM_SEND_ECHO_REPLY_NULL_POINTER_ERROR 1201

#define ECM_SEND_ECHO_REQUEST_OK 1300
#define ECM_SEND_ECHO_REQUEST_NULL_POINTER_ERROR 1301

/********** STRUCTURES **********/

typedef struct ecm_user_t
{
  char *user;
  char *password;
  char *email;
  char *name;
  char *surname;
} ecm_user_t;

typedef struct ecm_accelerometer_coords_t
{
  double latitude;
  double longitude;
  double accelerometer;
} ecm_accelerometer_coords_t;

typedef struct ecm_auxiliary_data_t
{
  ecm_user_t *user;
  uint32_t *maxRange;
  uint32_t *accelerometerThreshold;
  uint32_t *coordsArrayLength;
  ecm_accelerometer_coords_t *coordsArray;
} ecm_auxiliary_data_t;

/********** FUNCTIONS **********/

int ecm_incomeDecoder(char *incomeBuffer, ecm_auxiliary_data_t *aux);

int ecm_checkResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode);
int ecm_loginResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode);
int ecm_nearEventResponse(char **responseBuffer, uint32_t *bufferSize, ecm_accelerometer_coords_t *array, uint32_t arraySize, int responseCode);
int ecm_accelerometerThresholdResponse(char **responseBuffer, uint32_t *bufferSize, double accelerometerThreshold);
int ecm_saveCoordinatesResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode);
int ecm_closeConnectionResponse(char **responseBuffer, uint32_t *bufferSize);
int ecm_userRegistrationResponse(char **responseBuffer, uint32_t *bufferSize, int responseCode);
int ecm_echoReplyResponse(char **responseBuffer, uint32_t *bufferSize);

int ecm_echoRequest(char **sendBuffer, uint32_t *bufferSize);

#endif