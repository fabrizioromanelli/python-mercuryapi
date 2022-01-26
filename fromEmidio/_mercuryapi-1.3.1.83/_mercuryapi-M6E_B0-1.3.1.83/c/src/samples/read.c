/**
 * Sample program that reads tags for a fixed period of time (500ms)
 * and prints the tags found.
 * @file read.c
 */

#include <tm_reader.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

void errx(int exitval, const char *fmt, ...)
{
  va_list ap;

  va_start(ap, fmt);
  vfprintf(stderr, fmt, ap);

  exit(exitval);
}

void serialPrinter(bool tx, uint32_t dataLen, const uint8_t data[],
                   uint32_t timeout, void *cookie)
{
  FILE *out = cookie;
  uint32_t i;

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

int main(int argc, char *argv[])
{
  TMR_Reader r, *rp;
  TMR_Status ret;
  TMR_Region region;
  TMR_TransportListenerBlock tb;

  if (argc < 2)
  {
    errx(1, "Please provide reader URL, such as:\n"
           "tmr:///com4\n"
           "tmr://my-reader.example.com\n");
  }
  
  rp = &r;
  ret = TMR_create(rp, argv[1]);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error creating reader: %s\n", TMR_strerror(ret));
  }

  {
    uint32_t rate;
    rate = 115200;
    ret = rp->paramSet(rp, TMR_PARAM_BAUDRATE, &rate);
  }
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting baud rate: %s\n", TMR_strerror(ret));
  }

  tb.listener = serialPrinter;
  tb.cookie = stdout;
#if 0
  TMR_addTransportListener(rp, &tb);
#endif

  ret = TMR_connect(rp);
  {
    uint32_t rate;
    rate = 9600;
    ret = rp->paramSet(rp, TMR_PARAM_BAUDRATE, &rate);
  }
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error connecting reader: %s\n", TMR_strerror(ret));
  }

  region = TMR_REGION_NA;
/*  ret = TMR_paramSet(rp, TMR_paramID("/reader/region/id"), &region); */
  ret = TMR_paramSet(rp, TMR_PARAM_REGION_ID, &region);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting region: %s\n", TMR_strerror(ret));
  }
  
  ret = TMR_read(rp, 500, NULL);
  if (TMR_SUCCESS != ret)
  {
    errx(1,"Error reading tags: %s\n", TMR_strerror(ret));
  }

  while (TMR_SUCCESS == TMR_hasMoreTags(rp))
  {
    TMR_TagReadData trd;
    char epcStr[128];

    ret = TMR_getNextTag(rp, &trd);
    if (TMR_SUCCESS != ret)
    {
      errx(1, "Error fetching tag: %s\n", TMR_strerror(ret));
    }

    TMR_bytesToHex(trd.tag.epc, trd.tag.epcByteCount, epcStr);
    printf("%s\n", epcStr);
  }

  TMR_destroy(rp);
  return 0;
}
