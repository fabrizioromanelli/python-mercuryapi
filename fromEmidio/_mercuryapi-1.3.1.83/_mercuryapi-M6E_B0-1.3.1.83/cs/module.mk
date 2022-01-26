# Cygwin Makefile for MercuryAPI C# and auxiliary materials
#
# make bin -- Build binaries (MercuryAPI.dll, Demo.exe, TestM5eCommands.exe)
#  * Prerequisites:
#    * .NET Framework 2.0 SDK ( http://www.microsoft.com/downloads/details.aspx?FamilyID=fe6f2099-b7b4-4f47-a244-c96d69c35dec )
#    * .NET Framework 2.0 Service Pack 1 ( http://www.microsoft.com/downloads/details.aspx?familyid=79BC3B77-E02C-4AD3-AACF-A7633F706BA5 )
#
# make doc -- Auto-generated documentation
#  * Prerequisites: (For details, see http://www.ewoodruff.us/shfbdocs/Index.aspx?topic=html/8c0c97d0-c968-4c15-9fe9-e8f3a443c50a.htm)
#    * Sandcastle, Sandcastle Styles patches, Sandcastle Help File Builder
#    * Cygwin zip (There might also be a native Windows version, but I don't know if the syntax is the same.)

#ifneq ($(MODULES_MERCURYAPI_CS_MAKE),1)
MODULES_MERCURYAPI_CS_MAKE := 1
include $(TOP_LEVEL)/make_functions.mk
#
# Definitions
#
MERCURYAPI_CS_DIR        := $(TOP_LEVEL)/modules/mercuryapi/cs
ARCH_OS_DIR       ?= $(ARCHDIR)/linux
ARCH_MODULES_DIR  ?= $(ARCH_OS_DIR)/src/modules
ARCH_CHIP_DIR     ?= $(ARCHDIR)/ARM/ixp42x
MERCURYAPI_CS_TARGETS := $(MERCURYAPI_CS_DIR)/MercuryAPI.dll $(MERCURYAPI_CS_DIR)/MercuryAPI.xml $(MERCURYAPI_CS_DIR)/MercuryAPICE.dll
MERCURYAPI_CS_TARGETS += $(MERCURYAPI_CS_DIR)/MercuryAPIHelp.zip
MERCURYAPI_CS_CLEANS  := MercuryAPI.chm MercuryAPIHelp
CLEANS                += $(MERCURYAPI_CS_TARGETS) $(MERCURYAPI_CS_CLEANS) $(MERCURYAPI_CS_DIR)/MercuryAPI.chm
NODEPTARGETS += mapi_cs_set_product_number mapi_cs_set_major_number
NODEPTARGETS += mapi_cs_set_minor_number mapi_cs_set_build_number


#.PHONY: bin clean default doc
#all: bin doc
#bin: MercuryAPI.dll MercuryAPI.xml MercuryAPICE.dll
#doc: MercuryAPI.chm MercuryAPIHelp.zip
#

# MSBuild
# Note multiple versions, each one for a different version of the .NET framework
# v3.5 is capable of building older v2.0 projects, but I'm afraid of accidentally
# introducing forward dependencies on the 3.5 framework.
MSBUILD2 ?= "/cygdrive/c/WINDOWS/Microsoft.NET/Framework/v2.0.50727/MSBuild.exe"
MSBUILD3 ?= "/cygdrive/c/WINDOWS/Microsoft.NET/Framework/v3.5/MSBuild.exe"


# Add a solution file (that lives within $(MERCURYAPI_CS_DIR))
# Usage: $(call AddSolution,name.sln,list of targets made by solution)
# Example:
# $(eval $(call AddSolution,MercuryAPI.sln,\
#  MercuryAPI.dll\
#  MercuryAPI.xml\
#  MercuryAPICE.dll\
# ))
define AddSolution
SLNS += $(MERCURYAPI_CS_DIR)/$(2)
MERCURYAPI_CS_TARGETS += $(foreach tgt,$(3),$(MERCURYAPI_CS_DIR)/$(tgt))
$(foreach tgt,$(3),$(MERCURYAPI_CS_DIR)/$(tgt)):
	$(1) /t:Build /p:Configuration=Release $(2)
SLNCLEANS += clean-$(2)
clean-$(2):
	$(1) /t:Clean /p:Configuration=Release $(2)
endef

$(eval $(call AddSolution,$(MSBUILD2),MercuryAPI.sln,\
 MercuryAPICE.dll\
 MercuryAPI.dll\
 MercuryAPI.xml\
))

$(eval $(call AddSolution,$(MSBUILD2),Samples/Codelets/Codelets.sln,\
 $(foreach codelet,\
   CommandTime\
   Filter\
   LockTag\
   Read\
   ReadAsync\
   ReadAsyncFilter\
   ReadAsyncTrack\
   SerialCommand\
   SerialTime\
   ,$(codelet).exe)\
))


# Add solution from Samples subdirectory
# Example: $(call AddSample,$(MSBUILD2),Demo-AssetTrackingTool)
define AddSample
$(call AddSolution,$(1),Samples/$(2)/$(2).sln,$(2).exe)
endef

$(eval $(call AddSample,$(MSBUILD2),MercuryApiLongTest))

$(eval $(call AddSample,$(MSBUILD3),Demo-AssetTrackingTool))
$(eval $(call AddSample,$(MSBUILD3),Demo-ReadingWithGPIO))
$(eval $(call AddSample,$(MSBUILD3),DemoProg-WindowsForm))
$(eval $(call AddSample,$(MSBUILD3),M6e-AssetTracking-Demo-Tool))
$(eval $(call AddSample,$(MSBUILD3),M6e-Read-Write-Demo-Tool))
$(eval $(call AddSample,$(MSBUILD3),ThingMagic-Reader-Firmware-Upgrade))


# Sandcastle Help File Builder
SHFB ?= "/cygdrive/c/Program Files/EWSoftware/Sandcastle Help File Builder/SandcastleBuilderConsole.exe"

$(MERCURYAPI_CS_DIR)/MercuryAPI.chm\
 $(MERCURYAPI_CS_DIR)/MercuryAPIHelp/Index.html:\
 $(MERCURYAPI_CS_DIR)/Reader.shfb\
 $(MERCURYAPI_CS_DIR)/MercuryAPI.dll\
 $(MERCURYAPI_CS_DIR)/MercuryAPI.xml
	TMP=/tmp; TEMP=/tmp; $(SHFB) $<
	mv MercuryAPIHelp/MercuryAPI.chm MercuryAPI.chm

autoparams_cs: $(wildcard $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/*.cs)
	$(AUTOPARAMS) -o $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Reader.cs --template=$(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Reader.cs $^

AUTOPARAM_CLEANS += $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Reader.cs

$(MERCURYAPI_CS_DIR)/MercuryAPIHelp.zip: $(MERCURYAPI_CS_DIR)/MercuryAPIHelp/Index.html
	cd $(MERCURYAPI_CS_DIR); zip -9r $@ MercuryAPIHelp
#
#
# Clean
#clean:
#	rm -fr MercuryAPI.chm MercuryAPIHelp.zip MercuryAPIHelp
#	$(MSBUILD) /t:Clean /p:Configuration=Release MercuryAPI.sln
#	rm -fr MercuryAPI.dll MercuryAPI.xml MercuryAPICE.dll

########################################
.PHONY: mapi_cs_synctowin mapi_cs_winmakedsp mapi_cs_syncfromwin mapi_cs_winclean 
.PHONY: mapi_cs_syncfilelist.txt

mapi_cs_winbuild: mapi_cs_synctowin mapi_cs_winmakeAPI mapi_cs_syncfromwin 

# Try to rsync only necessary files for DSP winbuild
mapi_cs_syncfilelist.txt:
	cp /dev/null $@
	# Get top-level sub-makefiles
	ls ${TM_LEVEL}/*.mk >>$@
	# Get subdirectories
	#  but omit things that are obviously not necessary: e.g., wrong version of tools
	#  factory bundle, lint (handled by winmakelint target), linux distribution
	for dirname in modules/mercuryapi; do \
	  find ${TM_LEVEL}/$${dirname} \( -name .svn \) -prune -o \( -type f -print \) \
	  |fgrep -v .svn/ |fgrep -v CVS \
	  |fgrep -v ccsCgtoolsV3_2_2 \
	  |fgrep -v factory |fgrep -v lint |fgrep -v linux |fgrep -v walmart |fgrep -v win32 \
	  |fgrep -v Release_ | fgrep -v MercuryOSLinux \
	  |fgrep -v /tmfw- |grep -v '\.tmfw$$' \
	  |grep -v '\.d$$' |grep -v '\.o$$' \
	  |grep -v '\.exe$$' |grep -v '\.m5f$$' |grep -v '\.src$$' \
	  >>$@ \
	;done
	# Fix syntax for rsync by stripping dir prefix (rsync wants relative paths)
	cat $@ |sed -e 's|^${TM_LEVEL}/||' |sort -u >$@.tmp && mv $@.tmp $@

mapi_cs_synctowin: mapi_cs_syncfilelist.txt autoparams_cs
	ssh $(WINSRV) mkdir -p $(WINDIR)/tm
	${RSYNC} --files-from=$< $(TM_LEVEL)/ $(WINSRV):$(WINDIR)/tm

mapi_cs_winmakeAPI:
	ssh $(WINSRV) 'cd $(WINDIR)/tm/modules/mercuryapi/cs; make'

mapi_cs_syncfromwin:
	$(RSYNC) '$(WINSRV):$(WINDIR)/tm/modules/mercuryapi/cs/*' $(MERCURYAPI_CS_DIR)

mapi_cs_winclean:
	$(RM) $(MERCURYAPI_CS_DIR)/MercuryAPI.dll
	$(RM) $(MERCURYAPI_CS_DIR)/MercuryAPICE.dll
	$(RM) $(MERCURYAPI_CS_DIR)/MercuryAPI.xml
	$(RM) $(MERCURYAPI_CS_DIR)/MercuryAPIHelp.zip
	$(RM) $(MERCURYAPI_CS_DIR)/MercuryAPI.chm
	$(RM) -r $(MERCURYAPI_CS_DIR)/MercuryAPIHelp
	$(RM) -r $(MERCURYAPI_CS_DIR)/Release
	$(RM) -r $(MERCURYAPI_CS_DIR)/bin
	$(RM) -r $(MERCURYAPI_CS_DIR)/obj
	$(RM) -r $(MERCURYAPI_CS_DIR)/DemoCE/bin
	$(RM) -r $(MERCURYAPI_CS_DIR)/DemoCE/obj
	$(RM) -r $(MERCURYAPI_CS_DIR)/TestM5eCommands/bin
	$(RM) -r $(MERCURYAPI_CS_DIR)/TestM5eCommands/obj
	$(RM) -r $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/bin
	$(RM) -r $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/obj
	$(RM) -r $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/bin
	$(RM) -r $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/obj
#	ssh $(WINSRV) 'cd $(WINDIR)/tm/modules/mercuryapi/cs; make clean'
	ssh $(WINSRV) 'rm -fr $(WINDIR)'

# Change the build number in the version string
# The version string must look like "1.2.3.0"
# The 0 build number will be replaced with the contents of the variable BUILDNUMBER
PRODUCTNUMBER?=0
mapi_cs_set_product_number: $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
mapi_cs_set_product_number: $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1$(PRODUCTNUMBER).\3.\4.\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly file version
	@$(RSED) 's/^(\[assembly: AssemblyFileVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1$(PRODUCTNUMBER).\3.\4.\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.ReaderCE assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1$(PRODUCTNUMBER).\3.\4.\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
 
MAJORNUMBER?=0
mapi_cs_set_major_number: $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
mapi_cs_set_major_number: $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.$(MAJORNUMBER).\4.\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly file version
	@$(RSED) 's/^(\[assembly: AssemblyFileVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.$(MAJORNUMBER).\4.\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.ReaderCE assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.$(MAJORNUMBER).\4.\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
 
MINORNUMBER?=0
mapi_cs_set_minor_number: $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
mapi_cs_set_minor_number: $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.$(MINORNUMBER).\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly file version
	@$(RSED) 's/^(\[assembly: AssemblyFileVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.$(MINORNUMBER).\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.ReaderCE assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.$(MINORNUMBER).\5\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
 
BUILDNUMBER?=0
mapi_cs_set_build_number: $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
mapi_cs_set_build_number: $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.\4.$(BUILDNUMBER)\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.Reader assembly file version
	@$(RSED) 's/^(\[assembly: AssemblyFileVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.\4.$(BUILDNUMBER)\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.Reader/Properties/AssemblyInfo.cs
# Set the cs ThingMagic.ReaderCE assembly version
	@$(RSED) 's/^(\[assembly: AssemblyVersion\(\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.\4.$(BUILDNUMBER)\6/' < $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs > $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new
	@mv $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs.new $(MERCURYAPI_CS_DIR)/ThingMagic.ReaderCE/Properties/AssemblyInfo.cs

#endif
