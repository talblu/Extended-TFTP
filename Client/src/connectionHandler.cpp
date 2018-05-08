#include "../include/connectionHandler.h"
#include <vector>
#include <fstream>

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;

ConnectionHandler::ConnectionHandler(string host, short port): host_(host), port_(port), io_service_(), socket_(io_service_), encdec(EncoderDecoder()), lock(){
}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    try {
        tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
        socket_.connect(endpoint, error);
        if (error)
            throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getBytes(char* bytes, unsigned int bytesToRead) {
    size_t tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
            tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);
        }
        if(error)
            throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
    boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
        if(error)
            throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed sendBytes(Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

void ConnectionHandler::getLine(string &userInput){ //TODO check deadLock????
    getline(cin, userInput, '\n');
}


bool ConnectionHandler::sendFrameAscii(const std::string& frame, char delimiter) {
    bool result=sendBytes(frame.c_str(),frame.length());
    if(!result) return false;
    return sendBytes(&delimiter,1);
}

// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}

void ConnectionHandler::sendPacket(Packet p){
    mutex lock2;
    lock2.lock();
    vector<char> bytesVector = encdec.encode(&p);
    if (!bytesVector.empty()) //an empty vector implies an invalid request
        sendBytes(bytesVector.data(), bytesVector.size());
    lock2.unlock();
    bytesVector.clear();
}

Packet* ConnectionHandler::receive() {
    vector<char> byteVector;
    char charToGet[2] = {};
    getBytes(charToGet, 2);
    byteVector.push_back(charToGet[0]);
    byteVector.push_back(charToGet[1]); //now opCodeByte holds the 2 bytes to be converted to the opcode
    short opCode = encdec.bytesToShort(byteVector, 0);
    switch (opCode) {
        case 3: {
            getBytes(charToGet, 2);
            byteVector.push_back(charToGet[0]);
            byteVector.push_back(charToGet[1]);
            short packetSize = encdec.bytesToShort(byteVector, 2);
            for (int i = 0; i < packetSize + 2; i++) {
                getBytes(charToGet, 1);
                byteVector.push_back(charToGet[0]);
            }
            return encdec.decode(byteVector);
        }
        case 4: { //ACK
            getBytes(charToGet, 2);
            byteVector.push_back(charToGet[0]);
            byteVector.push_back(charToGet[1]);
            return encdec.decode(byteVector);
        }
        case 5: { //ERROR
            char singleChar [1];
            getBytes(charToGet, 2);
            byteVector.push_back(charToGet[0]);
            byteVector.push_back(charToGet[1]);
            getBytes(singleChar, 1);
            byteVector.push_back(singleChar[0]);
            while (singleChar[0] != '\0') {
                getBytes(singleChar, 1);
                byteVector.push_back(singleChar[0]);
            }
            return encdec.decode(byteVector);
        }
        case 9: {
            getBytes(charToGet,1);
            byteVector.push_back(charToGet[0]);
            do {
                getBytes(charToGet, 1);
                byteVector.push_back(charToGet[0]);
            } while (*charToGet != '\0');
            return encdec.decode(byteVector);
        }
        default: {
            return 0;
        }
    }
}

short ConnectionHandler::getRequest(){
    return encdec.getRequest();
}

void ConnectionHandler::printToScreen(string message){
    cout << message  << endl;
}