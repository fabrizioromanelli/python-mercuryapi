/**
 * Sample program that demonstrates different types and uses of TagFilter objects.
 * @file filter.c
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


void readAndPrintTags(TMR_Reader *rp, int timeout)
{
  TMR_Status ret;
  TMR_TagReadData trd;
  char epcString[128];

  ret = TMR_read(rp, timeout, NULL);
  if (TMR_SUCCESS != ret)
  {
    errx(1,"Error reading tags: %s\n", TMR_strerror(ret));
  }
  while (TMR_SUCCESS == TMR_hasMoreTags(rp))
  {
    ret = TMR_getNextTag(rp, &trd);
    if (TMR_SUCCESS != ret)
    {
      errx(1, "Error fetching tag: %s\n", TMR_strerror(ret));
    }
    TMR_bytesToHex(trd.tag.epc, trd.tag.epcByteCount, epcString);
    printf("%s\n", epcString);
  }
}  


int main(int argc, char *argv[])
{
  TMR_Reader r, *rp;
  TMR_Status ret;
  TMR_Region region;
  TMR_TagReadData trd;
  TMR_TagFilter filter;
  TMR_ReadPlan filteredReadPlan;
  char epcString[128];
  uint16_t newPassword[2];
  uint8_t mask[2];

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

  TMR_bytesToHex(trd.tag.epc, trd.tag.epcByteCount, epcString);
  printf("Writing 0x00000000 to kill password of tag %s\n", epcString);

  TMR_TF_init_tag(&filter, &trd.tag);
  newPassword[0] = 0;
  newPassword[1] = 0;

  ret = TMR_writeTagMemWords(rp, &filter, 0, 0, 2, newPassword);
  /* 
   * Ignore code errors here - specifically, we have no guarantee that the 
   * kill password of the tag is writable.
   */
  if (TMR_SUCCESS != ret && !TMR_ERROR_IS_CODE(ret))
  {
    errx(1, "Error writing kill password: %s\n", TMR_strerror(ret));
  }
  
  /*
   * Filter objects that apply to multiple tags are most useful in
   * narrowing the set of tags that will be read. This is
   * performed by setting a read plan that contains a filter.
   */
      
  /*
   * A TagData with a short EPC will filter for tags whose EPC
   * starts with the same sequence.
   */
  filter.type = TMR_FILTER_TYPE_TAG_DATA;
  filter.u.tagData.epcByteCount = 1;
  filter.u.tagData.epc[0] = 0x8E;
  TMR_RP_init_simple(&filteredReadPlan,
                     0, NULL, TMR_TAG_PROTOCOL_GEN2, 1000);
  TMR_RP_set_filter(&filteredReadPlan, &filter);

  ret = TMR_paramSet(rp, TMR_paramID("/reader/read/plan"), &filteredReadPlan);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting read plan: %s\n", TMR_strerror(ret));
  }

  TMR_bytesToHex(filter.u.tagData.epc, filter.u.tagData.epcByteCount,
                 epcString);
  printf("Reading tags that begin with %s\n", epcString);
  readAndPrintTags(rp, 500);

  /*
   * A filter can also be an explicit Gen2 Select operation.  For
   * example, this filter matches all Gen2 tags where bits 8-19 of
   * the TID are 0x003 (that is, tags manufactured by Alien
   * Technology).
   */
  mask[0] = 0x00;
  mask[1] = 0x03;
  TMR_TF_init_gen2_select(&filter, false, TMR_GEN2_BANK_TID, 8, 12, mask);
  /*
   * filteredReadPlan already points to filter, and
   * "/reader/read/plan" already points to filteredReadPlan.
   * However, we need to set it again in case the reader has 
   * saved internal state based on the read plan.
   */
  ret = TMR_paramSet(rp, TMR_paramID("/reader/read/plan"), &filteredReadPlan);
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting read plan: %s\n", TMR_strerror(ret));
  }
  printf("Reading tags with a TID manufacturer of 0x003\n");
  readAndPrintTags(rp, 500);

  /*
   * Filters can also be used to match tags that have already been
   * read. This form can only match on the EPC, as that's the only
   * data from the tag's memory that is contained in a TagData
   * object.
   * Note that this filter has invert=true. This filter will match
   * tags whose bits do not match the selection mask.
   * Also note the offset - the EPC code starts at bit 32 of the
   * EPC memory bank, after the StoredCRC and StoredPC.
   */
  TMR_TF_init_gen2_select(&filter, true, TMR_GEN2_BANK_TID, 32, 2, mask);

  ret = TMR_read(rp, 500, NULL);
  if (TMR_SUCCESS != ret)
  {
    errx(1,"Error reading tags: %s\n", TMR_strerror(ret));
  }
  while (TMR_SUCCESS == TMR_hasMoreTags(rp))
  {
    ret = TMR_getNextTag(rp, &trd);
    if (TMR_SUCCESS != ret)
    {
      errx(1, "Error fetching tag: %s\n", TMR_strerror(ret));
    }
    if (TMR_TF_match(&filter, &trd.tag))
    {
      TMR_bytesToHex(trd.tag.epc, trd.tag.epcByteCount, epcString);
      printf("%s\n", epcString);
    }
  }

  TMR_destroy(rp);
  return 0;
}
