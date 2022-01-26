/**
 * Sample program that reads tags for a fixed period of time (500ms)
 * @file SavedConfig.c
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
  
ret = TMR_SR_cmdSetProtocol(rp,TMR_TAG_PROTOCOL_GEN2);   // This to set the protocol
  if (TMR_SUCCESS != ret)
  {
    printf("Error reading tags: %s\n", TMR_strerror(ret));
    return;
  }
  ret=TMR_SR_cmdSetUserProfile(rp,0x01,0x01,0x01);  //Save all the configurations 
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting user profile option save all configuration: %s\n", TMR_strerror(ret));
  }
  else
    printf("User profile set option:save all configuration\n");

  ret=TMR_SR_cmdSetUserProfile(rp,0x02,0x01,0x01);  //Restore all saved configuration parameters 
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting user profile option restore all saved configuration params: %s\n", TMR_strerror(ret));
  }
  else
    printf("User profile set option:restore all saved configuration params\n");

  
  
  ret=TMR_SR_cmdSetUserProfile(rp,0x03,0x01,0x01);  //verify all configuration parameters 
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting user profile option verify all configuration parameters: %s\n", TMR_strerror(ret));
  }
  else
    printf("User profile set option:verify all configuration parameters\n");

  
  /* Testing 
  * TMR_SR_cmdGetUserProfile function
  */
  {
  uint8_t data[]={0x67};
  uint8_t response[10],i,j=0;
  ret=TMR_SR_cmdGetUserProfile(rp,data,1,response,&j);  
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error getting user profile option region: %s\n", TMR_strerror(ret));
  }
  else
  {
    printf(" Get user profile success option:region\n");
    for(i=0;i<j;i++)
    {
      printf(" %02x ",response[i]);
    }
    printf("\n");
  }
  }

  {
  uint8_t data[]={0x63};
  uint8_t response[10],i,j=0;
  ret=TMR_SR_cmdGetUserProfile(rp,data,1,response,&j);   
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error getting user profile option region: %s\n", TMR_strerror(ret));
  }
  else
  {
    printf(" Get user profile success option:Protocol");
    for(i=0;i<j;i++)
    {
      printf(" %02x ",response[i]);
    }
    printf("\n");
  }
  }

  {
  uint8_t data[]={0x06};
  uint8_t response[10],i,j=0;
  ret=TMR_SR_cmdGetUserProfile(rp,data,1,response,&j);  
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error getting user profile option region: %s\n", TMR_strerror(ret));
  }
  else
  {
    printf(" Get user profile success option:baudrate");
    for(i=0;i<j;i++)
    {
      printf(" %02x ",response[i]);
    }
    printf("\n");
  }
  }
  ret=TMR_SR_cmdSetUserProfile(rp,0x04,0x01,0x01);  //reset all configuration parameters 
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting user profile option reset all configuration parameters: %s\n", TMR_strerror(ret));
  }
  else
    printf("User profile set option:reset all configuration parameters\n");

  ret = TMR_SR_cmdSetProtocol(rp,TMR_TAG_PROTOCOL_GEN2);   // This to set the protocol
  if (TMR_SUCCESS != ret)
  {
    printf("Error setting protocol: %s\n", TMR_strerror(ret));
    return;
  }

  ret=TMR_SR_cmdSetUserProfile(rp,0x01,0x01,0x01);  //Save all the configurations 
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting user profile option save all configuration: %s\n", TMR_strerror(ret));
  }
  else
    printf("User profile set option:save all configuration\n");

  ret=TMR_SR_cmdSetUserProfile(rp,0x02,0x01,0x00);  //restore firmware default configuration parameters
  if (TMR_SUCCESS != ret)
  {
    errx(1, "Error setting user profile option restore firmware default configuration parameters: %s\n", TMR_strerror(ret));
  }
  else
    printf("User profile set option:restore firmware default configuration parameters\n");

  TMR_destroy(rp);
  return 0;
}
