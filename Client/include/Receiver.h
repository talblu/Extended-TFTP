/*
 * Receiver.h
 *
 *  Created on: Jan 13, 2017
 *      Author: talblu
 */
//TODO complete!!!!!!!!!!!!!!!!
#ifndef INCLUDE_RECEIVER_H_
#define INCLUDE_RECEIVER_H_

#include <boost/asio.hpp>

using namespace std;

namespace msg{
enum message{
	ASKTXT,
	ASKCHOICES,
	SYSMSG,
	GAMEMSG,
	USRMSG,
	NOTEXISTS
};
}

class Receiver {
public:
	Receiver();
	ConnectionHandler* handler;
	void opertaor();
	Receiver(ConnectionHandler *hand);
	virtual ~Receiver();
};

#endif /* INCLUDE_RECEIVER_H_ */
