package server;


import java.io.Serializable;
import java.util.Optional;


public class Protocol implements Serializable{
    
    public enum Status
    {                           // Sent by: 
    CONN_INIT_HANDSHAKE,    // server
    CONN_BOOT,  // server
    CONN_REQ,   // client
    CONN_ACK,   // server
    CONN_CONF,  // client
    CONN_OK,    // server
    CONN_DISCONNECT // client
    }

    // in general every protocol is made up of the following

    // A Status and a message
    private Status status;
    public Packet packet;
    public Protocol(Status status, Packet packet)
    {
        this.status = status;
        this.packet = packet;
    }

    // for debugging purposes
    public Protocol()
    {
        this.status = null;
        this.packet = null;
    }

    public Protocol(Status status)
    {
        this.status = status;
        this.packet = null;
    }

    public Packet getPacket()
    {
        return this.packet;
    }

    public Status getStatus()
    {
        return this.status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public static class Packet implements Serializable
    {
        // Now this class has some extra stuff

        // The sender, receiver, text amd metadata
        private String sender;
        private String receiver;
        private String text;
        private MetaData metadata;

        public Packet(String sender, String receiver, String text, MetaData metadata)
        {
            this.sender = sender;
            this.receiver = receiver;
            this.text = text;
            this.metadata = metadata;
        }

        // empty packet
        public Packet()
        {
            this.sender = null;
            this.receiver = null;
            this.text = null;
            this.metadata = null;
        }

        
        public String getSender()
        {
            return this.sender;
        }

        public String getReceiver()
        {
            return this.receiver;
        }

        public String getText()
        {
            return this.text;
        }

        public MetaData getMetaData()
        {
            return this.metadata;
        }


        // This has the commands, still thinking about it
        // Basically tells the receiver what to do with the message
        public static class MetaData implements Serializable
        {
            enum CommProtocol
            {
                GET,
                POST,
                UPDATE,
                DELETE,
                RESPONSE_OK,
                RESPONSE_ERR,       // Baba no time to dey do error codes
            }
            private CommProtocol comm_protocol;
            private Object payload;

            // this is to handle the get and delete handlers.
            // Basic idea is just to have an optional string object which represents a key
            // this optional, MUST be something if the protocol is a GET or DELETE based on id
            private String typeKey;

            // 5 Constructors is crazy, i know!!!!!!


            
            public MetaData()
            {

                this.comm_protocol = null;  // useful during the handshake phase
                this.payload = null;
                this.typeKey = null;
            }

            public MetaData(CommProtocol comm_protocol, Object payload, String typeKey)
            {
                this.comm_protocol = comm_protocol;
                this.payload = payload;
                this.typeKey = typeKey;
            }

            public MetaData(CommProtocol comm_protocol, Object payload)
            {
                this.comm_protocol = comm_protocol;
                this.payload = payload;
                this.typeKey = null;
            }


            public MetaData(CommProtocol comm_protocol)
            {
                this.comm_protocol = comm_protocol;
                this.payload = null;
                this.typeKey =null;
            }

            public MetaData(Object payload)
            {
                this.comm_protocol = null;
                this.payload = payload;
                this.typeKey = null;
            }

            // Please note that this can return null so we use optional
            // we have to known the type of the payload we are fetching
            public  Optional<Object> getPayload()
            {   
                return Optional.ofNullable(payload);
            }

            public Optional<CommProtocol> getCommProtocol()
            {
                return Optional.ofNullable(comm_protocol);
            }

            public Optional<String> getKey()
            {
                return Optional.ofNullable(typeKey);
            }

        }
    }

    /* Soooooooo
     * This is the protocol in a nutshell
     * 
     */

    
}
