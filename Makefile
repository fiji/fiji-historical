NEW_JARS=$(wildcard staged-plugins/*.jar)
JARS=$(patsubst staged-plugins/%,plugins/%,$(NEW_JARS))

all: $(JARS) run

plugins/%.jar: staged-plugins/%.jar staged-plugins/%.config
	CONFIG=$(patsubst plugins/%.jar,staged-plugins/%.config,$@) && \
	cp $$CONFIG plugins.config && \
	jar uvf $< plugins.config && \
	rm plugins.config && \
	MSG="$$(if [ -f $@ ]; then echo Updated; else echo Added; fi) $@" && \
	mv $< $@ && \
	git add $$CONFIG $@ && \
	git commit -s -m "$$MSG" $$CONFIG $@

# Java wrapper
uname_S := $(shell sh -c 'uname -s 2>/dev/null || echo not')
uname_M := $(shell sh -c 'uname -m 2>/dev/null || echo not')

LIBDL=-ldl
ifeq ($(uname_S),Linux)
ifeq ($(uname_M),x86_64)
	JAVA_HOME=java/linux-x64/jdk1.6.0_04
	JAVA_LIB_PATH=jre/lib/amd64/server/libjvm.so
else
	JAVA_HOME=java/linux/jdk1.6.0
	JAVA_LIB_PATH=jre/lib/i386/client/libjvm.so
endif
	ARCH=linux
endif
ifneq (,$(findstring MINGW,$(uname_S)))
	JAVA_HOME=java/win32/jdk1.6.0_03
	JAVA_LIB_PATH=jre/bin/client/jvm.dll
	ARCH=win32
	EXTRADEFS+= -DMINGW32
	LIBDL=
endif
ifeq ($(uname_S),Darwin)
	JAVA_HOME=java/macosx/Home
	JAVA_LIB_PATH=../Libraries/libjvm.dylib
	ARCH=macosx
	EXTRADEFS+= -DJNI_CREATEVM=\"JNI_CreateJavaVM_Impl\" -DMACOSX
	LIBMACOSX=-lpthread -framework CoreFoundation
endif
INCLUDE=$(JAVA_HOME)/include

CXXFLAGS=-g -I$(INCLUDE) -I$(INCLUDE)/$(ARCH) $(EXTRADEFS) \
	-DJAVA_HOME=\"$(JAVA_HOME)\" -DJAVA_LIB_PATH=\"$(JAVA_LIB_PATH)\"
LIBS=$(LIBDL) $(LIBMACOSX)

fiji: fiji.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)

fiji.o: fiji.cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

run: fiji
	./fiji
