/**
 * Sample program that sets an access password on a tag and locks its EPC.
 * @file locktag.c
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

int main(int argc, char *argv[])
{
  TMR_Reader r, *rp;
  TMR_Status ret;
  TMR_Region region;
  TMR_TagReadData trd;
  TMR_TagFilter filter;
  TMR_TagAuthentication auth;
  TMR_Tagop tagop;
  TMR_Tagop *op = &tagop;
  char epcString[128];
  uint16_t newPassword[2];

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
  ret = TMR_paramSet(rp, TMR_paramID("/reader/region/id"), &region);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting region: %s\n", TMR_strerror(ret));
  }
  
  ret = TMR_read(rp, 500, NULL);
  if (TMR_SUCCESS != ret)
  {
    errx(1,"Error reading tags: %s\n", TMR_strerror(ret));
  }

  if (TMR_ERROR_NO_TAGS == TMR_hasMoreTags(rp))
  {
    errx(1, "No tags found for test\n");
  }

  ret = TMR_getNextTag(rp, &trd);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error reading tags: %s\n", TMR_strerror(ret));
  }

  /* Set the access password of the tag */
  TMR_TF_init_tag(&filter, &trd.tag);
  filter.u.tagData = trd.tag;
  newPassword[0] = 0x8888;
  newPassword[1] = 0x7777;
  ret = TMR_writeTagMemWords(rp, &filter, 0, 0, 2, newPassword);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error writing access password: %s\n", TMR_strerror(ret));
  }

  TMR_bytesToHex(trd.tag.epc, trd.tag.epcByteCount, epcString);
  printf("Set access password of %s to 0x88887777\n", epcString);

  TMR_TA_init_gen2(&auth, 0x88887777);
  ret = TMR_paramSet(rp, TMR_paramID("/reader/gen2/accessPassword"), &auth);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting access password parameter: %s\n", TMR_strerror(ret));
  }


  op->type=TMR_TAGOP_GEN2_LOCK;
  op->u.gen2_lock.action = TMR_GEN2_LOCK_BITS_EPC;
  op->u.gen2_lock.mask = TMR_GEN2_LOCK_BITS_EPC;

  ret= TMR_SR_executeTagOp(rp,op, &filter, NULL);

  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error locking tag: %s\n", TMR_strerror(ret));
  }
  printf("Locked EPC of tag %s\n", epcString);

  TMR_destroy(rp);
  return 0;
}
