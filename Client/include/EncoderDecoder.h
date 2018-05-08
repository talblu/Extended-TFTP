#ifndef INCLUDE_ENCODERDECODER_H_
#define INCLUDE_ENCODERDECODER_H_

#include <string>
#include <iostream>
#include "Packet.h"
#include <vector>

class EncoderDecoder {
private:
    short request;
    vector<char> collection;
    bool loggedIn;
public:
    EncoderDecoder();
    vector<char> encode(Packet* message);
    Packet* decode(vector<char> bytes);
    short bytesToShort(vector<char> bytesArr, int index);
    void shortToBytes(short num, vector<char>& bytesArr);
    virtual ~EncoderDecoder();
    void setRequest(short toSet);
    short getRequest();
};

#endif