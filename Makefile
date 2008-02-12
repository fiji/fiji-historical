TARGET=fiji

NEW_JARS=$(wildcard staged-plugins/*.jar)
JARS=$(patsubst staged-plugins/%,plugins/%,$(NEW_JARS))

SUBMODULE_TARGETS=ImageJA/ij.jar TrakEM2/TrakEM2_.jar VIB/VIB_.jar
SUBMODULE_TARGETS_IN_FIJI=$(shell echo "$(SUBMODULE_TARGETS)" | \
	sed -e "s|[^ ]*/ij.jar|ij.jar|" \
		-e "s|[^ ]*/\([^ /]*\.jar\)|plugins/\1|g")

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
	JDK=java/linux-amd64
	JAVA_HOME=$(JDK)/jdk1.6.0_04/jre
	JAVA_LIB_PATH=lib/amd64/server/libjvm.so
else
	JDK=java/linux
	JAVA_HOME=$(JDK)/jdk1.6.0/jre
	JAVA_LIB_PATH=lib/i386/client/libjvm.so
endif
	ARCH=linux
endif
ifneq (,$(findstring MINGW,$(uname_S)))
	JDK=java/win32
	JAVA_HOME=$(JDK)/jdk1.6.0_03/jre
	JAVA_LIB_PATH=bin/client/jvm.dll
	ARCH=win32
	EXTRADEFS+= -DMINGW32
	LIBDL=
endif
ifeq ($(uname_S),Darwin)
	JDK=java/macosx
	JAVA_HOME=$(JDK)/Home
	JAVA_LIB_PATH=../Libraries/libjvm.dylib
	ARCH=macosx
	EXTRADEFS+= -DJNI_CREATEVM=\"JNI_CreateJavaVM_Impl\" -DMACOSX
	LIBMACOSX=-lpthread -framework CoreFoundation
endif
INCLUDE=$(JAVA_HOME)/../include

CXXFLAGS=-g -I$(INCLUDE) -I$(INCLUDE)/$(ARCH) $(EXTRADEFS) \
	-DJAVA_HOME=\"$(JAVA_HOME)\" -DJAVA_LIB_PATH=\"$(JAVA_LIB_PATH)\"
LIBS=$(LIBDL) $(LIBMACOSX)

.PHONY: $(JDK)
all: $(JDK) $(SUBMODULE_TARGETS_IN_FIJI) $(JARS) $(TARGET) run

$(TARGET): $(TARGET).o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)

$(TARGET).o: $(TARGET).cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

run: $(JDK) $(TARGET)
	./$(TARGET)

# submodules

.PHONY: $(SUBMODULE_TARGETS_IN_FIJI)
$(SUBMODULE_TARGETS_IN_FIJI):
	@echo "Making $@"
	@export JAVA_HOME="$$(pwd)/$(JAVA_HOME)/.." && \
	export PATH="$$JAVA_HOME"/bin:"$$PATH" && \
	ORIGINAL_TARGET=$(shell echo " $(SUBMODULE_TARGETS) " | \
		sed "s/.* \([^ ]*$$(basename "$@")\) .*/\1/") && \
	DIR=$$(dirname $$ORIGINAL_TARGET) && \
	test ! -e $$DIR/Makefile || { \
		$(MAKE) -C $$DIR $$(basename $$ORIGINAL_TARGET) && \
		(test -f $$ORIGINAL_TARGET -a ! $$ORIGINAL_TARGET -nt $@ || \
			cp $$ORIGINAL_TARGET $@) \
	}

# JDK
$(JDK):
	@echo "Making $@"
	@test -d "$(JDK)/.git" || \
		(OBJECTSDIR="$$(pwd)/.git/objects" && \
		 cd "$(JDK)" && \
		 git init && \
		 echo "$$OBJECTSDIR" > .git/objects/info/alternates && \
		 BRANCH="refs/remotes/origin/$(JDK)" && \
		 SHA1=$$(cd ../.. && git rev-parse --verify $$BRANCH) && \
		 git update-ref "$$BRANCH" $$SHA1 && \
		 git config remote.origin.url ../.. && \
		 git config remote.origin.fetch +"$$BRANCH:$$BRANCH" && \
		 git config branch.master.remote origin && \
		 git config branch.master.merge "$$BRANCH" && \
		 git fetch)
	@echo "Updating $@"
	@(cd "$(JDK)" && git pull)

