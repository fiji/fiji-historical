# The default target:

all: alltargets run

# Rules for building Java plugins, etc.

NEW_JARS=$(wildcard staged-plugins/*.jar)

# This maps the names of all the jars in staged-plugins/
# to the corresponding name in plugins/

JARS=$(patsubst staged-plugins/%,plugins/%,$(NEW_JARS))

# A rule for building the jars in plugins: copy-jar-if-newer.sh will
# incorporate the .config file into the ja file, and put the result
# in plugins/  (It will also commit the jar file in plugins.)

plugins/%.jar: staged-plugins/%.jar staged-plugins/%.config
	./scripts/copy-jar-if-newer.sh --delete $< $@

# The sed expressions here strip anything before ij.jar and replace
# the leading directory elements of any other jar with plugins/

SUBMODULE_TARGETS=ImageJA/ij.jar TrakEM2/TrakEM2_.jar VIB/VIB_.jar \
	mpicbg/mpicbg_.jar
SUBMODULE_TARGETS_IN_FIJI=$(shell echo "$(SUBMODULE_TARGETS)" | \
	sed -e "s|[^ ]*/ij.jar|ij.jar|" \
		-e "s|[^ ]*/\([^ /]*\.jar\)|plugins/\1|g")

.PHONY: $(SUBMODULE_TARGETS_IN_FIJI)
$(SUBMODULE_TARGETS_IN_FIJI):
	@echo "Making $@"
	@export JAVA_HOME="$$(pwd)/$(JAVA_HOME)/.." && \
	echo JAVA_HOME is $$JAVA_HOME && \
	export PATH="$$JAVA_HOME"/bin:"$$PATH" && \
	ORIGINAL_TARGET=$(shell echo " $(SUBMODULE_TARGETS) " | \
		sed "s/.* \([^ ]*$$(basename "$@")\) .*/\1/") && \
	DIR=$$(dirname $$ORIGINAL_TARGET) && \
	test ! -e $$DIR/Makefile || ( \
		$(MAKE) -C $$DIR $$(basename $$ORIGINAL_TARGET) && \
		./scripts/copy-jar-if-newer.sh $$ORIGINAL_TARGET $@ \
	)

# Recursively invoke make in src-plugins:

.PHONY: src-plugins
src-plugins:
	@export JAVA_HOME="$$(pwd)/$(JAVA_HOME)/.." && \
	export PATH="$$JAVA_HOME"/bin:"$$PATH" && \
	$(MAKE) -C $@

check: check-class-versions check-precompiled check-submodules

check-class-versions: src-plugins $(TARGET)$(EXE)
	./$(TARGET)$(EXE) --headless \
		--main-class=fiji.CheckClassVersions plugins/ jars/ misc/

PRECOMPILED=$(patsubst %,precompiled/fiji-%,$(patsubst win%,win%.exe,$(ARCHS)))
check-precompiled:
	./scripts/check-generated-content.sh fiji.cxx $(PRECOMPILED)

check-submodules:
	count=0; \
	for s in $(SUBMODULE_TARGETS_IN_FIJI); do \
		BASENAME="$$(basename "$$s")"; \
		ORIGINAL_TARGET="$$(echo " $(SUBMODULE_TARGETS) " | \
			sed "s/.* \([^ ]*\)\/$$BASENAME .*/\1/")"; \
		./scripts/check-generated-content.sh \
			$$(basename $$ORIGINAL_TARGET) $$s || \
			count=$$(($$count+1)); \
	done && \
	test $$count = 0

# MicroManager
mm:
	test -f micromanager1.1/build.sh || \
		(git submodule init micromanager1.1 && \
		 git submodule update micromanager1.1)
	export JAVA_LIB_DIR='$(JAVA_LIB_DIR)'; \
	export JAVA_HOME="$$(pwd)/$(JAVA_HOME)/.."; \
	export JAVAINC="-I$$JAVA_HOME/include -I$$JAVA_HOME/include/linux"; \
	cd micromanager1.1 && sh build.sh

# ------------------------------------------------------------------------
# Detect properties about the system we're building on (and whether
# we're cross-compiling):

uname_S := $(shell sh -c 'uname -s 2>/dev/null || echo not')
uname_M := $(shell sh -c 'uname -m 2>/dev/null || echo not')

ARCHS=linux linux-amd64 macosx win32 win64

LIBDL=-ldl
INCLUDES=-I$(JAVA_HOME)/../include -I$(JAVA_HOME)/../include/$(ARCH_INCLUDE)
JDK=java/$(ARCH)
ARCH_INCLUDE=$(ARCH)
JAVA_LIB_DIR=$(JAVA_HOME)/lib/$(CPU)

ifeq ($(ARCH),)
ifeq ($(uname_S),Linux)
ifeq ($(uname_M),x86_64)
	ARCH=linux-amd64
else
	ARCH=linux
endif
endif
ifneq (,$(findstring MINGW,$(uname_S)))
	ARCH=win32
endif
ifeq ($(uname_S),Darwin)
	ARCH=macosx
endif
endif

ifeq ($(ARCH),linux-amd64)
	CPU=amd64
	ARCH_INCLUDE=linux
	JAVA_HOME=$(JDK)/jdk1.6.0_04/jre
	JAVA_LIB_PATH=lib/amd64/server/libjvm.so
endif
ifeq ($(ARCH),linux)
	CPU=i386
	JAVA_HOME=$(JDK)/jdk1.6.0/jre
	JAVA_LIB_PATH=lib/i386/client/libjvm.so
endif
ifeq ($(ARCH),win32)
	JAVA_HOME=$(JDK)/jdk1.6.0_03/jre
	JAVA_LIB_PATH=bin/client/jvm.dll
	JAVA_LIB_DIR=bin
	EXTRADEFS+= -DMINGW32
	LIBDL=
	EXE=.exe
	STRIP_TARGET=1
endif
ifeq ($(ARCH),macosx)
	EXTRADEFS+= $(shell file -L \
	 /System/Library/Frameworks/CoreFoundation.framework/CoreFoundation \
	 | sed -n "s/^.*for architecture \\([a-z0-9_]*\\).*$$/-arch \\1/p") \
	 -mmacosx-version-min=10.4
	JDK=java/macosx-java3d
	JAVA_HOME=$(JDK)/Home
	JAVA_LIB_PATH=../Libraries/libjvm.dylib
	JAVA_LIB_DIR=
	INCLUDES= -I/System/Library/Frameworks/JavaVM.framework/Headers
	EXTRADEFS+= -DMACOSX
	LIBMACOSX=-lpthread -framework CoreFoundation -framework JavaVM

$(TARGET): Info.plist

precompiled/fiji-tiger: fiji.o
	$(CXX)  -arch ppc -arch i386 -mmacosx-version-min=10.4 -o $@ $< \
		$(LIBMACOSX)

precompiled/fiji-tiger-pita: fiji-tiger-pita.o
	$(CXX) -arch ppc -arch i386 -mmacosx-version-min=10.4 -o $@ $<

fiji-tiger-pita.o: fiji-tiger-pita.c
	$(CXX) -arch ppc -arch i386 -mmacosx-version-min=10.4 -c -o $@ $<
endif
ifeq ($(ARCH),win64)
	CXX=PATH="$$(pwd)/root-x86_64-pc-linux/bin:$$PATH" x86_64-pc-mingw32-g++
	ARCH_INCLUDE=win32
	JAVA_HOME=$(JDK)/jdk1.6.0_04/jre
	JAVA_LIB_PATH=bin/server/jvm.dll
	JAVA_LIB_DIR=bin
	EXTRADEFS+= -DMINGW32
	LIBDL=
	EXE=.exe
	STRIP_TARGET=1
endif

# ------------------------------------------------------------------------
# Rules for building the launcher:

CXXFLAGS=-g $(INCLUDES) $(EXTRADEFS) \
	-DJAVA_HOME=\"$(JAVA_HOME)\" -DJAVA_LIB_PATH=\"$(JAVA_LIB_PATH)\"
LIBS=$(LIBDL) $(LIBMACOSX)

TARGET=fiji

$(TARGET)$(EXE): fiji.o
	$(CXX) $(LDFLAGS) $(EXTRADEFS) -o $@ $< $(LIBS)
ifeq ($(STRIP_TARGET),1)
	strip $@
endif

fiji.o: fiji.cxx Makefile $(JDK)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

save-precompiled: precompiled/$(TARGET)-$(ARCH)$(EXE)

precompiled/$(TARGET)-$(ARCH)$(EXE): $(TARGET)$(EXE)
	cp $< $@

ifeq ($(FIJI_ARGS),)
ifneq ($(FIJI_RUN_PLUGIN),)
FIJI_ARGS=-eval 'run("$(FIJI_RUN_PLUGIN)");'
endif
endif

# Actually run the launcher (optionally running any plugin specified
# in the FIJI_RUN_PLUGIN environment variable):

run: $(JDK) $(TARGET)$(EXE)
ifeq ($(CROSS_COMPILE_WIN64_ON_LINUX),)
	./$(TARGET)$(EXE) $(FIJI_ARGS)
endif

dev: $(JDK) $(TARGET)$(EXE)
	AWT_TOOLKIT=MToolkit ./$(TARGET)$(EXE) \
	-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n -- \
	$(FIJI_ARGS)

# ------------------------------------------------------------------------
# JDK
.PHONY: $(JDK)
$(JDK):
	@echo "Making $@"
	@test -d "$(JDK)/.git" || \
		(OBJECTSDIR="$$(pwd -W 2> /dev/null || pwd)/.git/objects" && \
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
	@(cd "$(JDK)" && git pull origin "refs/remotes/origin/$(JDK)")

# ------------------------------------------------------------------------

portable-app: Fiji.app
	for arch in linux linux-amd64 win32; do \
		case $$arch in win32) exe=.exe;; *) exe=;; esac; \
		cp precompiled/fiji-$$arch$$exe $</; \
		jdk=$$(git ls-tree --name-only origin/java/$$arch:); \
		jre=$$jdk/jre; \
		git archive --prefix=$</java/$$arch/$$jre/ \
				origin/java/$$arch:$$jre | \
			tar xvf -; \
	done

Fiji.app: MACOS=$@/Contents/MacOS
Fiji.app: RESOURCES=$@/Contents/Resources
Fiji.app: PLIST=$@/Contents/Info.plist
Fiji.app: precompiled/fiji-macosx
Fiji.app: precompiled/fiji-tiger
Fiji.app: precompiled/fiji-tiger-pita

Fiji.app: precompiled/fiji-macosx
	test ! -d $@ || rm -rf Fiji.app
	mkdir -p $(MACOS)
	mkdir -p $(RESOURCES)
	cp Info.plist $(PLIST)
	cp precompiled/fiji-macosx $(MACOS)/
	cp precompiled/fiji-tiger $(MACOS)/
	cp precompiled/fiji-tiger-pita $(MACOS)/
	for d in java plugins macros ij.jar jars misc; do \
		test -h $(MACOS)/$$d || ln -s ../../$$d $(MACOS)/; \
	done
	ln -s Contents/Resources $@/images
	ln -s ../Resources $(MACOS)/images
	cp images/icon.png $@/images/
	git archive --prefix=$@/java/macosx-java3d/ \
		origin/java/macosx-java3d: | \
		tar xvf -
	cp ij.jar $@/
	cp -R plugins macros jars misc $@/
	cp images/Fiji.icns $(RESOURCES)

Fiji.app-%:
	ARCH=$$(echo $@ | sed "s/^Fiji.app-//"); \
	case $$ARCH in \
	$(ARCH)) \
		case $$ARCH in win*) EXE=.exe;; *) EXE=;; esac; \
		mkdir -p $@/$(JAVA_HOME) && \
		mkdir -p $@/images && \
		cp -R precompiled/fiji-$$ARCH$$EXE $@/fiji$$EXE && \
		cp -R ij.jar plugins macros jars misc $@ && \
		REL_PATH=$$(echo $(JAVA_HOME) | sed "s|java/$(ARCH)/||") && \
		git archive --prefix=java/$(ARCH)/$$REL_PATH/ \
				origin/java/$(ARCH):$$REL_PATH | \
			(cd $@ && tar xf -) && \
		cp images/icon.png $@/images/ \
	;; \
	*) \
		$(MAKE) ARCH=$$ARCH $@ \
	;; \
	esac

fiji-%.tar.bz2: Fiji.app-%
	tar cf - $< | bzip2 -9 > $@

fiji-%.zip: Fiji.app-%
	zip -9r $@ $<

fiji-%.dmg: Fiji.app
	sh scripts/mkdmg.sh $@ $<

dmg: fiji-macosx.dmg

# All targets...

alltargets: $(JDK) $(SUBMODULE_TARGETS_IN_FIJI) $(JARS) src-plugins run
	echo $^

