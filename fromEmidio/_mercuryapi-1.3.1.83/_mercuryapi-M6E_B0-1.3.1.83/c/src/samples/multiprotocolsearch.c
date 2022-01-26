/**
* Sample program that perform BlockWrite
* @file multiprotocolsearch.c
*/
#include "tm_reader.h"
#include "serial_reader_imp.h"
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

const char* protocolName(TMR_TagProtocol protocol)
{
	switch (protocol)
	{
	case TMR_TAG_PROTOCOL_NONE:
		return "NONE";
	case TMR_TAG_PROTOCOL_ISO180006B:
		return "ISO180006B";
	case TMR_TAG_PROTOCOL_GEN2:
		return "GEN2";
	case TMR_TAG_PROTOCOL_ISO180006B_UCODE:
		return "ISO180006B_UCODE";
	case TMR_TAG_PROTOCOL_IPX64:
		return "IPX64";
	case TMR_TAG_PROTOCOL_IPX256:
		return "IPX256";
	default:
		return "unknown";
	}
}

int main(int argc, char *argv[])
{
  TMR_Reader r, *rp;
  TMR_Status ret;
  TMR_Region region;
  TMR_TransportListenerBlock tb;
  TMR_TagProtocol pp;
  TMR_TagProtocolList p;
  TMR_TagProtocolList *protocols;
  p.len = 1;
  p.max = 1;
  p.list = &pp;
  protocols = &p;


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

  tb.listener = serialPrinter;
  tb.cookie = stdout;
#if 0
  TMR_addTransportListener(rp, &tb);
#endif

  ret = TMR_connect(rp);
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

  {
    TMR_ReadPlan a,b,c,e,f;
    TMR_ReadPlan *d[4];
    int32_t tags ;
    TMR_ReadPlan *plan1 = &a;
    TMR_ReadPlan *plan2 = &b;
    TMR_ReadPlan *plan3 = &c;
    TMR_ReadPlan *plan4 = &e;
    TMR_ReadPlan *plan5 = &f;

    plan5->u.multi.plans = d;
    tags = 0;
    plan1->type = TMR_READ_PLAN_TYPE_SIMPLE;
    plan2->type = TMR_READ_PLAN_TYPE_SIMPLE;
    plan3->type = TMR_READ_PLAN_TYPE_SIMPLE;
    plan4->type = TMR_READ_PLAN_TYPE_SIMPLE;
    plan5->type = TMR_READ_PLAN_TYPE_MULTI;

    plan1->weight = 0;
    plan2->weight = 0;
    plan3->weight = 0;
    plan4->weight = 0;
    plan5->u.multi.totalWeight = (plan1->weight)+(plan2->weight)+(plan3->weight)+(plan4->weight);

    plan1->u.simple.protocol = TMR_TAG_PROTOCOL_GEN2;
    plan2->u.simple.protocol = TMR_TAG_PROTOCOL_ISO180006B;
    plan3->u.simple.protocol = TMR_TAG_PROTOCOL_IPX64;
    plan4->u.simple.protocol = TMR_TAG_PROTOCOL_IPX256;

    plan1->u.simple.filter = NULL; 
    plan2->u.simple.filter = NULL;
    plan3->u.simple.filter = NULL; 
    plan4->u.simple.filter = NULL;

    plan5->u.multi.planCount = 4;
    plan5->u.multi.plans[0]= plan1;
    plan5->u.multi.plans[1]= plan2;
    plan5->u.multi.plans[2]= plan3;
    plan5->u.multi.plans[3]= plan4;

    rp->readParams.readPlan = plan5;

    TMR_read(rp, 1000, &tags);

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
      printf("%s %s\n", protocolName(trd.tag.protocol), epcStr);
    }
  }

  TMR_destroy(rp);
  return 0;
}

