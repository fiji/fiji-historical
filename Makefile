TARGET=fiji-$(ARCH)

# MicroManager
mm:
	cd micromanager1.1 && sh build.sh

# ------------------------------------------------------------------------
# Set up default for building the launcher on different
# architectures:

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

$(TARGET)$(EXE): fiji.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)
ifeq ($(STRIP_TARGET),1)
	strip $@
endif

fiji.o: fiji.cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

# ------------------------------------------------------------------------
# Run the launcher:

run: $(TARGET)$(EXE)
	./$(TARGET)$(EXE)

# ---- Some rules only useful for Debian packaging .... ---------------

FIJI_VERSION=0.9.0
IDEAL_DIRECTORY=fiji-$(FIJI_VERSION)
CURRENT_DIRECTORY=$(shell pwd | sed 's/^.*\///')
ORIG=fiji_$(FIJI_VERSION).orig.tar.gz

.PHONY: orig

orig: ../$(ORIG)

../$(ORIG) :
	if [ "$(CURRENT_DIRECTORY)" != "$(IDEAL_DIRECTORY)" ]; \
	then \
		echo The source directory must be called $(IDEAL_DIRECTORY); \
	fi
	( cd .. && tar czvf $(ORIG) -X $(IDEAL_DIRECTORY)/exclude-from-source-archive $(IDEAL_DIRECTORY) )

.PHONY: clean

EXTRACLEANFILES=VIB/Quick3dApplet-1.0.8.jar \
	VIB/jzlib-1.0.7.jar \
	VIB/junit-4.4.jar \
	VIB/VIB_.jar \
	ij.jar

clean:
	echo JAVA_HOME is $(JAVA_HOME)
	$(MAKE) -C ImageJA clean
	( cd ImageJA && ant clean )
	( cd TrakEM2 && ant clean )
	( cd jtk && ant clean )
	$(MAKE) -C VIB clean
	find . -name '*~' -exec rm {} \;
	for d in plugins src-plugins libs; do \
		( cd $$d && find . \( -name '*.class' -o -name '*.jar' \) -a -exec rm {} \; ) \
	done
	rm -f $(EXTRACLEANFILES)
	rm -f fiji-linux fiji-win32.exe fiji-linux-amd64  fiji-win64.exe  fiji-macosx ij.jar
	# Takes ages to build again:
	# rm -rf api
	rm -f fiji.o
	rm -f *-stamp

.PHONY: debs

DEBIAN_VERSION=$(shell perl -e 'use Dpkg::Changelog qw(parse_changelog); print parse_changelog->{version}')
DEBIAN_PACKAGES=$(shell egrep 'Package: ' debian/control | sed -r 's/Package: //')

debs:
	# FIXME: generate the regular expression automatically
	dpkg-buildpackage -i'((^|/).git(/|$$)|(^|/)java($$|/)|(^|/)api($$|/)|(^|/)cachedir($$|/)|(^|/)micromanager1.1($$|/))' \
		 -I.git -Ijava -Icachedir -Imicromanager1.1 -rfakeroot -k88855837
	echo DEBIAN_VERSION is $(DEBIAN_VERSION)
	echo DEBIAN_PACKAGES are $(DEBIAN_PACKAGES)
	mkdir -p packages
	rm -f packages/*
	cp $(patsubst %,../%_$(DEBIAN_VERSION)_*.deb,$(DEBIAN_PACKAGES)) packages/
	cp ../$(ORIG) packages/
	cp ../fiji_$(DEBIAN_VERSION).dsc packages/
	cp ../fiji_$(DEBIAN_VERSION)_i386.changes packages/

.PHONY: build-imageja build-fiji build-fiji-plugins
.PHONY: build-doc-imageja build-doc-fiji build-doc-fiji-plugins

.PHONY: install-imageja install-fiji install-fiji-plugins
.PHONY: install-doc-imageja install-doc-fiji install-doc-fiji-plugins

# For the imageja package:

build-imageja : ImageJA/ij.jar

ImageJA/ij.jar :
	( cd ImageJA && ant build )

install-imageja :
	echo Installing ImageJA to prefix $(DESTDIR)
	install -d $(DESTDIR)/usr/bin/
	install -m 755 simple-launcher $(DESTDIR)/usr/bin/imageja
	install -d $(DESTDIR)/usr/share/imageja/
	install -m 644 ImageJA/ij.jar $(DESTDIR)/usr/share/imageja/
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	install -m 644 debian/README.plugins $(DESTDIR)/usr/share/imageja/plugins/README

build-doc-imageja :
	# ( cd ImageJA && ant javadocs )

install-doc-imageja :
	install -d $(DESTDIR)/usr/share/doc/imageja/
	cp -r api $(DESTDIR)/usr/share/doc/imageja/
	chmod a=rX $(DESTDIR)/usr/share/doc/imageja/

# For the fiji package:

build-fiji : 
	# Nothing to be done any more...

install-fiji :
	install -d $(DESTDIR)/usr/bin/	
	( cd $(DESTDIR)/usr/bin && ln -s imageja fiji )

# For the fiji-plugins package:

build-fiji-plugins : ImageJA/ij.jar plugins/VIB_.jar

plugins/VIB_.jar plugins/TrakEM2_.jar : VIB/VIB_.jar staged-plugins/VIB_.config TrakEM2/TrakEM2_.jar staged-plugins/TrakEM2_.config
	ant compile
	ant add-all-configs

VIB/VIB_.jar :
	( cd VIB && ant compile )

install-fiji-plugins : build-fiji-plugins
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	cp -r plugins/* $(DESTDIR)/usr/share/imageja/plugins/
	rm $(DESTDIR)/usr/share/imageja/plugins/TrakEM2_.jar

# For the trakem2 package:

build-trakem2 TrakEM2/TrakEM2_.jar : ImageJA/ij.jar VIB/VIB_.jar jtk/build/jar/edu_mines_jtk.jar
	( cd TrakEM2 && ant compile )

jtk/build/jar/edu_mines_jtk.jar :
	( cd jtk && ant all )

install-trakem2 : build-trakem2
	install -d $(DESTDIR)/usr/share/java/
	install jtk/build/jar/edu_mines_jtk.jar $(DESTDIR)/usr/share/java/
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	cp -r plugins/TrakEM2_.jar $(DESTDIR)/usr/share/imageja/plugins/
