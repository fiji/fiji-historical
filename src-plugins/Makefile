JAVACOPTS=-target 1.3 -source 1.3 \
	-classpath .:../ij.jar:../plugins/TrakEM2_.jar:../jars/jpedalSTD.jar:../jars/jruby.jar
JARS=common Jython JRuby
JAVAS=$(filter-out $(wildcard $(patsubst %,%/*.java,$(JARS))),$(wildcard *.java */*.java))
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
DESTCLASSES=$(patsubst %,../plugins/%,$(CLASSES))

all: $(DESTCLASSES)

# javac will generate classes that aren't in DESTCLASSES (e.g. inner
# classes) so copy over every class file we find under this directory:
$(DESTCLASSES): ../plugins/%: %
	for i in $(wildcard $(patsubst %.class,%*.class,$^)); do \
		mkdir -p `dirname ../plugins/$$i`; \
		cp $$i ../plugins/$$i; \
	done

%.class: %.java
	javac $(JAVACOPTS) $<

Jython_Interpreter.jar: common/*.class Jython/*.class Jython/plugins.config
	jar cf $@ $^
