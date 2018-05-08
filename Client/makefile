CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_locale -lboost_thread -lboost_filesystem

all: runClient
	g++ -o bin/TFTPclient bin/connectionHandler.o bin/echoClient.o bin/EncoderDecoder.o $(LDFLAGS)

runClient: bin/connectionHandler.o bin/echoClient.o bin/EncoderDecoder.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/echoClient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp

bin/EncoderDecoder.o: src/EncoderDecoder.cpp
	g++ $(CFLAGS) -o bin/EncoderDecoder.o src/EncoderDecoder.cpp

	
.PHONY: clean
clean:
	rm -f bin/*