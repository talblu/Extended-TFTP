
#ifndef INCLUDE_PACKET_H_
#define INCLUDE_PACKET_H_

#include<string>
#include<vector>

using namespace std;

class Packet{
private:
	short opCode;
    string str;
    short packetSize;
    short blockNum;
    short errorCode;
    vector<char> dataBytes;
    char addDel;

public:
	Packet(short op):opCode(op), str(), packetSize(), blockNum(), errorCode(), dataBytes(), addDel(){}; //DIRQ, DISC
    Packet(short op, string Name):opCode(op), str(Name), packetSize(), blockNum(), errorCode(), dataBytes(), addDel(){}; //RRQ, WRQ, LOGRQ, DELRQ
    Packet(short op, short size, short num,vector<char> bytes): opCode(op),str(), packetSize(size), blockNum(num), errorCode(), dataBytes(bytes), addDel(){}; //DATARQ
    Packet(short op, short num):opCode(op), str(), packetSize(), blockNum(num), errorCode(), dataBytes(), addDel(){};//ACK
    Packet(short op, short code, string msg):opCode(op), str(msg), packetSize(), blockNum(), errorCode(code), dataBytes(), addDel(){};//ERROR
    Packet(short op, char addel, string msg):opCode(op), str(msg), packetSize(), blockNum(), errorCode(), dataBytes(), addDel(addel){}; //BCAST
    Packet(const Packet& toCopy):opCode(toCopy.opCode), str(toCopy.str), packetSize(toCopy.packetSize), blockNum(toCopy.blockNum), errorCode(toCopy.errorCode) ,dataBytes(toCopy.dataBytes),addDel(toCopy.addDel) {};
	short getOpCode() {return opCode;};
    string getString() {return str;};
    short getPacketSize() {return packetSize;};
    short getBlockNumber() {return blockNum;};
    void addDataBytes(vector<char> &vec) {for (int i = 0; i<packetSize; i++) vec.push_back(dataBytes[i]);};
    vector<char> getDataBytes() {return dataBytes;};
    short getErrorCode(){return errorCode;};
    string getErrorMessage(){return str;};
    char getAddDel(){return addDel;};
	virtual ~Packet(){};
};

#endif /* INCLUDE_PACKET_H_ */