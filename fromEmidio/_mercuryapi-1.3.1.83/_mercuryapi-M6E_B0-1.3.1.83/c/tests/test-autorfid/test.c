#ifndef WIN32
#define TMR_ENABLE_GETOPT
#define TMR_ENABLE_READLINE
#endif

#include <ctype.h>
#include <errno.h>
#ifdef TMR_ENABLE_GETOPT
#include <getopt.h>
#endif /* TMR_ENABLE_GETOPT */ 

#ifdef WIN32
#define PRIx32 "x"
#define PRIu32 "u"
#define SCNi32 "i"
#define SCNi16 "hi"
#define SCNi8 "hhi"
#define SCNx32 "x"
#define SCNx16 "hx"
#define SCNx8 "hhx"
#else
#include <inttypes.h>
#endif

#include <time.h>

#include <stdint.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#ifdef TMR_ENABLE_READLINE
  #include <unistd.h>
  #include <readline/readline.h>
  #include <readline/history.h>
#endif /* TMR_ENABLE_READLINE */

#ifdef WIN32
#define strcasecmp _stricmp
#define strncasecmp _strnicmp
#define strtok_r strtok_s
#define strdup _strdup
#endif /* WIN32 */

#include "tm_reader.h"

#include "serial_reader_imp.h"

bool connected;

TMR_Reader r, *rp;
TMR_TransportListenerBlock tb, *listener;
TMR_Region region;
TMR_TagProtocol protocol; 

void errx(int exitval, const char *fmt, ...);
void serialPrinter(bool tx, uint32_t dataLen, const uint8_t data[],
                   uint32_t timeout, void *cookie);


int
main(int argc, char *argv[])
 {    
      TMR_Status ret;
      while(1)
      {
      rp = &r;
      ret = TMR_create(rp, argv[1]);
      if (TMR_SUCCESS != ret)
      {
        errx(1, "Error creating reader: %s\n", TMR_strerror(ret));
      }
      else 
        printf("created \n");
      {
      TMR_SR_PowerMode powerMode = TMR_SR_POWER_MODE_FULL;
      ret = rp->paramSet(rp, TMR_PARAM_POWERMODE, &powerMode);
      if (TMR_SUCCESS != ret)
      {
        errx(1, "Error setting pre-connect power mode: %s\n", TMR_strerror(ret));
      }
      }
      {
      uint32_t rate;
      rate = 921600;
      ret = rp->paramSet(rp, TMR_PARAM_BAUDRATE, &rate);
      if (TMR_SUCCESS != ret)
      {
        errx(1, "Error setting baud rate: %s\n", TMR_strerror(ret));
      }
      }
      region = TMR_REGION_NA;
      ret = TMR_paramSet(rp, TMR_PARAM_REGION_ID, &region);
      if (TMR_SUCCESS != ret)
      {
        errx(1, "Error setting region: %s\n", TMR_strerror(ret));
      }
      else
        printf("region set \n");
      
      
      tb.listener = serialPrinter;
      tb.cookie = stdout;
#if 1
      TMR_addTransportListener(rp, &tb);
#endif

   //   printlnDebug("Connecting to reader...\n");
      ret = TMR_connect(rp);
      if (TMR_SUCCESS != ret)
      {
        errx(1, "Error %x connecting reader: %s\n", ret, TMR_strerror(ret));
      }
      else
        printf("conneted \n");

      /*protocol=TMR_TAG_PROTOCOL_GEN2;
      ret=TMR_SR_cmdSetProtocol(rp,protocol);

      if (TMR_SUCCESS != ret)
      {
        errx(1, "Error setting protocol: %s\n", TMR_strerror(ret));
      }
      else
        printf("protocol set \n"); */
      //printlnDebug("Setting read power to %d...\n", app->readPower);
      /*ret = TMR_paramSet(rp, TMR_PARAM_RADIO_READPOWER, 3000);
      if (TMR_SUCCESS != ret)
      {
        errx(1,"Error setting read power: %s\n", TMR_strerror(ret));
      }
      else
        printf(" readpower set \n");
*/
      {
        TMR_GEN2_Session value;
        value = TMR_GEN2_SESSION_S1;
       // printlnDebug("Setting Gen2 session to %d...", value);
        ret = TMR_paramSet(rp, TMR_PARAM_GEN2_SESSION, &value);
        if (TMR_SUCCESS != ret)
        {
          errx(1, "Error setting Gen2 session: %s", TMR_strerror(ret));
        }
        else
        {
          printf("session set \n");
        }
      }

      ret = TMR_read(rp, 5*1000, NULL);
      if (TMR_SUCCESS != ret)
      {
        errx(1,"Error %x reading tags: %s\n", ret, TMR_strerror(ret));
      }
      else
        printf("reading \n");
      while (TMR_SUCCESS == TMR_hasMoreTags(rp))
      {
        int i;
        TMR_TagReadData trdInstance;
        TMR_TagReadData* trd = &trdInstance;
        ret = TMR_getNextTag(rp, trd);
        printf("in while \n");
        if (TMR_SUCCESS != ret)
        {
          errx(1, "Error fetching tag: %s\n", TMR_strerror(ret));
        }
        printf("Tag Data: ");
        for(i=0;i<trd->tag.epcByteCount;i++)
        {
          printf("%02"SCNx8,trd->tag.epc[i]);
        }
        printf("\n");
      }
      TMR_destroy(rp);
      }
 }
void
errx(int exitval, const char *fmt, ...)
{
  va_list ap;

  va_start(ap, fmt);
  vfprintf(stderr, fmt, ap);

  if (connected)
  {
    TMR_destroy(rp);
  }
  exit(exitval);
}

void
serialPrinter(bool tx, uint32_t dataLen, const uint8_t data[],
              uint32_t timeout, void *cookie)
{
  FILE *out;
  uint32_t i;

  out = cookie;

  fprintf(out, "%s", tx ? "Sending: " : "Received:");
  for (i = 0; i < dataLen; i++)
  {
    if (i > 0 && (i & 15) == 0)
    {
      fprintf(out, "\n         ");
    }
    fprintf(out, " %02x", data[i]);
  }
  fprintf(out, "\n");
}

