#ifndef threadpool_h
#define threadpool_h

#include <pthread.h>
#include <stdbool.h>

#define THREADPOOL_STOP 0
#define THREADPOOL_RUN 1

typedef void (*thread_func_t)(void *args); // tipo per il passaggio della funzione per i lavori.

/* Struttura di un lavoro. */
typedef struct threadpool_work {
    thread_func_t threadFunction;       // Funzione che deve essere eseguita.
    void *argument;                     // Argomenti della funzione che deve essere eseguita.
    struct threadpool_work *next;       // Puntatore al prossimo elemento della coda di lavori.
} threadpool_work_t;

/* Struttura di un thread pool */
typedef struct threadpool {
    threadpool_work_t *firstWork;       // Puntatore al primo elemento della coda dei lavori.
    threadpool_work_t *lastWork;        // Puntatore all'ultimo elemento della coda dei lavori.
    size_t maxNumberOfParallelThreads;  // Numero massimo dei thread eseguiti in parallelo.
    size_t maximumNumberOfWork;         // Dimensione massima della coda dei lavori.
    size_t currentNumberOfWork;         // Dimensione corrente della coda dei lavori.
    pthread_mutex_t workMutex;          // Mutex per la modifica della coda dei lavori.
    pthread_cond_t workCondition;       // Cond per i thread. Serve per fermare i thread se non hanno lavoro o se il flag di sto
    volatile bool run;                  // Booleano per impostare lo stato di elaborazione dei thread.
    volatile bool killAll;              // Booleano per impostare lo stato di terminazione dei thread.
    pthread_mutex_t endMutex;           // Mutex per incrementare il contatore di thread terminati.
    volatile size_t numEndedThread;     // Numero dei thread terminati.
    pthread_cond_t endCondition;        // Cond per fermare il thread principale in modo da attendere la terminazione dei thread del pool.
} threadpool_t;

threadpool_t *threadpool_create(size_t numberOfThreads, size_t maximumNumberOfWork, int startState);
bool threadpool_addWork(threadpool_t *threadPool, thread_func_t threadFunction, void *args);
bool threadpool_setRunState(threadpool_t *threadPool, int runState);
void threadpool_killAll(threadpool_t **threadpool);

#endif