CXXFLAGS=-g -I linux/jdk1.6.0/include/ -I linux/jdk1.6.0/include/linux/
LDFLAGS=-L linux/jdk1.6.0/jre/lib/i386/client/
LIBS=-ljvm

launcher: launcher.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)

launcher.o: launcher.cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

run:
	JAVA_HOME=linux/jdk1.6.0/jre \
		LD_LIBRARY_PATH=linux/jdk1.6.0/jre/lib/i386/client/ \
		./launcher
