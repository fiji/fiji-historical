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

SUBMODULE_TARGETS=ImageJA/ij.jar TrakEM2/TrakEM2_.jar VIB/VIB_.jar
SUBMODULE_TARGETS_IN_FIJI=$(shell echo "$(SUBMODULE_TARGETS)" | \
	sed -e "s|[^ ]*/ij.jar|ij.jar|" \
		-e "s|[^ ]*/\([^ /]*\.jar\)|plugins/\1|g")

.PHONY: $(SUBMODULE_TARGETS_IN_FIJI)
$(SUBMODULE_TARGETS_IN_FIJI):
	@echo "Making $@"
	@export JAVA_HOME="$$(pwd)/$(JAVA_HOME)/.." && \
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

check: src-plugins $(TARGET)$(EXE)
	./$(TARGET)$(EXE) -eval 'run("Get Class Versions"); run("Quit");' | \
		sort

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

LIBDL=-ldl
INCLUDES=-I$(JAVA_HOME)/../include -I$(JAVA_HOME)/../include/$(ARCH_INCLUDE)
JDK=java/$(ARCH)
ARCH_INCLUDE=$(ARCH)
JAVA_LIB_DIR=$(JAVA_HOME)/lib/$(CPU)
ifeq ($(uname_S),Linux)
ifeq ($(uname_M),x86_64)
	CPU=amd64
	ARCH=linux-amd64
	ARCH_INCLUDE=linux
	JAVA_HOME=$(JDK)/jdk1.6.0_04/jre
	JAVA_LIB_PATH=lib/amd64/server/libjvm.so
else
	CPU=i386
	ARCH=linux
	JAVA_HOME=$(JDK)/jdk1.6.0/jre
	JAVA_LIB_PATH=lib/i386/client/libjvm.so
endif
endif
ifneq (,$(findstring MINGW,$(uname_S)))
	ARCH=win32
	JAVA_HOME=$(JDK)/jdk1.6.0_03/jre
	JAVA_LIB_PATH=bin/client/jvm.dll
	JAVA_LIB_DIR=bin
	EXTRADEFS+= -DMINGW32
	LIBDL=
	EXE=.exe
	STRIP_TARGET=1
endif
ifeq ($(uname_S),Darwin)
ifeq ($(uname_M),Power Macintosh)
	ARCH=macosx
else
	ARCH=macosx-intel
	EXTRADEFS+= -arch i386 -arch ppc
endif

	JAVA_HOME=$(JDK)/Home
	JAVA_LIB_PATH=../Libraries/libjvm.dylib
	JAVA_LIB_DIR=../Libraries
	INCLUDES=-I$(JDK)/Headers
	EXTRADEFS+= -DJNI_CREATEVM=\"JNI_CreateJavaVM_Impl\" -DMACOSX
	LIBMACOSX=-lpthread -framework CoreFoundation
endif
ifneq ($(CROSS_COMPILE_WIN64_ON_LINUX),)
	CXX=PATH="$$(pwd)/root-x86_64-pc-linux/bin:$$PATH" x86_64-pc-mingw32-g++
	ARCH=win64
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

TARGET=fiji-$(ARCH)

$(TARGET)$(EXE): fiji.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)
ifeq ($(STRIP_TARGET),1)
	strip $@
endif

fiji.o: fiji.cxx Makefile $(JDK)
	$(CXX) $(CXXFLAGS) -c -o $@ $<

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
		cp fiji-$$arch$$exe $</; \
		jdk=$$(git ls-tree --name-only origin/java/$$arch:); \
		jre=$$jdk/jre; \
		git archive --prefix=$</java/$$arch/$$jre/ \
				origin/java/$$arch:$$jre | \
			tar xvf -; \
	done

Fiji.app: MACOS=$@/Contents/MacOS
Fiji.app: RESOURCES=$@/Contents/Resources
Fiji.app: PLIST=$@/Contents/Info.plist

# TODO: Tried to make it work for powerpc AND mac-intel
Fiji.app: fiji-macosx-intel
	mkdir -p $(MACOS)
	mkdir -p $(RESOURCES)
	echo '<?xml version="1.0" encoding="UTF-8"?>' > $(PLIST)
	echo '<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">' >> $(PLIST)
	echo '<plist version="1.0">' >> $(PLIST)
	echo '<dict>' >> $(PLIST)
	echo '	<key>CFBundleExecutable</key>' >> $(PLIST)
	echo '		<string>fiji-macosx-intel</string>' >> $(PLIST)
	echo '	<key>CFBundleGetInfoString</key>' >> $(PLIST)
	echo '		<string>Fiji for Mac OS X</string>' >> $(PLIST)
	echo '	<key>CFBundleIconFile</key>' >> $(PLIST)
	echo '		<string>Fiji.icns</string>' >> $(PLIST)
	echo '	<key>CFBundleIdentifier</key>' >> $(PLIST)
	echo '		<string>org.fiji</string>' >> $(PLIST)
	echo '	<key>CFBundleInfoDictionaryVersion</key>' >> $(PLIST)
	echo '		<string>6.0</string>' >> $(PLIST)
	echo '	<key>CFBundleName</key>' >> $(PLIST)
	echo '		<string>Fiji</string>' >> $(PLIST)
	echo '	<key>CFBundlePackageType</key>' >> $(PLIST)
	echo '		<string>APPL</string>' >> $(PLIST)
	echo '	<key>CFBundleVersion</key>' >> $(PLIST)
	echo '		<string>1.0</string>' >> $(PLIST)
	echo '	<key>NSPrincipalClass</key>' >> $(PLIST)
	echo '		<string>NSApplication</string>' >> $(PLIST)
	echo '</dict>' >> $(PLIST)
	echo '</plist>"' >> $(PLIST)
	cp fiji-macosx-intel $(MACOS)/
	for d in java plugins macros ij.jar jars; do \
		test -h $(MACOS)/$$d || ln -s ../../$$d $(MACOS)/; \
	done
	git archive --prefix=$@/java/macosx-intel/ origin/java/macosx-intel: | \
		tar xvf -
	cp ij.jar $@/
	cp -R plugins $@/
	cp -R macros $@/
	cp -R jars $@/
	cp images/Fiji.icns $(RESOURCES)

# All targets...

alltargets: $(JDK) $(SUBMODULE_TARGETS_IN_FIJI) $(JARS) src-plugins run
	echo $^

