// encoder.cpp : test encoder functionality
// Use boost::locale::conv to convert encodings

#include "../include/EncoderDecoder.h"
#include <iostream>
#include <fstream>
#include "../include/Packet.h"
#include <string>
#include <vector>
#include <boost/filesystem.hpp>


EncoderDecoder::EncoderDecoder(): request(0),collection(vector<char>()),loggedIn(false){
}

vector<char> EncoderDecoder::encode(Packet* msg){
    collection = vector<char>();
	switch (msg->getOpCode()){
        case 1: { //RRQ
            vector<char> emptyVector;
            shortToBytes(msg->getOpCode(), collection);
            string fileName = msg->getString();
            fileName += '\0';
            for (unsigned int i = 0; i<fileName.length(); i++)
                collection.push_back(fileName[i]);
            setRequest(1);
            break;
        }
        case 2: { //WRQ
            boost::filesystem::path filePath(msg->getString());
            if (boost::filesystem::exists(filePath)) { //file exists
                shortToBytes(msg->getOpCode(), collection);
                string fileName = msg->getString();
                fileName += '\0';
                for (unsigned int i = 0; i < fileName.length(); i++)
                    collection.push_back(fileName[i]);
                setRequest(2);
            }
            break;
        }
		case 3: {//DATA
			shortToBytes(msg->getOpCode(), collection);
			shortToBytes(msg->getPacketSize(), collection);
			shortToBytes(msg->getBlockNumber(), collection);
			msg->addDataBytes(collection);
			break;
		}
		case 4: {//ACK
			shortToBytes(msg->getOpCode(), collection);
			shortToBytes(msg->getBlockNumber(), collection);
			break;
		}
		case 5: {//ERROR
			string errorMsg = msg->getErrorMessage();
			errorMsg += '\0';
			shortToBytes(msg->getOpCode(), collection);
			shortToBytes(msg->getErrorCode(), collection);
			for (unsigned int i = 0; i<errorMsg.length(); i++)
				collection.push_back(errorMsg[i]);
            setRequest(0);
			break;
		}
        case 6: { //DIRQ
            shortToBytes(msg->getOpCode(), collection);
            setRequest(6);
            break;
        }
        case 7: { //LOGRQ
            shortToBytes(msg->getOpCode(), collection);
            string userName = msg->getString();
            userName += '\0';
            for (unsigned int i = 0; i<userName.length(); i++)
                collection.push_back(userName[i]);
            setRequest(7);
            break;
        }
        case 8: { //DELRQ
            shortToBytes(msg->getOpCode(), collection);
            string filename = msg->getString();
            filename += '\0';
            for (unsigned int i = 0; i<filename.length(); i++)
                collection.push_back(filename[i]);
            setRequest(8);
            break;
        }
		case 9: {//BCAST
			char delAdd = msg->getAddDel();
			string fileName = msg->getString();
			fileName += '\0';
			shortToBytes(msg->getOpCode(), collection);
			collection.push_back(delAdd);
			for (unsigned int i = 0; i<fileName.length(); i++)
				collection.push_back(fileName[i]);
            //setRequest(9);
			break;
		}
        case 10: { //DISC
            shortToBytes(msg->getOpCode(), collection);
            if (loggedIn)
                setRequest(10);
            break;
        }
	}
	return collection;
}

Packet* EncoderDecoder::decode(vector<char> message) {
	//notice that the top 128 ASCII characters have the same representation as their utf-8 counterparts
	//this allow us to do the following comparison
	//  pushByte(nextByte);

	//packet to be sent to server, applies only for client to server communication
	//notice that we explicitly requesting that the string will be decoded from UTF-8
	//this is not actually required as it is the default encoding in java.
	short opCode = bytesToShort(message,0);
	switch (opCode) {
		case 3: {//DATA
			short packetSize = bytesToShort(message, 2);
			short blockNumber = bytesToShort(message, 4);
			vector<char> data;
			for (int i = 0; i < packetSize; i++)
				data.push_back(message[i + 6]);
			return new Packet(3,packetSize, blockNumber, data);
		}
		case 4: {
			short blockNumber = bytesToShort(message, 2);
            if ((blockNumber == 0) && request == 7) //requested login was accepted
                loggedIn = true;
			return new Packet(4,blockNumber);
		}
		case 5: {//ERROR
			short ErrorCode = bytesToShort(message, 2);
			string errorMessage = "";
			for (unsigned int i = 0; i < message.size() - 5; i++)
				errorMessage += message.at(i + 4);
			return new Packet(5,ErrorCode, errorMessage);
		}
		case 9: { //BCAST
			char addDel = message.at(2);
			string BCASTMessage = "";
			for (unsigned int i = 0; i < message.size() - 4; i++)
				BCASTMessage += message.at(i + 3);
			return new Packet(9,addDel, BCASTMessage);
		}
	}
	return 0;
}

short EncoderDecoder::bytesToShort(vector<char> bytesArr, int index)
{
	short result = (short)((bytesArr[index] & 0xff) << 8);
	result += (short)(bytesArr[index+1] & 0xff);
	return result;
}

void EncoderDecoder::shortToBytes(short num, vector<char>& bytesVec)
{
    char bytesArr [2];
	bytesArr[0] = ((num >> 8) & 0xFF);
	bytesArr[1] = (num & 0xFF);
	bytesVec.push_back(bytesArr[0]);
	bytesVec.push_back(bytesArr[1]);
}

EncoderDecoder::~EncoderDecoder() {
}

void EncoderDecoder::setRequest(short toSet){
    request = toSet;
}

short EncoderDecoder::getRequest() {
    return request;
}
