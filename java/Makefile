uname_S := $(shell sh -c 'uname -s 2>/dev/null || echo not')

LIBDL=-ldl
ifeq ($(uname_S),Linux)
	JAVA_HOME=linux/jdk1.6.0
	JAVA_LIB_PATH=jre/lib/i386/client/libjvm.so
	ARCH=linux
endif
ifneq (,$(findstring MINGW,$(uname_S)))
	JAVA_HOME=win32/jdk1.6.0_03
	JAVA_LIB_PATH=jre/bin/client/jvm.dll
	ARCH=win32
	EXTRADEFS+=-DMINGW32
	LIBDL=
endif
INCLUDE=$(JAVA_HOME)/include

CXXFLAGS=-g -I$(INCLUDE) -I$(INCLUDE)/$(ARCH) $(EXTRADEFS) \
	-DJAVA_HOME=\"$(JAVA_HOME)\" -DJAVA_LIB_PATH=\"$(JAVA_LIB_PATH)\"
LIBS=$(LIBDL)

launcher: launcher.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)

launcher.o: launcher.cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

run:
	./launcher
