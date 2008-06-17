# TODO: micromanager
# TODO: precompiled/*
# TODO: tiger stuff
# TODO: fallback to copying from precompiled/ when submodule was not checked out

# This is a configuration file for Fiji mAKE ("fake")
#
# The syntax of a Fakefile is meant to be very simple.
#
# The first rule is the default rule.
#
# All rules are of the form
#
#	target <- prerequisites
#
# before making "target", all prerequisites will be made (i.e. if there
# exists a rule, for an item on the right side, it will be executed before
# the current rule).
#
# Most rules have implicit actions: if the target is a .jar file, the items
# on the right side are packaged into the target, compiling them first, if
# they are .java files.
#
# If the last item on the right side is a .cxx file, the GNU C++ compiler
# will be invoked to make the target from it.
#
# If an item on the right side is a directory, and a Fakefile or a Makefile
# exists in that directory, "fake" or "make" will be called in that directory.
# The target will be simply copied from that directory after handling all
# dependencies.
#
# There is a special type of rule when "fake" does not know how to produce
# the target from the prerequisites: you can call a program with
#
#	target[program] <- items
#
# This will check if the target is up-to-date, by checking the timestamps of the
# items (if there is no item, the target is deemed _not_ up-to-date). If the target
# is not up-to-date, "fake" will execute the program with all items as parameters.
#
# Variables are defined like this:
#
#	VARIABLE=VALUE
#
# and their values can be accessed with "$VARIABLE" in most places.
#
# You can define variables depending on the platform, the target, and in some
# cases the prerequisite, by adding a tag in parentheses to the name:
#
#	VARIABLE(target)=xyz
#	VARIABLE(platform)=abc

# These variables are special, as they will be interpreted by "fake".

# Be verbose
verbose=true

# Usually not necessary
debug=false

# Compile .java files for this Java version
javaVersion=1.5

# When building a .jar file, and a .config file of the same name is found in
# this directory, it will be included as "plugins.config".
pluginsConfigDirectory=staged-plugins

# When a prerequisite is a directory, but contains neither Fakefile nor
# Makefile, just ignore it
ignoreMissingFakefiles=true

# When a submodule could not be made, fall back to copying from this directory
precompiledDirectory=precompiled/

JAVA_HOME(linux)=java/linux/jdk1.6.0_06/jre
JAVA_HOME(linux64)=java/linux-amd64/jdk1.6.0_04/jre
JAVA_HOME(win32)=java/win32/jdk1.6.0_03/jre
JAVA_HOME(win64)=java/win64/jdk1.6.0_04/jre
JAVA_HOME(macosx)=java/macosx-java3d

# the main target

SUBMODULE_TARGETS=ij.jar \
	plugins/VIB_.jar \
	plugins/TrakEM2_.jar \
	plugins/mpicbg_.jar

PLUGIN_TARGETS=plugins/Jython_Interpreter.jar \
	plugins/Clojure_Interpreter.jar \
	plugins/JRuby_Interpreter.jar \
	plugins/BeanShell_Interpreter.jar \
	plugins/bUnwarpJ_.jar \
	plugins/register_virtual_stack_slices.jar \
	plugins/registration_3d.jar \
	plugins/IO_.jar \
	plugins/CLI_.jar \
	plugins/Javascript_.jar \
	plugins/lens_correction.jar \
	plugins/LSM_Toolbox.jar \
	\
	plugins/Analyze/Grid_.class \
	plugins/Filters/Preprocessor_Smooth.class \
	plugins/Input-Output/HandleExtraFileTypes.class \
	plugins/Stacks/Stack_Reverser.class \
	plugins/Examples/Example_Plot.class \
	plugins/Utilities/Command_Launcher.class \
	\
	misc/Fiji.jar

all <- jdk fiji $SUBMODULE_TARGETS $PLUGIN_TARGETS

# The "run" rule just executes ./fiji (as long as the file "run" does not exist...)
# It has items on the right side, because these would be passed to the executable.

run[] <- all run-fiji
run-fiji[./fiji] <-
DEBUG_ARGS=-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n
dev[./fiji $DEBUG_ARGS] <-


# JDK

JDK=java/$PLATFORM
JDK(linux64)=java/linux-amd64
JDK(macosx)=java/macosx-java3d

# Call the Jython script to ensure that the JDK is checked out (from Git)
jdk[scripts/checkout-jdk.py $JDK] <-

# From submodules
ij.jar <- ImageJA/
plugins/VIB_.jar <- VIB/
CLASSPATH(plugins/TrakEM2_.jar)=plugins/ij.jar
plugins/TrakEM2_.jar <- TrakEM2/
plugins/mpicbg_.jar <- mpicbg/

# From source
javaVersion(misc/Fiji.jar)=1.3
misc/Fiji.jar <- src-plugins/fiji/*.java

# These classes are common
COMMON=src-plugins/common/**/*.java
plugins/Jython_Interpreter.jar <- src-plugins/Jython/*.java $COMMON
plugins/Clojure_Interpreter.jar <- src-plugins/Clojure/*.java $COMMON
plugins/JRuby_Interpreter.jar <- src-plugins/JRuby/*.java $COMMON
plugins/BeanShell_Interpreter.jar <- src-plugins/BSH/*.java $COMMON
plugins/Javascript_.jar <- src-plugins/Javascript/*.java $COMMON

plugins/bUnwarpJ_.jar <- src-plugins/bUnwarpJ/*.java
plugins/register_virtual_stack_slices.jar <- \
	src-plugins/register_virtual_stack/*.java
plugins/registration_3d.jar <- src-plugins/registration3d/*.java
plugins/IO_.jar <- src-plugins/io/*.java
plugins/CLI_.jar <- src-plugins/CLI/*.java
plugins/lens_correction.jar <- src-plugins/lenscorrection/*.java
MAINCLASS(plugins/LSM_Toolbox.jar)=org.imagearchive.lsm.toolbox.gui.AboutDialog
plugins/LSM_Toolbox.jar <- src-plugins/LSM_Toolbox/**/*.java \
	src-plugins/LSM_Toolbox/**/*.png \
	src-plugins/LSM_Toolbox/**/*.jpg \
	src-plugins/LSM_Toolbox/**/*.html \
	src-plugins/LSM_Toolbox/**/*.txt

CLASSPATH(plugins/Filters/Preprocessor_Smooth.class)=plugins/TrakEM2_.jar
plugins/**/*.class <- src-plugins/**/*.java

MAINCLASS(jars/javac.jar)=com.sun.tools.javac.Main
JAVAVERSION(jars/javac.jar)=1.5
jars/javac.jar <- src-plugins/com/sun/tools/javac/**/*.java \
	src-plugins/com/sun/tools/javac/**/*.properties \
	src-plugins/com/sun/tools/javac/**/*.JavaCompilerTool \
	src-plugins/com/sun/source/**/*.java \

# Fiji launcher

JAVA_LIB_PATH(linux)=lib/i386/client/libjvm.so
JAVA_LIB_PATH(linux64)=lib/amd64/server/libjvm.so
JAVA_LIB_PATH(win32)=bin/client/jvm.dll
JAVA_LIB_PATH(win64)=bin/server/jvm.dll
JAVA_LIB_PATH(macosx)=

# The variables CFLAGS, CXXFLAGS, LDFLAGS and LIBS will be used for compiling
# C and C++ programs.
CXXFLAGS=-DJAVA_HOME='"$JAVA_HOME"' \
	-DJAVA_LIB_PATH='"$JAVA_LIB_PATH"' \
	-I$JAVA_HOME/../include
CXXFLAGS(linux)=$CXXFLAGS -I$JAVA_HOME/../include/linux
CXXFLAGS(linux64)=$CXXFLAGS -I$JAVA_HOME/../include/linux-amd64
WINOPTS=-mwindows -mno-cygwin -DMINGW32
CXXFLAGS(win32)=$CXXFLAGS -I$JAVA_HOME/../include/win32 $WINOPTS
CXXFLAGS(win64)=$CXXFLAGS -I$JAVA_HOME/../include/win64 $WINOPTS
MACOPTS=-I/System/Library/Frameworks/JavaVM.Framework/Headers \
	-DMACOSX -mmacosx-version-min=10.4 -arch ppc -arch i386
CXXFLAGS(macosx)=$CXXFLAGS $MACOPTS

# Include 64-bit architectures only in ./fiji (as opposed to ./fiji-tiger),
# and only on MacOSX
MACOSX_64BIT_ARCHS(macosx)=-arch ppc64 -arch x86_64
CXXFLAGS(fiji)=$CXXFLAGS $MACOSX_64BIT_ARCHS

LIBS(linux)=-ldl
LIBS(linux64)=-ldl
LIBS(macosx)=-framework CoreFoundation -framework JavaVM

fiji <- fiji.cxx

fiji-tiger-pita <- fiji-tiger-pita.cxx
fiji-tiger <- fiji.cxx

# Portable application/.app

app[scripts/make-app.py] <- all

# Fake itself

MAINCLASS(fake.jar)=Fake
JAVAVERSION(fake.jar)=1.3
fake.jar <- fake/Fake.java
