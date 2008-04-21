JAVACOPTS=-target 1.3 -source 1.3 -classpath ../ij.jar
JAVAS=$(wildcard *.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
DESTCLASSES=$(patsubst %,../plugins/%,$(CLASSES))

all: $(DESTCLASSES)

$(DESTCLASSES): ../plugins/%: %
	cp $< $@

$(CLASSES): %.class: %.java
	javac $(JAVACOPTS) $<
