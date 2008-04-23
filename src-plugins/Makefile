JAVACOPTS=-target 1.3 -source 1.3 \
	-classpath ../ij.jar:../plugins/TrakEM2_.jar:../plugins/jpedalSTD.jar
JAVAS=$(wildcard *.java */*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
DESTCLASSES=$(patsubst %,../plugins/%,$(CLASSES))

all: $(DESTCLASSES)

# javac will generate classes that aren't in DESTCLASSES (e.g. inner
# classes) so copy over every class file we find under this directory:
$(DESTCLASSES): ../plugins/%: %
	for i in `find . -name '*.class'`; do mkdir -p `dirname $$i`; cp $$i ../plugins/$$i; done

$(CLASSES): %.class: %.java
	javac $(JAVACOPTS) $<
