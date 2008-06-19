all: run

run: fiji
	./fiji --fake -- run

fiji: fiji.cxx fake.jar
	./fiji --fake -- fiji || \
	java -classpath fake.jar Fake jdk fiji

fake.jar: fake/Fake.java
	./fiji --fake fake.jar || \
	(cd fake && \
	 javac Fake.java && \
	 jar cvf ../fake.jar Fake*.class)

check: check-class-versions check-precompiled check-submodules

check-class-versions:
	./fiji --headless --main-class=fiji.CheckClassVersions \
		plugins/ jars/ misc/ precompiled/

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

precompiled/fiji-tiger: fiji.o
	$(CXX)  -arch ppc -arch i386 -mmacosx-version-min=10.4 -o $@ $< \
		$(LIBMACOSX)

precompiled/fiji-tiger-pita: fiji-tiger-pita.o
	$(CXX) -arch ppc -arch i386 -mmacosx-version-min=10.4 -o $@ $<

fiji-tiger-pita.o: fiji-tiger-pita.c
	$(CXX) -arch ppc -arch i386 -mmacosx-version-min=10.4 -c -o $@ $<


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
	sh scripts/mkdmg.sh $@ $< plugins macros

dmg: fiji-macosx.dmg
