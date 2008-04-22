JAVACOPTS=-target 1.3 -source 1.3 \
	-classpath ../ij.jar:../plugins/TrakEM2_.jar:../plugins/jpedalSTD.jar
JAVAS=$(wildcard *.java */*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
DESTCLASSES=$(patsubst %,../plugins/%,$(CLASSES))

all: $(DESTCLASSES)

$(DESTCLASSES): ../plugins/%: %
	cp $< $@

$(CLASSES): %.class: %.java
	javac $(JAVACOPTS) $<
