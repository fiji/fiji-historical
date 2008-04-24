JARS3RDPARTY=$(shell find ../jars/ -name "*.jar" -printf ":%h/%f")
JAVACOPTS=-target 1.3 -source 1.3 \
	-classpath .:../ij.jar:../plugins/TrakEM2_.jar$(JARS3RDPARTY)

JARDIRS=common Jython Clojure
JARS=Jython_Interpreter.jar Clojure_Interpreter.jar

JAVAS=$(filter-out $(wildcard $(patsubst %,%/*.java,$(JARDIRS))),$(wildcard *.java */*.java))
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
DESTCLASSES=$(patsubst %,../plugins/%,$(CLASSES))
DESTJARS=$(patsubst %,../plugins/%,$(JARS))

all: $(DESTCLASSES) $(DESTJARS)

# javac will generate classes that aren't in DESTCLASSES (e.g. inner
# classes) so copy over every class file we find under this directory:
$(DESTCLASSES): ../plugins/%: %
	for i in $(wildcard $(patsubst %.class,%*.class,$^)); do \
		mkdir -p `dirname ../plugins/$$i`; \
		cp $$i ../plugins/$$i; \
	done

%.class: %.java
	javac $(JAVACOPTS) $<

$(DESTJARS): ../plugins/%: %
	echo Testing whether to copy $<
	../scripts/copy-jar-if-newer.sh $< $@

ifeq ($(TARGET),)

Jython_Interpreter.jar: SOURCES=common/*.java Jython/*.java Jython/plugins.config

Clojure_Interpreter.jar: SOURCES=common/*.java Clojure/*.java Clojure/plugins.config

.PHONY: $(JARS)

$(JARS):
	$(MAKE) TARGET=$@ SOURCES='$(SOURCES)' $@

else

$(TARGET): $(patsubst %.java,%.class,$(wildcard $(SOURCES)))
	echo $^
	jar cf $@  $(patsubst %.java,%*.class,$(wildcard $(SOURCES)))

endif
