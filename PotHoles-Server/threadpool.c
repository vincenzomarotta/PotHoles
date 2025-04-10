#include "threadpool.h"

#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

/*
Aggiunge il nuovo lavoro alla coda del thread pool.
Restituisce TRUE in caso di successo.
Restituisce FALSE nel caso la coda sia piena.
*/
static bool _threadpool_addWorkToQueue(threadpool_t *threadPool, threadpool_work_t *workToAdd) {
    pthread_mutex_lock(&(threadPool->workMutex));
    if (threadPool->firstWork == NULL) {
        threadPool->firstWork = threadPool->lastWork = workToAdd;
        threadPool->currentNumberOfWork++;
    } else if (threadPool->currentNumberOfWork < threadPool->maximumNumberOfWork) {
        threadPool->lastWork->next = workToAdd;
        threadPool->lastWork = workToAdd;
        threadPool->currentNumberOfWork++;
    } else {
        pthread_mutex_unlock(&(threadPool->workMutex));
        printf("Error: maximum number of jobs reached.\n");
        return false;
    }
    pthread_mutex_unlock(&(threadPool->workMutex));
    pthread_cond_broadcast(&(threadPool->threadCondition));
    return true;
}

/*
Recupera un lavoro da eseguire e aggiorna la coda e il contatore.
*/
static threadpool_work_t *_threadpool_getWork(threadpool_t *threadpool) {
    threadpool_work_t *temp = threadpool->firstWork;

    if (threadpool->firstWork == threadpool->lastWork)
        threadpool->firstWork = threadpool->lastWork = NULL;
    else
        threadpool->firstWork = threadpool->firstWork->next;
    threadpool->currentNumberOfWork--;

    return temp;
}

/*
Dealloca le memoria di una struttura threadpool_work_t.
*/
static void _threadpool_destoryWork(threadpool_work_t *workToDestoy) {
    free(workToDestoy);
}

/*
Codice per i thread che eseguono le funzioni.
*/
static void *threadpool_threadWorkFuncion(void *args) {
    threadpool_t *myThreadPool = (threadpool_t *)args;
    threadpool_work_t *workToDo;

    while (myThreadPool->killAll == false) {
        pthread_mutex_lock(&(myThreadPool->threadMutex));
        while ((myThreadPool->firstWork == NULL) || (myThreadPool->run == THREADPOOL_STOP))
            pthread_cond_wait(&(myThreadPool->threadCondition), &(myThreadPool->threadMutex));

        pthread_mutex_lock(&(myThreadPool->workMutex));
        workToDo = _threadpool_getWork(myThreadPool);
        pthread_mutex_unlock(&(myThreadPool->workMutex));

        pthread_mutex_unlock(&(myThreadPool->threadMutex));
        pthread_cond_broadcast(&(myThreadPool->threadCondition));

        workToDo->threadFunction(workToDo->argument);  // Esegue la funzione passando gli argomenti.

        _threadpool_destoryWork(workToDo);
    }

    // printf("Sono %lu e sono killato.\n", pthread_self());

    return NULL;
}

/*
Crea un nuovo thread pool.
Accetta come parametri il massimo numero di thread paralleli, la massima dimensione della coda di lavori
e lo stato di run iniziale.
Lo stato di run iniziale può assumere i seguenti valori:
    - THREADPOOL_STOP
    - THREADPOOL_RUN
Restituisce il puntatore al thread pool in caso di successo.
Restituisce NULL nel caso di errore.
*/
threadpool_t *threadpool_create(size_t numberOfThreads, size_t maximumNumberOfWork, int startState) {
    threadpool_t *newPool = (threadpool_t *)calloc(1, sizeof(threadpool_t));
    if (newPool == NULL) {
        perror("Memory allocation error");
        return NULL;
    }

    newPool->maxNumberOfParallelThreads = numberOfThreads;
    newPool->maximumNumberOfWork = maximumNumberOfWork;
    newPool->killAll = false;

    pthread_mutex_init(&(newPool->workMutex), NULL);
    pthread_mutex_init(&(newPool->threadMutex), NULL);
    pthread_cond_init(&(newPool->threadCondition), NULL);

    if (startState == THREADPOOL_STOP)
        newPool->run = false;
    if (startState == THREADPOOL_RUN)
        newPool->run = true;

    for (size_t i = 0; i < numberOfThreads; i++) {
        pthread_t thread;
        pthread_create(&thread, NULL, threadpool_threadWorkFuncion, (void *)newPool);
        pthread_detach(thread);
    }

    return newPool;
}

/*
Aggiunge un nuovo lavoro alla coda del thread pool.
Prende in ingresso il puntatore al thread pool, la funzione da far eseguire al thread, può accettare
argomenti per il thread.
Il parametro threadPool e threadFunction non possono essere NULL.
Restituisce TRUE in caso di successo.
Restituisce FALSE in caso di errore o coda piena.
*/
bool threadpool_addWork(threadpool_t *threadPool, thread_func_t threadFunction, void *args) {
    threadpool_work_t *newWork;

    if (threadPool == NULL) {
        printf("Error: thread pool can't be NULL.\n");
        return false;
    }
    if (threadFunction == NULL) {
        printf("Error: thread function can't be NULL.\n");
        return false;
    }

    newWork = (threadpool_work_t *)calloc(1, sizeof(threadpool_work_t));
    if (newWork == NULL) {
        perror("Memory allocation error");
        exit(EXIT_FAILURE);
    }
    newWork->threadFunction = threadFunction;
    newWork->argument = args;

    return _threadpool_addWorkToQueue(threadPool, newWork);
}

/*
Setta lo stato di run dei thread nell'eseguire lavori.
Quando viene femato il lavoro, i thread prima di fermarsi compleano il lavoro corrente.
*/
bool threadpool_setRunState(threadpool_t *threadPool, int runState) {
    if (threadPool == NULL) {
        printf("Error: thread pool can't be NULL.\n");
        return false;
    } else {
        if (runState == THREADPOOL_STOP)
            threadPool->run = THREADPOOL_STOP;
        if (runState == THREADPOOL_RUN) {
            threadPool->run = THREADPOOL_RUN;
            pthread_cond_broadcast(&(threadPool->threadCondition));
        } else
            return false;
        return true;
    }
}

/*
Comunica ai thread di terminarsi e distugge il thread pool.
Attenzione: nel caso un thread non terminasse, esso potrebbe rimanere in esecuzione (thread orfano).
*/
void threadpool_killAll(threadpool_t **threadPool) {
    threadpool_work_t *temp = NULL;

    (*threadPool)->killAll = true;
    pthread_cond_broadcast(&(*threadPool)->threadCondition);
    if ((*threadPool)->firstWork != NULL) {
        while ((*threadPool)->firstWork != NULL) {
            temp = (*threadPool)->firstWork;
            (*threadPool)->firstWork = (*threadPool)->firstWork->next;
            free(temp);
        }
    }
    free(*threadPool);
    *threadPool = NULL;
}