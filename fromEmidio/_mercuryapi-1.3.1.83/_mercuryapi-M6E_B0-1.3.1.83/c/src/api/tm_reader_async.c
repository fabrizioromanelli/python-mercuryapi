/**
 *  @file tm_reader_async.c
 *  @brief Mercury API - background reading implementation
 *  @author Nathan Williams
 *  @date 11/18/2009
 */

 /*
 * Copyright (c) 2009 ThingMagic, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
#include "tm_config.h"
#ifdef TMR_ENABLE_BACKGROUND_READS

#include <pthread.h>
#include <time.h>
#include <sys/time.h>

#include "tm_reader.h"



static void *do_background_reads(void *arg);

TMR_Status
TMR_startReading(struct TMR_Reader *reader)
{
  int ret;

  pthread_mutex_lock(&reader->backgroundLock);

  if (false == reader->backgroundSetup)
  {
    ret = pthread_create(&reader->backgroundReader, NULL,
                         do_background_reads, reader);
    if (0 != ret)
    {
      pthread_mutex_unlock(&reader->backgroundLock);
      return TMR_ERROR_NO_THREADS;
    }
    pthread_detach(reader->backgroundReader);
    reader->backgroundSetup = true;
  }
  reader->backgroundEnabled = true;
  pthread_cond_signal(&reader->backgroundCond);

  pthread_mutex_unlock(&reader->backgroundLock);
  
  return TMR_SUCCESS;
}

TMR_Status
TMR_stopReading(struct TMR_Reader *reader)
{

  pthread_mutex_lock(&reader->backgroundLock);

  if (false == reader->backgroundSetup)
  {
    pthread_mutex_unlock(&reader->backgroundLock);
    return TMR_SUCCESS;
  }

  reader->backgroundEnabled = false;
  while (true == reader->backgroundRunning)
  {
    pthread_cond_wait(&reader->backgroundCond, &reader->backgroundLock);
  }

  pthread_mutex_unlock(&reader->backgroundLock);

  return TMR_SUCCESS;
}

static void
notify_exception_listeners(TMR_Reader *reader, TMR_Status status)
{
  TMR_ReadExceptionListenerBlock *relb;

  /* 
   * If the background operation had an error, it should stop, and the
   * user should restart it when possible.
   */
  pthread_mutex_lock(&reader->backgroundLock);
  reader->backgroundEnabled = false;
  pthread_mutex_unlock(&reader->backgroundLock);

  pthread_mutex_lock(&reader->listenerLock);
  relb = reader->readExceptionListeners;
  while (relb)
  {
    relb->listener(reader, status, relb->cookie);
    relb = relb->next;
  }
  pthread_mutex_unlock(&reader->listenerLock);
}

static void *
do_background_reads(void *arg)
{
  TMR_Status ret;
  TMR_Reader *reader;
  uint32_t onTime, offTime;
  int32_t sleepTime;
  struct timeval end, now, difftime;
  struct timespec sleepspec;

  reader = arg;

  while (1)
  {

    /* Wait for reads to be enabled */
    pthread_mutex_lock(&reader->backgroundLock);
    reader->backgroundRunning = false;
    pthread_cond_broadcast(&reader->backgroundCond);
    while (false == reader->backgroundEnabled)
    {
      pthread_cond_wait(&reader->backgroundCond, &reader->backgroundLock);
    }
    reader->backgroundRunning = true;
    pthread_mutex_unlock(&reader->backgroundLock);

    /* Proceed with a round of reading and notifying */
    TMR_paramGet(reader, TMR_PARAM_READ_ASYNCONTIME, &onTime);
    TMR_paramGet(reader, TMR_PARAM_READ_ASYNCOFFTIME, &offTime);
    
    ret = TMR_read(reader, onTime, NULL);
    if (TMR_SUCCESS != ret)
    {
      notify_exception_listeners(reader, ret);
      continue;
    }

    gettimeofday(&end, NULL);

    while (TMR_SUCCESS == TMR_hasMoreTags(reader))
    {
      TMR_TagReadData trd;
      TMR_ReadListenerBlock *rlb;

      ret = TMR_getNextTag(reader, &trd);
      if (TMR_SUCCESS != ret)
      {
        notify_exception_listeners(reader, ret);
        break;
      }

      pthread_mutex_lock(&reader->listenerLock);
      rlb = reader->readListeners;
      while (rlb)
      {
        rlb->listener(reader, &trd, rlb->cookie);
        rlb = rlb->next;
      }
      pthread_mutex_unlock(&reader->listenerLock);
    }

    /* Wait for the asyncOffTime duration to pass */
    gettimeofday(&now, NULL);
    timersub(&now, &end, &difftime);

    sleepTime = offTime;
    sleepTime -= difftime.tv_usec / 1000;
    sleepTime -= difftime.tv_sec * 1000;
    if (sleepTime > 0)
    {
      sleepspec.tv_sec = sleepTime / 1000;
      sleepspec.tv_nsec = sleepTime * 1000000;
      nanosleep(&sleepspec, NULL);
    }
  }

  return NULL;
}


TMR_Status
TMR_addReadListener(TMR_Reader *reader, TMR_ReadListenerBlock *b)
{

  if (0 != pthread_mutex_trylock(&reader->listenerLock))
    return TMR_ERROR_TRYAGAIN;

  b->next = reader->readListeners;
  reader->readListeners = b;

  pthread_mutex_unlock(&reader->listenerLock);

  return TMR_SUCCESS;
}


TMR_Status
TMR_removeReadListener(TMR_Reader *reader, TMR_ReadListenerBlock *b)
{
  TMR_ReadListenerBlock *block, **prev;

  if (0 != pthread_mutex_trylock(&reader->listenerLock))
    return TMR_ERROR_TRYAGAIN;

  prev = &reader->readListeners;
  block = reader->readListeners;
  while (NULL != block)
  {
    if (block == b)
    {
      *prev = block->next;
      break;
    }
    prev = &block->next;
    block = block->next;
  }

  pthread_mutex_unlock(&reader->listenerLock);

  if (block == NULL)
  {
    return TMR_ERROR_INVALID;
  }

  return TMR_SUCCESS;
}


TMR_Status
TMR_addReadExceptionListener(TMR_Reader *reader,
                             TMR_ReadExceptionListenerBlock *b)
{

  if (0 != pthread_mutex_trylock(&reader->listenerLock))
    return TMR_ERROR_TRYAGAIN;

  b->next = reader->readExceptionListeners;
  reader->readExceptionListeners = b;

  pthread_mutex_unlock(&reader->listenerLock);

  return TMR_SUCCESS;
}


TMR_Status
TMR_removeReadExceptionListener(TMR_Reader *reader,
                                TMR_ReadExceptionListenerBlock *b)
{
  TMR_ReadExceptionListenerBlock *block, **prev;

  if (0 != pthread_mutex_trylock(&reader->listenerLock))
    return TMR_ERROR_TRYAGAIN;

  prev = &reader->readExceptionListeners;
  block = reader->readExceptionListeners;
  while (NULL != block)
  {
    if (block == b)
    {
      *prev = block->next;
      break;
    }
    prev = &block->next;
    block = block->next;
  }

  pthread_mutex_unlock(&reader->listenerLock);

  if (block == NULL)
  {
    return TMR_ERROR_INVALID;
  }

  return TMR_SUCCESS;
}


#endif /* TMR_ENABLE_BACKGROUND_READS */
