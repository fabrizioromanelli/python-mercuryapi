/**
* Sample program that perform BlockPermaLock
* @file BlockPermaLock.c
*/

#include <tm_reader.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include "serial_reader_imp.h"

void errx(int exitval, const char *fmt, ...)
{
  va_list ap;

  va_start(ap, fmt);
  vfprintf(stderr, fmt, ap);

  exit(exitval);
}

void serialPrinter(bool tx,uint32_t dataLen, const uint8_t data[],uint32_t timeout, void *cookie)
{
  FILE *out = cookie;
  uint32_t i;

  fprintf(out, "%s", tx ? "Sending: " : "Received:");
  for (i = 0; i < dataLen; i++)
  {
    if (i > 0 && 
      (i & 15) == 0)
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
  TMR_Tagop tagop;
  TMR_Tagop *op = &tagop;
  uint8_t data[]={0x01,0x02,0x03,0X04};
  uint32_t accessPassword = 0x00000000;
  uint16_t retData[10];
  uint16_t mask[1]={0x0001};

  if (argc < 2)
  {
    errx(1, "Please provide reader URL, such as:\n"
      "tmr:///com4\n"
      "tmr://my-reader.example.com\n");
  }

  rp = &r;
  ret = TMR_create(rp, argv[1]);
  if 
    (TMR_SUCCESS != ret)
  {
    errx(1, "Error creating reader: %s\n", TMR_strerror(ret));
  }

  {
    uint32_t rate;
    rate = 115200;
    ret = rp->paramSet(rp, TMR_PARAM_BAUDRATE, &rate);
    if (TMR_SUCCESS != ret)
    {
      errx(1, 
        "Error setting baud rate: %s\n", TMR_strerror(ret));
    }

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
  ret = TMR_paramSet(rp, TMR_PARAM_REGION_ID, &region);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting region: %s\n", TMR_strerror(ret));
  }

  ret = TMR_SR_cmdSetProtocol(rp,TMR_TAG_PROTOCOL_GEN2);   // This to set the protocol
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting protocol: %s\n", TMR_strerror(ret));
  }


  op->type = TMR_TAGOP_GEN2_BLOCKPERMALOCK;
  op->u.gen2_blockPermaLock.accessPassword=0;
  op->u.gen2_blockPermaLock.bank=TMR_GEN2_BANK_USER;
  op->u.gen2_blockPermaLock.blockPtr=0;
  op->u.gen2_blockPermaLock.blockRange=1;
  op->u.gen2_blockPermaLock.mask=mask;
  op->u.gen2_blockPermaLock.readLock=0;

  ret = TMR_SR_executeTagOp(rp,op, NULL, retData);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error permalock: %s\n", TMR_strerror(ret));
  }
  else
  {
    printf("BlockPermalock succeeded\n");
  }


  printf("PermaLock Bits:%04x\n",retData[0]);


  TMR_destroy(rp);
  return 0;
}