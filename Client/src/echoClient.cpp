#include <stdlib.h>
#include "../include/connectionHandler.h"
#include <boost/locale.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/lexical_cast.hpp>
#include <fstream>
#include <string>
#include "../include/Packet.h"

using namespace std;
static string fileName;
static vector<Packet> dataVector;
static bool closed = false;
/***
 * This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
 */

string subString(string& str, int start, int end){
    string res = "";
    for (int i = start ; i <= end ; i++){
        res += str.at(i);
    }
    return res;
}

Packet stringToPacket(string& userInput){
    switch (userInput.at(0)) {//TODO check if validation check is needed
        case 'R': {
            if (boost::starts_with(userInput, "RRQ ") /*&& userInput.find('0') != string::npos*/)
                if (userInput.length() > 4)
                    return Packet(1,userInput.substr(4, userInput.length() - 4));
            return Packet(5,(short)0, userInput + " IS AN ILLEGAL COMMAND");
        }
        case 'W': {
            if (boost::starts_with(userInput, "WRQ ") /*&& userInput.find('0') != string::npos*/)
                if (userInput.length() > 4)
                    return Packet(2,userInput.substr(4, userInput.length() - 4));
            return Packet(5,(short)0, userInput + " IS AN ILLEGAL COMMAND");
        }
        case 'L': {
            if (boost::starts_with(userInput, "LOGRQ ")/*&& userInput.find('0') != string::npos*/)
                if (userInput.length() > 6)
                    return Packet(7,userInput.substr(6, userInput.length() - 6));
            return Packet(5,(short)0, userInput + " IS AN ILLEGAL COMMAND");
        }
        case 'D': {
            if (userInput.at(1) == 'E') { //only DELRQ possible
                if (boost::starts_with(userInput, "DELRQ ") /*&& userInput.find('0') != string::npos*/)
                    if (userInput.length() > 6){
                        string toDelete = subString(userInput,6,userInput.length()-1);
                        return Packet(8,toDelete);
                    }
            }
            else {
                if (userInput.at(1) == 'I') {
                    if (userInput.compare("DIRQ") == 0)
                        return Packet(6);
                    if (userInput.compare("DISC") == 0)
                        return Packet(10);
                }
            }
            return Packet(5,(short)0, userInput + " IS AN ILLEGAL COMMAND");
        }
        default:
            return Packet(5,(short)0, userInput + " IS AN ILLEGAL COMMAND");
    }
}


vector<Packet> fileToPackets(vector<char> fileBytes, int size){
    vector<Packet> toGet;
    int fileCounter = 0;
    short blockCounter = 1;
    while (size >= 512) {
        vector<char> currPacketData;
        for (unsigned int i = 0 ; i < 512 ; i++)
            currPacketData.push_back(fileBytes[fileCounter++]);
        toGet.push_back(Packet(3,512, blockCounter++, currPacketData));
        size -= 512;
    }
    vector<char> currPacketData;
    for (int i = 0 ; i < size ; i++)
        currPacketData.push_back(fileBytes[fileCounter++]);
    toGet.push_back(Packet(3,size, blockCounter, currPacketData));
    return toGet;
}

static vector<char> ReadAllBytes(const char* myFile)
{
    ifstream stream(myFile, ios::binary | ios::ate);
    ifstream::pos_type posT = stream.tellg();
    vector<char> byteVector(posT);
    stream.seekg(0, ios::beg);
    stream.read(&byteVector[0], posT);
    return byteVector;
}

void  serverThread(ConnectionHandler & handler, bool &isClosed){
    while (1){
        if (closed)
            break;
        Packet* packet = (handler.receive());
        if (packet != 0) {
            short opCode = packet->getOpCode();
            if (opCode == 3) { //DATA, request(6) for DIRQ or request(1) for RRQ
                if (packet->getPacketSize() > 512) {
                    dataVector.clear(); //TODO check if needed to delete all the data vector in case of error!!!!
                } else
                    handler.sendPacket(Packet(4, packet->getBlockNumber()));
                dataVector.push_back(*packet);
                if (packet->getPacketSize() < 512) {
                    if (handler.getRequest() == 1) { //current packet is the last one of RRQ
                        ofstream myfile(fileName, ofstream::binary);
                        for (unsigned int i = 0; i < dataVector.size(); i++) {
                            myfile.write(dataVector.at(i).getDataBytes().data(), dataVector.at(i).getPacketSize());
                        }
                        myfile.close();
                        dataVector.clear();
                        handler.printToScreen("RRQ " + fileName + " complete");
                    } else { //DIRQ last data packet accepted;

                        int size = dataVector.size();
                        for (int i = 0; i < size; i++) {
                            Packet packet = dataVector[i];
                            string fileName = "";
                            for (int j = 0; j < packet.getPacketSize(); j++) {
                                if (packet.getDataBytes()[j] == '\0') {
                                    handler.printToScreen(fileName);
                                    fileName = "";
                                } else
                                    fileName += packet.getDataBytes()[j]; //chain the char at[j] to filename
                            }
                        }
                        dataVector.clear();
                    }
                }
            }
            else {
                if(opCode == 4) { //ACK
                    handler.printToScreen("ACK " + to_string(packet->getBlockNumber()));
                    if (packet->getBlockNumber() == 0) {
                        if (handler.getRequest() == 10) { // disconnect request accepted
                            closed = true;
                            handler.close();
                        }
                        if (handler.getRequest() == 2) { //Write request accepted

                            vector<char> fileBytes = ReadAllBytes(fileName.c_str());
                            dataVector = fileToPackets(fileBytes, fileBytes.size());
                            handler.sendPacket(dataVector[0]);
                            dataVector.erase(dataVector.begin());
                        }
                    } else { //ack num > 0, therefore an ack for a data packet
                        if (!dataVector.empty()) {
                            handler.sendPacket(dataVector[0]);
                            dataVector.erase(dataVector.begin());
                        } else
                            handler.printToScreen("WRQ " + fileName + " complete");
                    }
                }
                else {
                    if (opCode == 5){ //ERROR
                        cout << "Error " << packet->getErrorCode() << endl;
                        dataVector.clear();
                    }
                    else {
                        if (opCode == 9) { //BAST
                            if (packet->getAddDel() == 1) { //file was added
                                handler.printToScreen("BCAST add " + packet->getString());
                            } else
                                handler.printToScreen("BCAST del " + packet->getString());
                        }
                    }
                }
            }
        }
        delete(packet);
    }
}

void keyBoardThread(ConnectionHandler &handler, bool &isClosed){
    while(1) {
        if (closed)
            break;
        string userInput;
        if (handler.getRequest() != 10) {
            handler.getLine(userInput);
            Packet inputPacket = stringToPacket(userInput); //TODO DELETE
            if (inputPacket.getOpCode() == 5) {//5 opCode is for Error packet in case of invalid input
                handler.printToScreen(inputPacket.getErrorMessage()); //TODO Maybe delete this!
            } else {
                if (inputPacket.getOpCode() == 1 || inputPacket.getOpCode() == 2) //write
                    fileName = subString(userInput, 4, userInput.size() - 1);
                if (inputPacket.getOpCode() == 8) //delete
                    fileName = subString(userInput, 6, userInput.size() - 1);
                handler.sendPacket(inputPacket);
            }
        }
    }
}

int main (int argc, char* argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    bool isClosed;
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    boost::thread server(serverThread,boost::ref(connectionHandler),boost::ref(isClosed));
    boost::thread keyboard(keyBoardThread,boost::ref(connectionHandler),boost::ref(isClosed));

    keyboard.join();

    return 0;
}