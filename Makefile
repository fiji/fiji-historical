TARGET=fiji-$(ARCH)

NEW_JARS=$(wildcard staged-plugins/*.jar)

# This maps the names of all the jars in staged-plugins/
# to the corresponding name in plugins/

JARS=$(patsubst staged-plugins/%,plugins/%,$(NEW_JARS))

# The sed expressions here strip anything before ij.jar and replace
# the leading directory elements of any other jar with plugins/

SUBMODULE_TARGETS=ImageJA/ij.jar TrakEM2/TrakEM2_.jar VIB/VIB_.jar
SUBMODULE_TARGETS_IN_FIJI=$(shell echo "$(SUBMODULE_TARGETS)" | \
	sed -e "s|[^ ]*/ij.jar|ij.jar|" \
		-e "s|[^ ]*/\([^ /]*\.jar\)|plugins/\1|g")

# A rule for building the jars in plugins: copy-jar-if-newer.sh will
# incorporate the .config file into the ja file, and put the result
# in plugins/  (It will also commit the jar file in plugins.)

plugins/%.jar: staged-plugins/%.jar staged-plugins/%.config
	./scripts/copy-jar-if-newer.sh --delete --commit $< $@

# Java wrapper
uname_S := $(shell sh -c 'uname -s 2>/dev/null || echo not')
uname_M := $(shell sh -c 'uname -m 2>/dev/null || echo not')

LIBDL=-ldl
INCLUDES=-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/$(ARCH)
ARCH_INCLUDE=$(ARCH)
JAVA_LIB_DIR=$(JAVA_HOME)/lib/$(CPU)
ifeq ($(uname_S),Linux)
ifeq ($(uname_M),x86_64)
	CPU=amd64
	ARCH=linux-amd64
	ARCH_INCLUDE=linux
else
	CPU=i386
	ARCH=linux
endif
endif
ifneq (,$(findstring MINGW,$(uname_S)))
	ARCH=win32
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
endif
	EXTRADEFS+= -DJNI_CREATEVM=\"JNI_CreateJavaVM_Impl\" -DMACOSX
	LIBMACOSX=-lpthread -framework CoreFoundation
endif
ifneq ($(CROSS_COMPILE_WIN64_ON_LINUX),)
	CXX=PATH="$$(pwd)/root-x86_64-pc-linux/bin:$$PATH" x86_64-pc-mingw32-g++
	ARCH=win64
	ARCH_INCLUDE=win32
	EXTRADEFS+= -DMINGW32
	LIBDL=
	EXE=.exe
	STRIP_TARGET=1
endif

# FIXME: check if we can do without the -DJAVA_HOME etc. when building
# the debian packages.
CXXFLAGS=-g $(INCLUDES) $(EXTRADEFS) \
	-DJAVA_HOME=\"$(JAVA_HOME)\" -DJAVA_LIB_PATH=\"$(JAVA_LIB_PATH)\"
LIBS=$(LIBDL) $(LIBMACOSX)

all: $(SUBMODULE_TARGETS_IN_FIJI) $(JARS) src-plugins run

$(TARGET)$(EXE): fiji.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)
ifeq ($(STRIP_TARGET),1)
	strip $@
endif

fiji.o: fiji.cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

ifeq ($(FIJI_ARGS),)
ifneq ($(FIJI_RUN_PLUGIN),)
FIJI_ARGS=-eval 'run("$(FIJI_RUN_PLUGIN)");'
endif
endif

run: $(JDK) $(TARGET)$(EXE)
ifeq ($(CROSS_COMPILE_WIN64_ON_LINUX),)
	./$(TARGET)$(EXE) $(FIJI_ARGS)
endif

# submodules

.PHONY: $(SUBMODULE_TARGETS_IN_FIJI)
$(SUBMODULE_TARGETS_IN_FIJI):
	@echo "Making $@"
	ORIGINAL_TARGET=$(shell echo " $(SUBMODULE_TARGETS) " | \
		sed "s/.* \([^ ]*$$(basename "$@")\) .*/\1/") && \
	DIR=$$(dirname $$ORIGINAL_TARGET) && \
	test ! -e $$DIR/Makefile || ( \
		$(MAKE) -C $$DIR $$(basename $$ORIGINAL_TARGET) && \
		./scripts/copy-jar-if-newer.sh $$ORIGINAL_TARGET $@ \
	)

# MicroManager
mm:
	cd micromanager1.1 && sh build.sh

# JDK
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

.PHONY: src-plugins
src-plugins:
	$(MAKE) -C $@

check: src-plugins $(TARGET)$(EXE)
	./$(TARGET)$(EXE) -eval 'run("Get Class Versions"); run("Quit");' | \
		sort

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
	echo '		<string>run_fiji.sh</string>' >> $(PLIST)
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
	cp fiji-macosx $(MACOS)/
	cp fiji-macosx-intel $(MACOS)/
	for d in java plugins macros ij.jar jars; do \
		test -h $(MACOS)/$$d || ln -s ../../$$d $(MACOS)/; \
	done
	git archive --prefix=$@/java/macosx/ origin/java/macosx: | \
		tar xvf -
	git archive --prefix=$@/java/macosx-intel/ origin/java/macosx-intel: | \
		tar xvf -
	cp ij.jar $@/
	cp -R plugins $@/
	cp -R macros $@/
	cp -R jars $@/
	cp scripts/run_fiji.sh $(MACOS)
	cp images/Fiji.icns $(RESOURCES)


# ---- Some rules only useful for Debian packaging .... ---------------

FIJI_VERSION=0.9.0
IDEAL_DIRECTORY=fiji-$(FIJI_VERSION)
CURRENT_DIRECTORY=$(shell pwd | sed 's/^.*\///')
ORIG=fiji_$(FIJI_VERSION).orig.tar.gz

.PHONY: orig

orig: ../$(ORIG)

../fiji_$(FIJI_VERSION).orig.tar.gz :
	if [ "$(CURRENT_DIRECTORY)" != "$(IDEAL_DIRECTORY)" ]; \
	then \
		echo The source directory must be called $(IDEAL_DIRECTORY); \
	fi
	( cd .. && tar czvf $(ORIG) -X $(IDEAL_DIRECTORY)/exclude-from-source-archive $(IDEAL_DIRECTORY) )

.PHONY: clean

EXTRACLEANFILES=ImageJA/plugins/MacAdapter.class \
	VIB/Quick3dApplet-1.0.8.jar \
	VIB/jzlib-1.0.7.jar \
	VIB/junit-4.4.jar \
	VIB/VIB_.jar \
	ij.jar

clean:
	echo JAVA_HOME is $(JAVA_HOME)
	$(MAKE) -C ImageJA clean
	( cd ImageJA && ant clean )
	$(MAKE) -C VIB clean
	find . -name '*~' -exec rm {} \;
	for d in plugins src-plugins libs; do \
		( cd $$d && find . \( -name '*.class' -o -name '*.jar' \) -a -exec rm {} \; ) \
	done
	rm -f $(EXTRACLEANFILES)
	rm -f fiji-linux fiji-win32.exe fiji-linux-amd64  fiji-win64.exe  fiji-macosx ij.jar
	rm -rf api
	rm -f fiji.o

.PHONY: debs

debs:
	dpkg-buildpackage -i.git -I.git -rfakeroot -us -uc

.PHONY: build-imageja build-fiji-launcher build-fiji-plugins
.PHONY: build-doc-imageja build-doc-fiji-launcher build-doc-fiji-plugins

.PHONY: install-imageja install-fiji-launcher install-fiji-plugins
.PHONY: install-doc-imageja install-doc-fiji-launcher install-doc-fiji-plugins

# For the imageja package:

build-imageja: ij.jar

install-imageja:
	echo Installing ImageJA to prefix $(DESTDIR)
	install -d $(DESTDIR)/usr/bin/
	install -m 755 simple-launcher $(DESTDIR)/usr/bin/imageja
	install -d $(DESTDIR)/usr/share/imageja/
	install -m 644 ij.jar $(DESTDIR)/usr/share/imageja/
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	install -d $(DESTDIR)/usr/share/imageja/jars/

build-doc-imageja:
	( cd ImageJA && ant javadocs )

install-doc-imageja:
	install -d $(DESTDIR)/usr/share/doc/imageja/
	cp -r api $(DESTDIR)/usr/share/doc/imageja/
	chmod a=rX $(DESTDIR)/usr/share/doc/imageja/

# For the fiji-launcher package:

build-fiji-launcher: fiji-linux

install-fiji-launcher: fiji-linux
	install -m 755 fiji-linux $(DESTDIR)/usr/bin/fiji

# For the fiji-plugins package:

build-fiji-plugins: ij.jar $(JARS) src-plugins
	echo JARS is $(JARS)

install-fiji-plugins: $(JARS)
	cp -r plugins/* $(DESTDIR)/usr/share/imageja/plugins/

# ---------------------------------------------------------------------
