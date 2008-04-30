# ---- Some rules only useful for Debian packaging .... ---------------

.PHONY:   build-imageja   build-fiji   build-fiji-plugins   build-imageja-doc
.PHONY: install-imageja install-fiji install-fiji-plugins install-imageja-doc

# The build- rules called from debian/rules:

build-imageja : ImageJA/ij.jar

build-imageja-doc :
	cd ImageJA && ant javadocs

# Nothing to be done any more:
build-fiji : 

build-fiji-plugins : ImageJA/ij.jar plugins/VIB_.jar

build-trakem2 TrakEM2/TrakEM2_.jar : ImageJA/ij.jar VIB/VIB_.jar jtk/build/jar/edu_mines_jtk.jar
	( cd TrakEM2 && ant compile )

# The install- rules called from debian/rules.  Each has to make sure
# it installs into DESTDIR:

install-imageja :
	echo Installing ImageJA to prefix $(DESTDIR)
	install -d $(DESTDIR)/usr/bin/
	install -m 755 simple-launcher $(DESTDIR)/usr/bin/imageja
	install -d $(DESTDIR)/usr/share/imageja/
	install -m 644 ImageJA/ij.jar $(DESTDIR)/usr/share/imageja/
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	install -m 644 debian/README.plugins $(DESTDIR)/usr/share/imageja/plugins/README
	install -d $(DESTDIR)/usr/share/doc/imageja/
	install -m 644 LICENSES $(DESTDIR)/usr/share/doc/imageja/

install-imageja-doc :
	install -d $(DESTDIR)/usr/share/doc/imageja/
	cp -r api $(DESTDIR)/usr/share/doc/imageja/
	chmod a=rX $(DESTDIR)/usr/share/doc/imageja/
	install -d $(DESTDIR)/usr/share/doc/imageja-doc/
	install -m 644 LICENSES $(DESTDIR)/usr/share/doc/imageja-doc/

install-trakem2 : build-trakem2
	install -d $(DESTDIR)/usr/share/java/
	install jtk/build/jar/edu_mines_jtk.jar $(DESTDIR)/usr/share/java/
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	cp -r plugins/TrakEM2_.jar $(DESTDIR)/usr/share/imageja/plugins/
	install -d $(DESTDIR)/usr/share/doc/trakem2/
	install -m 644 LICENSES $(DESTDIR)/usr/share/doc/trakem2/

install-fiji-plugins : build-fiji-plugins
	install -d $(DESTDIR)/usr/share/imageja/plugins/
	cp -r plugins/* $(DESTDIR)/usr/share/imageja/plugins/
	rm $(DESTDIR)/usr/share/imageja/plugins/TrakEM2_.jar
	install -d $(DESTDIR)/usr/share/doc/fiji-plugins/
	install -m 644 LICENSES $(DESTDIR)/usr/share/doc/fiji-plugins/

install-fiji :
	install -d $(DESTDIR)/usr/bin/	
	( cd $(DESTDIR)/usr/bin && ln -s imageja fiji )
	install -d $(DESTDIR)/usr/share/doc/fiji/
	install -m 644 LICENSES $(DESTDIR)/usr/share/doc/fiji/

# ------------------------------------------------------------------------

FIJI_VERSION=0.9.0
IDEAL_DIRECTORY=fiji-$(FIJI_VERSION)
CURRENT_DIRECTORY=$(shell pwd | sed 's/^.*\///')
ORIG=fiji_$(FIJI_VERSION).orig.tar.gz

DEBIAN_VERSION=$(shell perl -e 'use Dpkg::Changelog qw(parse_changelog); print parse_changelog->{version}')
DEBIAN_PACKAGES=$(shell egrep 'Package: ' debian/control | sed -r 's/Package: //')

# The slightly ugly rule for generating the package files.  (In
# particular, the regular expression should be generated...)
.PHONY: debs
debs:
	dpkg-buildpackage -i'((^|/).git(/|$$)|(^|/)java($$|/)|(^|/)jtk/lib($$|/)|(^|/)jtk/jar($$|/)|(^|/)api($$|/)|(^|/)cachedir($$|/)|(^|/)micromanager1.1($$|/))' \
		 -I.git -Ijava -Icachedir -Imicromanager1.1 -rfakeroot -k88855837
	echo DEBIAN_VERSION is $(DEBIAN_VERSION)
	echo DEBIAN_PACKAGES are $(DEBIAN_PACKAGES)
	mkdir -p packages
	rm -f packages/*
	cp $(patsubst %,../%_$(DEBIAN_VERSION)_*.deb,$(DEBIAN_PACKAGES)) packages/
	cp ../$(ORIG) packages/
	cp ../fiji_$(DEBIAN_VERSION).dsc packages/
	cp ../fiji_$(DEBIAN_VERSION).diff.gz packages/
	cp ../fiji_$(DEBIAN_VERSION)_i386.changes packages/

# Build the "upstream" source archive.  It's a bit silly doing things
# this way, since this orig.tar.gz file represents the upstream tree
# after a lot of Debian specific changes, rather than recording those
# changes in the diff.  (However, we couldn't very well distribute all
# of the stuff in the master branch even in the orig.tar.gz.)  Really
# this should be a native package build from the debian branch, I
# think.

.PHONY: orig
orig: ../$(ORIG)

../$(ORIG) :
	if [ "$(CURRENT_DIRECTORY)" != "$(IDEAL_DIRECTORY)" ]; \
	then \
		echo The source directory must be called $(IDEAL_DIRECTORY); \
	fi
	( cd .. && tar czvf $(ORIG) -X $(IDEAL_DIRECTORY)/exclude-from-source-archive $(IDEAL_DIRECTORY) )

# ------------------------------------------------------------------------

.PHONY: release
release:
	scp packages/* longair@pacific.mpi-cbg.de:/var/www/downloads/apt/
	ssh longair@pacific.mpi-cbg.de /home/longair/bin/update-apt-repository

# ------------------------------------------------------------------------
# Rules for building the various class files and jars we want to include:

ImageJA/ij.jar :
	( cd ImageJA && ant build )

plugins/VIB_.jar plugins/TrakEM2_.jar : VIB/VIB_.jar staged-plugins/VIB_.config TrakEM2/TrakEM2_.jar staged-plugins/TrakEM2_.config
	ant compile
	ant add-all-configs

VIB/VIB_.jar :
	( cd VIB && ant compile )

# This is needed by TrakEM2:

jtk/build/jar/edu_mines_jtk.jar :

	( cd jtk && ant all )

# ------------------------------------------------------------------------
# Very thorough cleaning:

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
	rm -rf api
	rm -f fiji.o
	rm -f *-stamp
	rm -rf packages/
