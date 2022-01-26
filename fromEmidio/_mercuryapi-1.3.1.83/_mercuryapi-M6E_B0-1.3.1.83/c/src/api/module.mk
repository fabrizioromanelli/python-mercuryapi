#
#  Makefile to build the C implementation of the Mercury API
#
#ifneq ($(MODULES_MERCURYAPI_C_MAKE),1)
MODULES_MERCURYAPI_C_MAKE := 1
include $(TOP_LEVEL)/make_functions.mk

#
# Definitions
#
MERCURYAPI_C_TARGET_LIB := $(BUILD)/libmercuryapi$(LIB_SUFFIX)
MERCURYAPI_C_DIR         = $(TOP_LEVEL)/modules/mercuryapi/c
MERCURYAPI_C_SRCDIR      = $(MERCURYAPI_C_DIR)/src/api

MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/serial_transport_posix.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/serial_transport_llrp.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/tmr_strerror.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/tmr_param.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/hex_bytes.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/tm_reader.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/tm_reader_async.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/serial_reader.c
MERCURYAPI_C_SRCS += $(MERCURYAPI_C_SRCDIR)/serial_reader_l3.c

MERCURYAPI_C_OBJS := $(MERCURYAPI_C_SRCS:.c=$(OBJ_SUFFIX))
MERCURYAPI_C_OBJS := $(notdir $(MERCURYAPI_C_OBJS))
MERCURYAPI_C_OBJS := $(MERCURYAPI_C_OBJS:%=${BUILD}/%)

ALL_OBJS   += $(MERCURYAPI_C_OBJS)
ALL_C_SRCS += $(MERCURYAPI_C_SRCS)

TARGETS    += $(MERCURYAPI_C_TARGET_LIB)
TARGETS    += $(BUILD)/cdoc/index.html
CINCS      += -I$(MERCURYAPI_C_SRCDIR)

$(MERCURYAPI_C_TARGET_LIB): $(MERCURYAPI_C_OBJS)
	$(AR) $(ARFLAGS) $@ $^

$(eval $(call compile-rules,$(MERCURYAPI_C_SRCS)))

.INTERMEDIATE: $(MERCURYAPI_C_OBJS)

$(BUILD)/cdoc/index.html: $(MERCURYAPI_C_SRCS) autoparams_c
	cd $(MERCURYAPI_C_SRCDIR); $(DOXYGEN) $(MERCURYAPI_C_SRCDIR)/Doxyfile
	mkdir -p `dirname $@`
	cp -p $(MERCURYAPI_C_SRCDIR)/cdoc/html/* `dirname $@`

autoparams_c: $(wildcard $(MERCURYAPI_C_SRCDIR)/*.[ch])
	$(AUTOPARAMS) -o $(MERCURYAPI_C_SRCDIR)/tm_reader.h --template=$(MERCURYAPI_C_SRCDIR)/tm_reader.h $^

AUTOPARAM_CLEANS += $(MERCURYAPI_C_SRCDIR)/tm_reader.h


# Change the build number in the version string
# The version string must look like "1.2.3.0"
# The 0 build number will be replaced with the contents of the variable BUILDNUMBER
PRODUCTNUMBER?=0
mapi_c_set_product_number: $(MERCURYAPI_C_SRCDIR)/tm_config.h
# Set the C API version
	$(RSED) 's/(TMR_VERSION\ *\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1$(PRODUCTNUMBER).\3.\4.\5\6/' < $(MERCURYAPI_C_SRCDIR)/tm_config.h > $(MERCURYAPI_C_SRCDIR)/tm_config.h.new
	mv $(MERCURYAPI_C_SRCDIR)/tm_config.h.new $(MERCURYAPI_C_SRCDIR)/tm_config.h

MAJORNUMBER?=0
mapi_c_set_major_number: $(MERCURYAPI_C_SRCDIR)/tm_config.h
# Set the C API version
	@$(RSED) 's/(TMR_VERSION\ *\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.$(MAJORNUMBER).\4.\5\6/' < $(MERCURYAPI_C_SRCDIR)/tm_config.h > $(MERCURYAPI_C_SRCDIR)/tm_config.h.new
	@mv $(MERCURYAPI_C_SRCDIR)/tm_config.h.new $(MERCURYAPI_C_SRCDIR)/tm_config.h

MINORNUMBER?=0
mapi_c_set_minor_number: $(MERCURYAPI_C_SRCDIR)/tm_config.h
# Set the C API version
	@$(RSED) 's/(TMR_VERSION\ *\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.$(MINORNUMBER).\5\6/' < $(MERCURYAPI_C_SRCDIR)/tm_config.h > $(MERCURYAPI_C_SRCDIR)/tm_config.h.new
	@mv $(MERCURYAPI_C_SRCDIR)/tm_config.h.new $(MERCURYAPI_C_SRCDIR)/tm_config.h

BUILDNUMBER?=0
mapi_c_set_build_number: $(MERCURYAPI_C_SRCDIR)/tm_config.h
# Set the C API version
	@$(RSED) 's/(TMR_VERSION\ *\")([0-9]+).([0-9]+).([0-9]+).([0-9]+)(\")/\1\2.\3.\4.$(BUILDNUMBER)\6/' < $(MERCURYAPI_C_SRCDIR)/tm_config.h > $(MERCURYAPI_C_SRCDIR)/tm_config.h.new
	@mv $(MERCURYAPI_C_SRCDIR)/tm_config.h.new $(MERCURYAPI_C_SRCDIR)/tm_config.h

#endif
