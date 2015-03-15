# BtGateway
android application for ECG streaming via wireless communication

## Feature
* Received streaming data from bluetooth with specific protocol
* Encode the packet to dataturbine--ring buffer for streaming data at endpoint server
* At server, dataturbine distributes the data domain-specific application
  eg. influxdb via [Databridge](https://github.com/nodtem66/DataBridge)

## Version
* v0.1.0 first version
