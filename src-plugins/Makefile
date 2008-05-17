JARS3RDPARTY=$(shell find ../jars/ -name "*.jar" -exec printf ":%s" {} \;)
JAVACOPTS=-target 1.5 -source 1.5 \
	-classpath .:../ij.jar:../plugins/TrakEM2_.jar$(JARS3RDPARTY)

# common is just in this list to exclude classes within it from being copied
JARDIRS=common Jython Clojure JRuby BSH bUnwarpJ register_virtual_stack registration3d io fiji CLI Javascript
FINDJARDIRS=$(patsubst %,-name % -o,$(JARDIRS))
JARS=Jython_Interpreter.jar Clojure_Interpreter.jar JRuby_Interpreter.jar BeanShell_Interpreter.jar bUnwarpJ_.jar register_virtual_stack_slices.jar registration_3d.jar IO_.jar Fiji.jar CLI_.jar Javascript_.jar

JAVAS=$(filter-out $(wildcard $(patsubst %,%/*.java,$(JARDIRS))),$(wildcard *.java */*.java))
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
DESTJARS=$(patsubst %,../plugins/%,$(JARS))

.PHONY: copyclasses

all: $(DESTJARS) copyclasses

# javac will generate classes that aren't in DESTCLASSES (e.g. inner
# classes) so copy over every class file we find under this directory
# that isn't in one of the JARDIR subdirectories:
copyclasses: $(CLASSES)
	for i in `find . \( $(FINDJARDIRS) -false \) -prune -o \( -type f -a -name '*.class' \) -print`; do \
		mkdir -p `dirname ../plugins/$$i`; \
		cp -v $$i ../plugins/$$i; \
	done

%.class: %.java
	javac $(JAVACOPTS) $<

$(DESTJARS): ../plugins/%: %
	echo Testing whether to copy $<
	../scripts/copy-jar-if-newer.sh $< $@

ifeq ($(TARGET),)

Jython_Interpreter.jar: SOURCES=common/*.java Jython/*.java Jython/plugins.config

Clojure_Interpreter.jar: SOURCES=common/*.java Clojure/*.java Clojure/plugins.config

JRuby_Interpreter.jar: SOURCES=common/*.java JRuby/*.java JRuby/plugins.config

BeanShell_Interpreter.jar: SOURCES=common/*.java BSH/*.java BSH/plugins.config

bUnwarpJ_.jar: SOURCES=bUnwarpJ/*.java bUnwarpJ/plugins.config

register_virtual_stack_slices.jar: SOURCES=register_virtual_stack/*.java register_virtual_stack/plugins.config

registration_3d.jar: SOURCES=registration3d/*.java registration3d/plugins.config

CLI_.jar: SOURCES=CLI/*.java CLI/plugins.config

Javascript_.jar: SOURCES=Javascript/*.java Javascript/plugins.config

# Special rule for non-packaged IO plugins present within the src-plugins/Input-Output folder
IO_.jar: SOURCES=io/*.java io/plugins.config

Fiji.jar: SOURCES=fiji/*.java

.PHONY: $(JARS)

$(JARS):
	$(MAKE) TARGET=$@ SOURCES='$(SOURCES)' $@

else

$(TARGET): $(patsubst %.java,%.class,$(wildcard $(SOURCES)))
	echo $^
	jar cf $@  $(patsubst %.java,%*.class,$(wildcard $(SOURCES)))

endif
