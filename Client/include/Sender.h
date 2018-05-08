/*
 * Sender.h
 *
 *  Created on: Jan 13, 2017
 *      Author: talblu
 */

#ifndef INCLUDE_SENDER_H_
#define INCLUDE_SENDER_H_
#include "connectionHandler.h"

class Sender {
private:
	ConnectionHandler* handler;

public:
	Sender();
	 void opertaor();
	 Sender(ConnectionHandler *hand);
	virtual ~Sender();
};

#endif /* INCLUDE_SENDER_H_ */
