CXXFLAGS=-g -I linux/jdk1.6.0/include/ -I linux/jdk1.6.0/include/linux/
LIBS=-ldl

launcher: launcher.o
	$(CXX) $(LDFLAGS) -o $@ $< $(LIBS)

launcher.o: launcher.cxx Makefile
	$(CXX) $(CXXFLAGS) -c -o $@ $<

run:
	./launcher
